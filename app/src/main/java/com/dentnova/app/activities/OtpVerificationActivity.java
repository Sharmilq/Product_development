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
    }

    private void setupListeners() {
        btnVerifyOtp.setOnClickListener(v -> handleOtpVerification());
        btnResetPassword.setOnClickListener(v -> handlePasswordReset());
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
                    Toast.makeText(this, "Connection failed. Please check your network.", Toast.LENGTH_LONG).show();
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

    private void handlePasswordReset() {
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

        if (newPassword.length() < 6) {
            Toast.makeText(this, R.string.err_short_password, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, R.string.otp_reset_success, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "Connection failed. Please check your network.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
