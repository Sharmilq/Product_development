package com.dentnova.app.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.InputType;

import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.services.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etPassword;
    private TextInputEditText etConfirm;
    private MaterialButton btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 50);

        etPassword = new TextInputEditText(this);
        etPassword.setHint("New password");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        etConfirm = new TextInputEditText(this);
        etConfirm.setHint("Confirm password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        btnReset = new MaterialButton(this);
        btnReset.setText("Reset Password");

        layout.addView(etPassword);
        layout.addView(etConfirm);
        layout.addView(btnReset);

        setContentView(layout);

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String p1 = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String p2 = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

        if (p1.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            return;
        }

        if (!p1.equals(p2)) {
            etConfirm.setError("Passwords do not match");
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Updating password...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                ApiService.changePassword(this, p1);

                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Password reset successful", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Password reset failed", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}