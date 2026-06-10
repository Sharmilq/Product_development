package com.dentnova.app.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.R;
import com.google.android.material.button.MaterialButton;

public class FeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ImageView btnBack = findViewById(R.id.btnBack);
        EditText etFeedback = findViewById(R.id.etFeedback);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmitFeedback);

        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {

            String feedback = etFeedback.getText().toString().trim();

            if (feedback.isEmpty()) {
                etFeedback.setError("Please enter feedback");
                return;
            }

            Toast.makeText(
                    this,
                    "Thank you for your feedback 💙",
                    Toast.LENGTH_LONG
            ).show();

            etFeedback.setText("");
        });
    }
}