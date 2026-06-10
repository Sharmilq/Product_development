package com.dentnova.app.activities;

import android.graphics.Color;
import com.google.gson.JsonArray;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import com.bumptech.glide.Glide;
import com.dentnova.app.services.ApiService;
import com.google.gson.JsonObject;
import de.hdodenhof.circleimageview.CircleImageView;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.dentnova.app.R;

public class ProfileActivity extends AppCompatActivity {

    private boolean brushDone = false;
    private boolean flossDone = false;
    private CircleImageView civAvatar;
    private TextView tvName, tvEmail, tvAgeGender, tvAssessments;
    private CardView cardBrush, cardFloss;
    private TextView tvBrushStatus, tvFlossStatus, tvStreakTitle, tvStreakSub;
    private TextView tvHygieneStatus, tvConsistentStatus, tvProStatus, tvImproverStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        civAvatar = findViewById(R.id.civAvatar);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvAgeGender = findViewById(R.id.tvAgeGender);
        tvAssessments = findViewById(R.id.tvAssessments);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        cardBrush = findViewById(R.id.cardBrush);
        cardFloss = findViewById(R.id.cardFloss);

        tvBrushStatus = findViewById(R.id.tvBrushStatus);
        tvFlossStatus = findViewById(R.id.tvFlossStatus);
        tvStreakTitle = findViewById(R.id.tvStreakTitle);
        tvStreakSub = findViewById(R.id.tvStreakSub);

        tvHygieneStatus = findViewById(R.id.tvHygieneStatus);
        tvConsistentStatus = findViewById(R.id.tvConsistentStatus);
        tvProStatus = findViewById(R.id.tvProStatus);
        tvImproverStatus = findViewById(R.id.tvImproverStatus);

        cardBrush.setOnClickListener(v -> {
            brushDone = true;
            saveHabitProgress();
            updateHabitUI();
        });
        View btnEditProfile = findViewById(R.id.btnEditProfile);

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, ProfileSetupActivity.class);
                startActivity(intent);
            });
        }
        View rowFeedback = findViewById(R.id.rowFeedback);
        View btnSettings = findViewById(R.id.btnSettings);
        View btnLogout = findViewById(R.id.rowLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new Thread(() -> {
                    try {
                        ApiService.logout(ProfileActivity.this);
                    } catch (Exception e) {
                        e.printStackTrace();
                        new com.dentnova.app.utils.SessionManager(ProfileActivity.this)
                                .clearSession();
                    }

                    runOnUiThread(() -> {

                        getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply();

                        Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);

                        finish();
                    });
                }).start();
            });
        }

        if (rowFeedback != null) {
            rowFeedback.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, FeedbackActivity.class));
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
            });
        }
        cardFloss.setOnClickListener(v -> {
            flossDone = true;
            saveHabitProgress();
            updateHabitUI();
        });
        loadProfile();
        loadHabitProgress();
        updateHabitUI();
    }
    private void loadProfile() {

        new Thread(() -> {

            try {
                JsonObject result = ApiService.getProfile(ProfileActivity.this);

                if (result.has("success") &&
                        result.get("success").getAsBoolean()) {

                    JsonObject profile = result.getAsJsonObject("profile");

                    String name = profile.has("name")
                            ? profile.get("name").getAsString()
                            : "DentNova User";

                    String email = profile.has("email") &&
                            !profile.get("email").isJsonNull()
                            ? profile.get("email").getAsString()
                            : "No Email";

                    String photo = profile.has("photo_url") &&
                            !profile.get("photo_url").isJsonNull()
                            ? profile.get("photo_url").getAsString()
                            : "";

                    JsonObject ah = ApiService.getAssessmentHistory(ProfileActivity.this);
                    JsonArray assessments = ah.getAsJsonArray("assessments");
                    int assessmentCount = assessments.size();

                    runOnUiThread(() -> {

                        tvName.setText(name);
                        tvEmail.setText(email);
                        tvAgeGender.setText("Profile Active");
                        tvAssessments.setText(assessmentCount + " Assessments");

                        if (!photo.isEmpty()) {
                            byte[] decoded = android.util.Base64.decode(
                                    photo,
                                    android.util.Base64.DEFAULT
                            );

                            android.graphics.Bitmap bmp =
                                    android.graphics.BitmapFactory.decodeByteArray(
                                            decoded,
                                            0,
                                            decoded.length
                                    );

                            civAvatar.setImageBitmap(bmp);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
    private void saveHabitProgress() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("brush_done_" + today, brushDone)
                .putBoolean("floss_done_" + today, flossDone)
                .putString("last_habit_date", today)
                .apply();
    }
    private void loadHabitProgress() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        brushDone = getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                .getBoolean("brush_done_" + today, false);

        flossDone = getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                .getBoolean("floss_done_" + today, false);
    }
    private void updateHabitUI() {
        if (brushDone) {
            cardBrush.setCardBackgroundColor(Color.parseColor("#E8F8EE"));
            tvBrushStatus.setText("Done ✓ 🔥");
            tvBrushStatus.setTextColor(Color.parseColor("#43A047"));
        } else {
            cardBrush.setCardBackgroundColor(Color.WHITE);
            tvBrushStatus.setText("Pending");
            tvBrushStatus.setTextColor(Color.parseColor("#6B7B8D"));
        }

        if (flossDone) {
            cardFloss.setCardBackgroundColor(Color.parseColor("#E8F8EE"));
            tvFlossStatus.setText("Done ✓ 🔥");
            tvFlossStatus.setTextColor(Color.parseColor("#43A047"));
        } else {
            cardFloss.setCardBackgroundColor(Color.WHITE);
            tvFlossStatus.setText("Pending");
            tvFlossStatus.setTextColor(Color.parseColor("#6B7B8D"));
        }

        if (brushDone || flossDone) {
            tvStreakTitle.setText("Streak started 🔥");
            tvStreakSub.setText("Healthy habits in motion — keep going!");
            tvHygieneStatus.setText((brushDone && flossDone) ? "Earned ✓" : "Locked");
            tvConsistentStatus.setText("Locked");
            tvProStatus.setText("Locked");
        } else {
            tvStreakTitle.setText("No streak yet");
            tvStreakSub.setText("Complete a habit to start your streak");
            tvHygieneStatus.setText("Locked");
            tvConsistentStatus.setText("Locked");
            tvProStatus.setText("Locked");
        }

        tvImproverStatus.setText("Locked");
    }
}