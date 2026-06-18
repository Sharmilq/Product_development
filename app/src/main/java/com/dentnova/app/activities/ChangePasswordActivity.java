package com.dentnova.app.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.google.android.material.button.MaterialButton;

import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private TextView tvRuleLength, tvRuleUpper, tvRuleLower, tvRuleNumber, tvRuleSpecial, tvMatchError;
    private MaterialButton btnUpdatePassword;

    private boolean isLengthValid = false;
    private boolean isUpperValid = false;
    private boolean isLowerValid = false;
    private boolean isNumberValid = false;
    private boolean isSpecialValid = false;
    private boolean isMatchValid = false;

    // Password regex patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleUpper = findViewById(R.id.tvRuleUpper);
        tvRuleLower = findViewById(R.id.tvRuleLower);
        tvRuleNumber = findViewById(R.id.tvRuleNumber);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
        tvMatchError = findViewById(R.id.tvMatchError);
        
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        updateButtonState();

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                
                isLengthValid = password.length() >= 8;
                isUpperValid = UPPERCASE_PATTERN.matcher(password).matches();
                isLowerValid = LOWERCASE_PATTERN.matcher(password).matches();
                isNumberValid = NUMBER_PATTERN.matcher(password).matches();
                isSpecialValid = SPECIAL_PATTERN.matcher(password).matches();

                updateRuleUI(tvRuleLength, isLengthValid, "Minimum 8 characters");
                updateRuleUI(tvRuleUpper, isUpperValid, "One uppercase letter");
                updateRuleUI(tvRuleLower, isLowerValid, "One lowercase letter");
                updateRuleUI(tvRuleNumber, isNumberValid, "One number");
                updateRuleUI(tvRuleSpecial, isSpecialValid, "One special character");

                checkPasswordsMatch();
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordsMatch();
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnUpdatePassword.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString();
            
            // Show loading dialog
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                    .setView(getLayoutInflater().inflate(R.layout.layout_tooth_scan_analyzing, null))
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            new Thread(() -> {
                try {
                    ApiService.changePassword(this, newPassword);
                    
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        new AlertDialog.Builder(this)
                                .setTitle("Password Updated")
                                .setMessage("Your password has been updated successfully.")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    });
                } catch (Exception e) {
                    android.util.Log.e("ChangePasswordActivity", "Error changing password", e);
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Password update failed. Please try again.", Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }

    private void updateRuleUI(TextView tv, boolean isValid, String text) {
        int colorPassed = Color.parseColor("#43A047");
        int colorError = com.google.android.material.color.MaterialColors.getColor(tv, com.google.android.material.R.attr.colorError);
        
        if (isValid) {
            tv.setText("✓ " + text);
            tv.setTextColor(colorPassed);
        } else {
            tv.setText("✗ " + text);
            tv.setTextColor(colorError);
        }
    }

    private void checkPasswordsMatch() {
        String p1 = etNewPassword.getText().toString();
        String p2 = etConfirmPassword.getText().toString();
        
        if (p2.isEmpty()) {
            tvMatchError.setVisibility(android.view.View.GONE);
            isMatchValid = false;
            return;
        }

        if (p1.equals(p2)) {
            tvMatchError.setVisibility(android.view.View.GONE);
            isMatchValid = true;
        } else {
            tvMatchError.setVisibility(android.view.View.VISIBLE);
            isMatchValid = false;
        }
    }

    private void updateButtonState() {
        boolean allValid = isLengthValid && isUpperValid && isLowerValid && isNumberValid && isSpecialValid && isMatchValid;
        
        if (allValid) {
            btnUpdatePassword.setEnabled(true);
            btnUpdatePassword.setAlpha(1.0f);
        } else {
            btnUpdatePassword.setEnabled(false);
            btnUpdatePassword.setAlpha(0.5f);
        }
    }
}
