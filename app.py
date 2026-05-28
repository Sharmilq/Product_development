from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import joblib
import tensorflow as tf
import numpy as np
from tensorflow.keras.preprocessing import image

app = Flask(__name__)
CORS(app)

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


if __name__ == "__main__":
    print("Starting DentNova Flask server...")
    app.run(host="0.0.0.0", port=5000, debug=False)
