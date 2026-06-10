from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import joblib
import tensorflow as tf
import numpy as np
from tensorflow.keras.preprocessing import image
import smtplib
import hashlib
import os
import requests
import datetime
import random
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

app = Flask(__name__)
CORS(app)

# Env config for Supabase
supabase_url = os.environ.get("SUPABASE_URL", "https://kxuwskwwmrpoilrxngha.supabase.co")
supabase_key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")

def sha256_hash(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def supabase_headers():
    return {
        "apikey": supabase_key,
        "Authorization": f"Bearer {supabase_key}",
        "Content-Type": "application/json",
        "Prefer": "return=representation"
    }

def upsert_otp(email, otp_hash, expires_at):
    headers = supabase_headers()
    check_url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}&select=id"
    check_res = requests.get(check_url, headers=headers)
    
    payload = {
        "email": email,
        "otp_hash": otp_hash,
        "expires_at": expires_at,
        "used": False
    }
    
    if check_res.status_code == 200 and len(check_res.json()) > 0:
        update_url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}"
        res = requests.patch(update_url, headers=headers, json=payload)
    else:
        insert_url = f"{supabase_url}/rest/v1/password_reset_otps"
        res = requests.post(insert_url, headers=headers, json=payload)
    return res.status_code in [200, 201, 204]

def send_otp_email(to_email, otp):
    smtp_host = os.environ.get("SMTP_HOST", "smtp.gmail.com")
    smtp_port = int(os.environ.get("SMTP_PORT", "587"))
    smtp_user = os.environ.get("SMTP_USER")
    smtp_pass = os.environ.get("SMTP_PASS")
    
    if not smtp_user or not smtp_pass:
        print("[OTP] Warning: SMTP credentials are not configured in environment.", flush=True)
        return False
        
    msg = MIMEMultipart()
    msg['From'] = f"DentNova <{smtp_user}>"
    msg['To'] = to_email
    msg['Subject'] = "Your DentNova Password Reset Code"
    
    body = f"""
    <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E0E8EF;border-radius:12px;">
      <h2 style="color:#00BCD4;text-align:center;margin-top:0;">🦷 DentNova Password Reset</h2>
      <p>Hello,</p>
      <p>We received a request to reset your DentNova password. Use the code below — it expires in <strong>5 minutes</strong>.</p>
      <div style="text-align:center;margin:28px 0;">
        <span style="display:inline-block;font-size:36px;font-weight:bold;letter-spacing:8px;color:#1A2332;background:#F5F9FA;padding:12px 28px;border-radius:10px;border:2px dashed #00BCD4;">{otp}</span>
      </div>
      <p style="font-size:13px;color:#888;">If you did not request this, you can safely ignore this email. Your password will not change.</p>
      <p style="font-size:13px;color:#888;">— The DentNova Team</p>
    </div>
    """
    msg.attach(MIMEText(body, 'html'))
    
    try:
        server = smtplib.SMTP(smtp_host, smtp_port)
        server.starttls()
        server.login(smtp_user, smtp_pass)
        server.send_message(msg)
        server.quit()
        return True
    except Exception as e:
        print(f"[OTP] Error sending email: {e}", flush=True)
        return False

print("Loading assessment model...")
model = joblib.load("dentnova_catboost_model_v2.pkl")
print("Assessment model loaded")

print("Loading tooth model...")
interpreter = tf.lite.Interpreter(model_path="dentnova_mobilenetv2.tflite")
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
print("Tooth model loaded")

CLASS_NAMES = ["Calculus", "Gingivitis", "Healthy"]


@app.route("/predict", methods=["POST"])
def predict():
    data = request.json
    sample = pd.DataFrame([data])

    score = model.predict(sample)[0]
    score = max(5, min(97, score))

    if score >= 70:
        risk = "Low"
    elif score >= 40:
        risk = "Moderate"
    else:
        risk = "High"

    return jsonify({
        "success": True,
        "score": round(score),
        "risk": risk
    })


