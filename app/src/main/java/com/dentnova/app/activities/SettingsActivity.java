package com.dentnova.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatDelegate;
import com.dentnova.app.services.ApiService;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView btnBack = findViewById(R.id.btnBack);

        TextView rowEditProfile = findViewById(R.id.rowEditProfile);
        TextView rowPassword = findViewById(R.id.rowPassword);
        TextView rowNotifications = findViewById(R.id.rowNotifications);
        TextView rowTheme = findViewById(R.id.rowTheme);
        TextView rowPrivacy = findViewById(R.id.rowPrivacy);
        TextView rowVersion = findViewById(R.id.rowVersion);
        TextView rowFeedback =
                findViewById(R.id.rowFeedback);
        rowFeedback.setOnClickListener(v -> {

            EditText input = new EditText(this);
            input.setHint("Share your feedback");

            new AlertDialog.Builder(this)
                    .setTitle("DentNova Feedback")
                    .setView(input)
                    .setPositiveButton("Send", (d, w) -> {

                        String msg =
                                input.getText()
                                        .toString()
                                        .trim();

                        if (msg.isEmpty()) return;

                        new Thread(() -> {

                            try {

                                ApiService.sendFeedback(
                                        this,
                                        msg
                                );

                                runOnUiThread(() ->
                                        Toast.makeText(
                                                this,
                                                "Feedback sent 💙",
                                                Toast.LENGTH_LONG
                                        ).show()
                                );

                            } catch (Exception e) {
                                android.util.Log.e("SettingsActivity", "Error sending feedback to Supabase", e);
                            }

                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnBack.setOnClickListener(v -> finish());

        rowEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(
                    SettingsActivity.this,
                    ProfileSetupActivity.class
            ));
        });

        TextView rowLogout =
                findViewById(R.id.rowLogout);
        rowLogout.setOnClickListener(v -> {

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (d, w) -> {

                        getSharedPreferences(
                                "dentnova_prefs",
                                MODE_PRIVATE
                        ).edit().clear().apply();

                        Intent intent =
                                new Intent(
                                        SettingsActivity.this,
                                        AuthActivity.class
                                );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rowPassword.setOnClickListener(v -> {

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 30, 50, 10);

            EditText newPass = new EditText(this);
            newPass.setHint("New password");
            newPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(newPass);

            EditText confirmPass = new EditText(this);
            confirmPass.setHint("Confirm password");
            confirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(confirmPass);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Update Password")
                    .setMessage("Choose a secure password for your DentNova account.")
                    .setView(layout)
                    .setPositiveButton("Update", null)
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.setOnShowListener(d -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {

                    String p1 = newPass.getText().toString().trim();
                    String p2 = confirmPass.getText().toString().trim();

                    if (p1.length() < 6) {
                        newPass.setError("Minimum 6 characters");
                        return;
                    }

                    if (!p1.equals(p2)) {
                        confirmPass.setError("Passwords do not match");
                        return;
                    }

                    new Thread(() -> {
                        try {
                            ApiService.changePassword(this, p1);

                            runOnUiThread(() -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show();
                            });

                        } catch (Exception e) {
                            android.util.Log.e("SettingsActivity", "Error changing password", e);
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Password update failed", Toast.LENGTH_LONG).show()
                            );
                        }
                    }).start();
                });
            });

            dialog.show();
        });

        rowNotifications.setOnClickListener(v -> {
            startActivity(new Intent(
                    SettingsActivity.this,
                    RemindersActivity.class
            ));
        });
        rowTheme.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "Dark mode will be added in the next update 🌙",
                    Toast.LENGTH_LONG
            ).show();
        });
        rowPrivacy.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "DentNova respects your privacy 💙",
                    Toast.LENGTH_LONG
            ).show();
        });

        rowVersion.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "DentNova Version 1.0",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}