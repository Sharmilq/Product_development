package com.dentnova.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OtpVerificationActivity extends AppCompatActivity {

    private String email;
    private String verifiedOtp = "";

    // Views
    private TextView tvOtpTitle, tvOtpSubtitle;
    private TextView tvRuleMinLength, tvRuleUppercase, tvRuleLowercase, tvRuleNumber, tvRuleSpecial;
    private LinearLayout layoutOtpState, layoutResetPasswordState;
    private TextInputEditText etOtpCode, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnVerifyOtp, btnResetPassword;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        email = getIntent().getStringExtra("email");
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is missing. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        tvOtpTitle = findViewById(R.id.tvOtpTitle);
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle);
        layoutOtpState = findViewById(R.id.layoutOtpState);
        layoutResetPasswordState = findViewById(R.id.layoutResetPasswordState);
        etOtpCode = findViewById(R.id.etOtpCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        tvRuleMinLength = findViewById(R.id.tvRuleMinLength);
        tvRuleUppercase = findViewById(R.id.tvRuleUppercase);
        tvRuleLowercase = findViewById(R.id.tvRuleLowercase);
        tvRuleNumber = findViewById(R.id.tvRuleNumber);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);

        btnResetPassword.setEnabled(false);
    }

    private void setupListeners() {
        btnVerifyOtp.setOnClickListener(v -> handleOtpVerification());
        btnResetPassword.setOnClickListener(v -> handlePasswordReset());

        android.text.TextWatcher passwordWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordStrengthLive();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        };

        etNewPassword.addTextChangedListener(passwordWatcher);
        etConfirmNewPassword.addTextChangedListener(passwordWatcher);
    }

    private void handleOtpVerification() {
        String otp = etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : "";

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(this, R.string.err_otp_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.length() != 6 || !TextUtils.isDigitsOnly(otp)) {
            Toast.makeText(this, R.string.err_otp_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Verifying code...");
        progress.setCancelable(false);
        progress.show();

        executor.execute(() -> {
            try {
                JsonObject result = ApiService.verifyResetOtp(this, email, otp);
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        verifiedOtp = otp;
                        Toast.makeText(this, R.string.otp_verification_success, Toast.LENGTH_SHORT).show();
                        showResetPasswordFields();
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Verification failed. Invalid or expired OTP.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OtpVerification", "Error verifying reset OTP", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showResetPasswordFields() {
        layoutOtpState.setVisibility(View.GONE);
        layoutResetPasswordState.setVisibility(View.VISIBLE);
        tvOtpTitle.setText(R.string.title_reset_password);
        tvOtpSubtitle.setText(R.string.subtitle_reset_password);
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void handlePasswordReset() {
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

        if (!isStrongPassword(newPassword)) {
            etNewPassword.setError("Weak password");
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Weak Password")
                .setMessage("Your password must contain:\n\n• At least 8 characters\n• One uppercase letter\n• One lowercase letter\n• One number\n• One special character")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, R.string.err_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Updating your password...");
        progress.setCancelable(false);
        progress.show();

        executor.execute(() -> {
            try {
                JsonObject result = ApiService.resetPasswordWithOtp(email, verifiedOtp, newPassword);
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        Toast.makeText(this, "Password reset successful. Please login.", Toast.LENGTH_LONG).show();
                        // Exit back to AuthActivity login screen
                        finish();
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Failed to reset password. Please request a new OTP.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OtpVerification", "Error updating password after OTP verification", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void validatePasswordStrengthLive() {
        String password = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

        boolean minLength = password.length() >= 8;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        updateRuleUI(tvRuleMinLength, minLength, "Minimum 8 characters");
        updateRuleUI(tvRuleUppercase, hasUpper, "One uppercase letter");
        updateRuleUI(tvRuleLowercase, hasLower, "One lowercase letter");
        updateRuleUI(tvRuleNumber, hasDigit, "One number");
        updateRuleUI(tvRuleSpecial, hasSpecial, "One special character");

        boolean isStrong = minLength && hasUpper && hasLower && hasDigit && hasSpecial;
        boolean matches = password.equals(confirmPassword) && !confirmPassword.isEmpty();

        btnResetPassword.setEnabled(isStrong && matches);
    }

    private void updateRuleUI(TextView tv, boolean passed, String ruleText) {
        if (tv == null) return;
        if (passed) {
            tv.setText("✓ " + ruleText);
            tv.setTextColor(android.graphics.Color.parseColor("#2E7D32")); // Green
        } else {
            tv.setText("✗ " + ruleText);
            tv.setTextColor(android.graphics.Color.parseColor("#E53935")); // Red
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