@app.route("/predict-tooth", methods=["POST"])
def predict_tooth():
    if "image" not in request.files:
        return jsonify({"success": False, "message": "Image required"})

    file = request.files["image"]
    file_path = "temp_tooth.jpg"
    file.save(file_path)

    img = image.load_img(file_path, target_size=(224, 224))
    img_array = image.img_to_array(img) / 255.0
    img_array = np.expand_dims(img_array, axis=0)

    interpreter.set_tensor(input_details[0]["index"], img_array.astype("float32"))
    interpreter.invoke()

    preds = interpreter.get_tensor(output_details[0]["index"])[0]

    class_index = int(np.argmax(preds))
    class_name = CLASS_NAMES[class_index]
    confidence = float(preds[class_index])
    print("Predicted:", class_name, flush=True)
    print("Confidence:", confidence, flush=True)
    print("All predictions:", preds.tolist(), flush=True)

    if class_name == "Healthy":
       inflammation_score = 0
       cleanliness_score = int(88 + confidence * 7)
       overall_score = int(88 + confidence * 7)
       result_label = "Healthy gums and excellent oral condition"

    elif class_name == "Gingivitis":
        inflammation_score = int(35 + confidence * 50)
        cleanliness_score = int(40 + (1 - confidence) * 35)
        overall_score = int(45 + confidence * 30)
        result_label = "Possible gingival inflammation detected"

    else:
        inflammation_score = int(60 + confidence * 35)
        cleanliness_score = int(20 + (1 - confidence) * 30)
        overall_score = int(25 + (1 - confidence) * 30)
        result_label = "Poor cleanliness / calculus signs detected"

    return jsonify({
        "success": True,
        "class": class_name,
        "confidence": round(confidence, 2),
        "overall_score": max(1, min(100, overall_score)),
        "inflammation_score": max(1, min(100, inflammation_score)),
        "cleanliness_score": max(1, min(100, cleanliness_score)),
        "result_label": result_label
    })


# ── Custom OTP Routes for Password Reset ─────────────────────────────────────

@app.route("/auth/request-password-otp", methods=["POST"])
def request_password_otp():
    print("[REQUEST_OTP] Received request", flush=True)
    data = request.json or {}
    email = data.get("email")
    if not email:
        return jsonify({"success": False, "message": "Email is required"}), 400
        
    try:
        headers = supabase_headers()
        url = f"{supabase_url}/rest/v1/users?email=eq.{email}&select=email"
        response = requests.get(url, headers=headers)
        user_exists = response.status_code == 200 and len(response.json()) > 0
        
        if user_exists:
            otp = f"{random.randint(100000, 999999)}"
            otp_hash = sha256_hash(otp)
            expires_at = (datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(minutes=5)).isoformat()
            
            if upsert_otp(email, otp_hash, expires_at):
                if send_otp_email(email, otp):
                    print(f"[OTP_SENT] OTP successfully sent to {email}", flush=True)
                else:
                    print(f"[OTP] Failed to send email to {email}", flush=True)
            else:
                print(f"[OTP] Failed to upsert OTP to database for {email}", flush=True)
        else:
            print(f"[OTP] Password reset requested for unknown email: {email}", flush=True)
            
        return jsonify({"success": True, "message": "If this email is registered, you will receive a 6-digit OTP shortly."})
    except Exception as e:
        print(f"[OTP] Error in request-password-otp: {e}", flush=True)
        return jsonify({"success": False, "message": "Failed to request OTP."}), 500

@app.route("/auth/verify-password-otp", methods=["POST"])
def verify_password_otp():
    print("[OTP_VERIFY] Received verification request", flush=True)
    data = request.json or {}
    email = data.get("email")
    otp = data.get("otp")
    if not email or not otp:
        return jsonify({"success": False, "message": "Email and OTP are required"}), 400
        
    try:
        headers = supabase_headers()
        url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}&select=*"
        response = requests.get(url, headers=headers)
        
        if response.status_code != 200 or len(response.json()) == 0:
            return jsonify({"success": False, "message": "No OTP request found for this email. Please request a new code."}), 400
            
        record = response.json()[0]
        
        if record.get("used"):
            return jsonify({"success": False, "message": "This OTP has already been used. Please request a new one."}), 400
            
        expires_at_str = record.get("expires_at")
        expires_at = datetime.datetime.fromisoformat(expires_at_str.replace("Z", "+00:00"))
        
        if datetime.datetime.now(datetime.timezone.utc) > expires_at:
            del_url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}"
            requests.delete(del_url, headers=headers)
            return jsonify({"success": False, "message": "OTP has expired. Please request a new code."}), 400
            
        otp_hash = sha256_hash(otp)
        if record.get("otp_hash") != otp_hash:
            return jsonify({"success": False, "message": "Invalid OTP code. Please check and try again."}), 400
            
        print(f"[OTP_VALID] OTP verified for {email}", flush=True)
        return jsonify({"success": True, "message": "OTP verified successfully"})
    except Exception as e:
        print(f"[OTP] Error in verify-password-otp: {e}", flush=True)
        return jsonify({"success": False, "message": "Server error during verification."}), 500

