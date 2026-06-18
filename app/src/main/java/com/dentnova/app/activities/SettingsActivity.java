package com.dentnova.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

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
        TextView rowFeedback = findViewById(R.id.rowFeedback);
        rowFeedback.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, FeedbackActivity.class));
        });

        btnBack.setOnClickListener(v -> finish());

        rowEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(
                    SettingsActivity.this,
                    ProfileSetupActivity.class
            ));
        });

        TextView rowLogout = findViewById(R.id.rowLogout);
        rowLogout.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (d, w) -> {

                        // 1. Clear SharedPreferences session and cached user data
                        new com.dentnova.app.utils.SessionManager(SettingsActivity.this).clearSession();
                        
                        // Keep onboarding state but clear other prefs
                        SharedPreferences prefs = getSharedPreferences("dentnova_prefs", MODE_PRIVATE);
                        boolean seenOnboarding = prefs.getBoolean("has_seen_onboarding", false);
                        String themeMode = prefs.getString("theme_mode", "system");
                        prefs.edit().clear()
                                .putBoolean("has_seen_onboarding", seenOnboarding)
                                .putString("theme_mode", themeMode)
                                .apply();
                        android.util.Log.d("THEME", "SESSION_TOKEN_PRESERVED is false, but theme preserved: " + themeMode);

                        // 2. Sign out of Firebase
                        FirebaseAuth.getInstance().signOut();

                        // 3. Sign out + revoke Google access so chooser appears next time
                        GoogleSignInOptions gso =
                                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail().build();
                        GoogleSignInClient gsc = GoogleSignIn.getClient(this, gso);
                        gsc.signOut().addOnCompleteListener(this, t ->
                                gsc.revokeAccess().addOnCompleteListener(this, t2 -> {
                                    android.util.Log.d("LOGOUT", "Google session revoked on logout.");
                                })
                        );

                        Intent intent = new Intent(SettingsActivity.this, AuthActivity.class);
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        rowPassword.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        });

        rowNotifications.setOnClickListener(v -> {
            startActivity(new Intent(
                    SettingsActivity.this,
                    RemindersActivity.class
            ));
        });
        rowTheme.setOnClickListener(v -> showThemeDialog());
        rowPrivacy.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, PrivacyPolicyActivity.class));
        });

        rowVersion.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "DentNova Version 1.0",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System Default"};

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setItems(themes, (dialog, which) -> {
                    SharedPreferences.Editor editor =
                            getSharedPreferences("dentnova_prefs", MODE_PRIVATE).edit();

                    if (which == 0) {
                        android.util.Log.d("THEME", "THEME_SELECTED: Light");
                        editor.putString("theme_mode", "light");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (which == 1) {
                        android.util.Log.d("THEME", "THEME_SELECTED: Dark");
                        editor.putString("theme_mode", "dark");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        android.util.Log.d("THEME", "THEME_SELECTED: System Default");
                        editor.putString("theme_mode", "system");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    }

                    editor.apply();
                    android.util.Log.d("THEME", "THEME_MODE_SAVED");
                    android.util.Log.d("THEME", "THEME_APPLIED");
                    recreate();
                })
                .show();
    }
}