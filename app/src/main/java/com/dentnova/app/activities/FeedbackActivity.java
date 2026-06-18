package com.dentnova.app.activities;

import android.app.AlertDialog;
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
import com.google.gson.JsonObject;

public class FeedbackActivity extends AppCompatActivity {

    private MaterialButton btnSubmit;
    private EditText etFeedback;
    private TextView tvCharCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ImageView btnBack = findViewById(R.id.btnBack);
        etFeedback = findViewById(R.id.etFeedback);
        btnSubmit = findViewById(R.id.btnSubmitFeedback);
        tvCharCount = findViewById(R.id.tvCharCount);

        btnSubmit.setEnabled(false);
        btnSubmit.setAlpha(0.5f);

        btnBack.setOnClickListener(v -> finish());

        etFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                tvCharCount.setText(len + "/1000");

                if (len >= 10 && len <= 1000) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setAlpha(1.0f);
                } else {
                    btnSubmit.setEnabled(false);
                    btnSubmit.setAlpha(0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String feedback = etFeedback.getText().toString().trim();

            if (feedback.length() < 10) {
                etFeedback.setError("Minimum 10 characters required");
                return;
            }

            // Show loading dialog
            AlertDialog loadingDialog = new AlertDialog.Builder(this)
                    .setView(getLayoutInflater().inflate(R.layout.layout_tooth_scan_analyzing, null))
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            new Thread(() -> {
                try {
                    JsonObject result = ApiService.sendFeedback(this, feedback);
                    
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        if (result.has("success") && result.get("success").getAsBoolean()) {
                            showSuccessDialog();
                        } else {
                            String errorMsg = result.has("error") ? result.get("error").getAsString() : "Unknown error";
                            showErrorDialog(errorMsg);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("FeedbackActivity", "Feedback send failed", e);
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        showErrorDialog(e.getMessage() != null ? e.getMessage() : "Exception occurred");
                    });
                }
            }).start();
        });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thank You!")
                .setMessage("Your feedback has been submitted successfully.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle("Submission Failed")
                .setMessage("Error: " + errorMessage + "\n\nPlease try again.")
                .setPositiveButton("OK", null)
                .show();
    }
}