@app.route("/auth/reset-password-with-otp", methods=["POST"])
def reset_password_with_otp():
    print("[PASSWORD_RESET] Received reset request", flush=True)
    data = request.json or {}
    email = data.get("email")
    otp = data.get("otp")
    new_password = data.get("newPassword")
    
    if not email or not otp or not new_password:
        return jsonify({"success": False, "message": "email, otp, and newPassword are required"}), 400
    if len(new_password) < 6:
        return jsonify({"success": False, "message": "Password must be at least 6 characters"}), 400
        
    try:
        headers = supabase_headers()
        url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}&select=*"
        response = requests.get(url, headers=headers)
        
        if response.status_code != 200 or len(response.json()) == 0:
            print("[PASSWORD_RESET_FAILED] OTP not found", flush=True)
            return jsonify({"success": False, "message": "OTP not found. Please request a new code."}), 400
            
        record = response.json()[0]
        
        if record.get("used"):
            print("[PASSWORD_RESET_FAILED] OTP already used", flush=True)
            return jsonify({"success": False, "message": "OTP already used. Please request a new one."}), 400
            
        expires_at_str = record.get("expires_at")
        expires_at = datetime.datetime.fromisoformat(expires_at_str.replace("Z", "+00:00"))
        
        if datetime.datetime.now(datetime.timezone.utc) > expires_at:
            print("[PASSWORD_RESET_FAILED] OTP expired", flush=True)
            del_url = f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}"
            requests.delete(del_url, headers=headers)
            return jsonify({"success": False, "message": "OTP expired. Please request a new code."}), 400
            
        otp_hash = sha256_hash(otp)
        if record.get("otp_hash") != otp_hash:
            print("[PASSWORD_RESET_FAILED] Invalid OTP", flush=True)
            return jsonify({"success": False, "message": "Invalid OTP."}), 400
            
        auth_list_url = f"{supabase_url}/auth/v1/admin/users"
        auth_headers = {
            "apikey": supabase_key,
            "Authorization": f"Bearer {supabase_key}"
        }
        
        list_res = requests.get(auth_list_url, headers=auth_headers)
        if list_res.status_code != 200:
            print(f"[PASSWORD_RESET_FAILED] Failed to list auth users. Status: {list_res.status_code}", flush=True)
            return jsonify({"success": False, "message": "Failed to verify user account in auth system."}), 500
            
        users = list_res.json()
        supabase_user = None
        user_list = users.get("users", []) if isinstance(users, dict) else users
        
        for u in user_list:
            if u.get("email") == email:
                supabase_user = u
                break
                
        if not supabase_user:
            print("[PASSWORD_RESET_FAILED] User not found in auth system", flush=True)
            return jsonify({"success": False, "message": "User account not found in auth system."}), 404
            
        user_id = supabase_user.get("id")
        
        update_url = f"{supabase_url}/auth/v1/admin/users/{user_id}"
        update_payload = {"password": new_password}
        update_res = requests.put(update_url, headers=auth_headers, json=update_payload)
        
        if update_res.status_code != 200:
            print(f"[PASSWORD_RESET_FAILED] Failed to update password: {update_res.text}", flush=True)
            return jsonify({"success": False, "message": "Failed to update password."}), 500
            
        used_payload = {"used": True}
        requests.patch(f"{supabase_url}/rest/v1/password_reset_otps?email=eq.{email}", headers=headers, json=used_payload)
        
        print(f"[PASSWORD_RESET_SUCCESS] Password reset complete for {email}", flush=True)
        return jsonify({"success": True, "message": "Password updated successfully. Please sign in."})
    except Exception as e:
        print(f"[PASSWORD_RESET_FAILED] Exception: {e}", flush=True)
        return jsonify({"success": False, "message": "Failed to update password. Please try again."}), 500

if __name__ == "__main__":
    print("Starting DentNova Flask server...")
    app.run(host="0.0.0.0", port=5000, debug=False)