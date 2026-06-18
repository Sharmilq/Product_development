package com.dentnova.app.activities;

import android.graphics.Color;
import com.google.gson.JsonArray;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.dentnova.app.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    // ── In-memory habit state (refreshed from Supabase on every open) ────────
    private boolean brushDone = false;
    private boolean flossDone = false;
    private int currentStreakCount = 0;

    // ── Views ────────────────────────────────────────────────────────────────
    private CircleImageView civAvatar;
    private TextView tvName, tvEmail, tvAgeGender, tvAssessments;
    private CardView cardBrush, cardFloss, cardStreak;
    private TextView tvBrushStatus, tvFlossStatus, tvStreakTitle, tvStreakSub;
    private TextView tvHygieneStatus, tvConsistentStatus, tvProStatus, tvImproverStatus;

    // ── Date helpers ─────────────────────────────────────────────────────────
    private static String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String yesterdayStr() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind views
        civAvatar         = findViewById(R.id.civAvatar);
        tvName            = findViewById(R.id.tvName);
        tvEmail           = findViewById(R.id.tvEmail);
        tvAgeGender       = findViewById(R.id.tvAgeGender);
        tvAssessments     = findViewById(R.id.tvAssessments);
        cardBrush         = findViewById(R.id.cardBrush);
        cardFloss         = findViewById(R.id.cardFloss);
        cardStreak        = findViewById(R.id.cardStreak);
        tvBrushStatus     = findViewById(R.id.tvBrushStatus);
        tvFlossStatus     = findViewById(R.id.tvFlossStatus);
        tvStreakTitle     = findViewById(R.id.tvStreakTitle);
        tvStreakSub       = findViewById(R.id.tvStreakSub);
        tvHygieneStatus   = findViewById(R.id.tvHygieneStatus);
        tvConsistentStatus= findViewById(R.id.tvConsistentStatus);
        tvProStatus       = findViewById(R.id.tvProStatus);
        tvImproverStatus  = findViewById(R.id.tvImproverStatus);

        // Show default "not done" UI immediately (before Supabase responds)
        updateHabitUI();

        // ── Back button ───────────────────────────────────────────────────────
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // ── Brushing card tap ─────────────────────────────────────────────────
        cardBrush.setOnClickListener(v -> {
            if (brushDone) return; // already done today
            brushDone = true;
            updateHabitUI();
            // Save to Supabase then check streak — sequential in background
            new Thread(() -> {
                try {
                    String today = todayStr();
                    android.util.Log.d("MARK_BRUSHING_DONE", "Saving brushing_done=true date=" + today);
                    ApiService.updateHabitStatus(ProfileActivity.this, true, flossDone, today);
                    checkAndUpdateStreak();
                } catch (Exception e) {
                    android.util.Log.e("ProfileActivity", "Error marking brushing done", e);
                }
            }).start();
        });

        // ── Flossing card tap ─────────────────────────────────────────────────
        cardFloss.setOnClickListener(v -> {
            if (flossDone) return; // already done today
            flossDone = true;
            updateHabitUI();
            new Thread(() -> {
                try {
                    String today = todayStr();
                    android.util.Log.d("MARK_FLOSSING_DONE", "Saving flossing_done=true date=" + today);
                    ApiService.updateHabitStatus(ProfileActivity.this, brushDone, true, today);
                    checkAndUpdateStreak();
                } catch (Exception e) {
                    android.util.Log.e("ProfileActivity", "Error marking flossing done", e);
                }
            }).start();
        });

        // ── DEBUG: Long-press streak card → reset today's habits ─────────────
        if (cardStreak != null) {
            cardStreak.setOnLongClickListener(v -> {
                resetHabitsDebug();
                return true;
            });
        }

        // ── Navigation ────────────────────────────────────────────────────────
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileSetupActivity.class)));
        }

        View rowFeedback = findViewById(R.id.rowFeedback);
        if (rowFeedback != null) {
            rowFeedback.setOnClickListener(v ->
                    startActivity(new Intent(this, FeedbackActivity.class)));
        }

        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));
        }

        View btnLogout = findViewById(R.id.rowLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }

        // ── Load data from Supabase ───────────────────────────────────────────
        loadProfile();
        loadHabitStatusFromSupabase();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // A) Load habit status from Supabase — SOURCE OF TRUTH
    //    Rules per spec: null date → init today, same date → use DB values,
    //    old date → reset habits (NOT streak), update Supabase
    // ─────────────────────────────────────────────────────────────────────────
    private void loadHabitStatusFromSupabase() {
        new Thread(() -> {
            try {
                int userId = new com.dentnova.app.utils.SessionManager(
                        ProfileActivity.this).getUserId();
                android.util.Log.d("PROFILE_USER_ID", "user_id=" + userId);

                String today = todayStr();
                android.util.Log.d("TODAY", today);

                JsonObject result = ApiService.getHabitStatus(ProfileActivity.this);
                if (!result.has("success") || !result.get("success").getAsBoolean()) {
                    android.util.Log.e("HABIT_LOAD", "Failed to fetch habit status");
                    return;
                }

                JsonObject data = result.getAsJsonObject("data");

                // ── Read DB values ─────────────────────────────────────────
                String dbHabitDate = safeStr(data, "habit_date");
                boolean dbBrushing = safeBool(data, "brushing_done");
                boolean dbFlossing = safeBool(data, "flossing_done");
                int dbStreak       = safeInt(data,  "streak_count");
                String dbLastStreak= safeStr(data, "last_streak_date");

                android.util.Log.d("DB_HABIT_DATE",      "db=" + dbHabitDate + " today=" + today);
                android.util.Log.d("DB_BRUSHING_DONE",   String.valueOf(dbBrushing));
                android.util.Log.d("DB_FLOSSING_DONE",   String.valueOf(dbFlossing));
                android.util.Log.d("DB_STREAK_COUNT",    String.valueOf(dbStreak));
                android.util.Log.d("DB_LAST_STREAK_DATE",dbLastStreak);

                boolean finalBrush;
                boolean finalFloss;

                if (dbHabitDate == null || dbHabitDate.isEmpty()) {
                    // ── habit_date is null → first time ever, init today ───
                    android.util.Log.d("RESET_DAILY_HABITS_FOR_NEW_DAY",
                            "habit_date is null, initializing today");
                    finalBrush = false;
                    finalFloss = false;
                    // Reset habits only, don't touch streak
                    ApiService.updateHabitStatus(ProfileActivity.this, false, false, today);

                } else if (dbHabitDate.equals(today)) {
                    // ── Same day: use DB values exactly, no reset ─────────
                    android.util.Log.d("RESET_DAILY_HABITS_FOR_NEW_DAY",
                            "Same day — using saved values from DB");
                    finalBrush = dbBrushing;
                    finalFloss = dbFlossing;

                } else {
                    // ── Old date: reset habits for new day, keep streak ───
                    android.util.Log.d("RESET_DAILY_HABITS_FOR_NEW_DAY",
                            "New day detected! Old=" + dbHabitDate + " resetting habits");
                    finalBrush = false;
                    finalFloss = false;
                    // Reset habits only (streak_count unchanged here)
                    ApiService.updateHabitStatus(ProfileActivity.this, false, false, today);
                }

                android.util.Log.d("DB_BRUSHING_DONE", "FINAL: " + finalBrush);
                android.util.Log.d("DB_FLOSSING_DONE", "FINAL: " + finalFloss);

                // Update in-memory state
                brushDone = finalBrush;
                flossDone = finalFloss;
                currentStreakCount = dbStreak;

                runOnUiThread(() -> {
                    updateHabitUI();
                    updateStreakUI(dbStreak);
                });

            } catch (Exception e) {
                android.util.Log.e("HABIT_LOAD", "Error loading habit status", e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // D) checkAndUpdateStreak — called on background thread after habit saved
    //    Only runs when both habits are done
    // ─────────────────────────────────────────────────────────────────────────
    private void checkAndUpdateStreak() {
        // Already on background thread — no need to spawn another
        if (!brushDone || !flossDone) {
            android.util.Log.d("BOTH_HABITS_DONE", "Not both done yet, skip streak check");
            return;
        }
        android.util.Log.d("BOTH_HABITS_DONE", "Both habits done! Checking streak...");

        try {
            // Re-fetch to get latest last_streak_date and streak_count
            JsonObject result = ApiService.getHabitStatus(ProfileActivity.this);
            if (!result.has("success") || !result.get("success").getAsBoolean()) return;

            JsonObject data = result.getAsJsonObject("data");
            int    currentStreak  = safeInt(data,  "streak_count");
            String lastStreakDate  = safeStr(data,  "last_streak_date");
            String today           = todayStr();
            String yesterday       = yesterdayStr();

            int newStreak;

            if (today.equals(lastStreakDate)) {
                // Already counted today — do nothing
                android.util.Log.d("STREAK_ALREADY_COUNTED_TODAY",
                        "Streak already updated today: " + lastStreakDate);
                return;

            } else if (lastStreakDate == null || lastStreakDate.isEmpty()) {
                // No previous streak → start at 1
                newStreak = 1;
                android.util.Log.d("STREAK_STARTED",
                        "No previous streak, starting at 1");

            } else if (yesterday.equals(lastStreakDate)) {
                // Completed yesterday → increment
                newStreak = currentStreak + 1;
                android.util.Log.d("STREAK_INCREMENTED",
                        currentStreak + " → " + newStreak);

            } else {
                // Missed one or more days → reset to 1
                newStreak = 1;
                android.util.Log.d("STREAK_RESET_AFTER_MISSED_DAY",
                        "Last=" + lastStreakDate + " > 1 day ago, reset to 1");
            }

            // Persist new streak
            ApiService.updateStreakInSupabase(ProfileActivity.this, newStreak, today);
            currentStreakCount = newStreak;
            runOnUiThread(() -> updateStreakUI(newStreak));

        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "Error in checkAndUpdateStreak", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load user profile (name, email, avatar, assessment count)
    // ─────────────────────────────────────────────────────────────────────────
    private void loadProfile() {
        new Thread(() -> {
            try {
                JsonObject result = ApiService.getProfile(ProfileActivity.this);
                if (!result.has("success") || !result.get("success").getAsBoolean()) return;

                JsonObject profile = result.getAsJsonObject("profile");
                String name  = profile.has("name") ? profile.get("name").getAsString() : "DentNova User";
                String email = safeStr(profile, "email");
                if (email.isEmpty()) email = "No Email";
                String photo = safeStr(profile, "photo_url");

                JsonObject ah = ApiService.getAssessmentHistory(ProfileActivity.this);
                int assessmentCount = ah.getAsJsonArray("assessments").size();

                final String fEmail = email;
                final String fPhoto = photo;
                final int fCount    = assessmentCount;

                runOnUiThread(() -> {
                    tvName.setText(name);
                    tvEmail.setText(fEmail);
                    tvAgeGender.setText("Profile Active");
                    tvAssessments.setText(fCount + " Assessments");

                    if (!fPhoto.isEmpty()) {
                        if (fPhoto.startsWith("http://") || fPhoto.startsWith("https://")) {
                            try { Glide.with(this).load(fPhoto).into(civAvatar); }
                            catch (Exception ignored) {}
                        } else {
                            try {
                                byte[] decoded = android.util.Base64.decode(
                                        fPhoto, android.util.Base64.DEFAULT);
                                android.graphics.Bitmap bmp =
                                        android.graphics.BitmapFactory.decodeByteArray(
                                                decoded, 0, decoded.length);
                                civAvatar.setImageBitmap(bmp);
                            } catch (Exception ignored) {}
                        }
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ProfileActivity", "loadProfile error", e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEBUG: Long-press streak card → reset today's habits in Supabase
    // ─────────────────────────────────────────────────────────────────────────
    private void resetHabitsDebug() {
        String today = todayStr();
        brushDone = false;
        flossDone = false;
        updateHabitUI();
        Toast.makeText(this, "🔄 Resetting habits in DB (debug)…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ApiService.updateHabitStatus(ProfileActivity.this, false, false, today);
                android.util.Log.d("HABIT_DEBUG", "DEBUG RESET done. date=" + today);
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this,
                        "✅ Both habits reset for today", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                android.util.Log.e("HABIT_DEBUG", "Reset error", e);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // E) UI update methods
    // ─────────────────────────────────────────────────────────────────────────
    private void updateStreakUI(int streak) {
        currentStreakCount = streak;
        if (streak <= 0) {
            tvStreakTitle.setText("No streak yet");
            tvStreakSub.setText("Complete both habits today to start your streak");
        } else if (streak == 1) {
            tvStreakTitle.setText("🔥 1 day streak");
            tvStreakSub.setText("Great start! Keep going tomorrow!");
        } else {
            tvStreakTitle.setText("🔥 " + streak + " day streak");
            tvStreakSub.setText("Amazing! Keep completing both habits daily!");
        }
        updateAchievements(streak);
    }

    private void updateHabitUI() {
        int cardBg = com.google.android.material.color.MaterialColors
                .getColor(cardBrush, com.google.android.material.R.attr.colorSurfaceVariant);
        int inactive = com.google.android.material.color.MaterialColors
                .getColor(tvBrushStatus, com.google.android.material.R.attr.colorOnSurfaceVariant);
        int green = Color.parseColor("#43A047");

        cardBrush.setCardBackgroundColor(cardBg);
        if (brushDone) {
            tvBrushStatus.setText("Done ✓ 🔥");
            tvBrushStatus.setTextColor(green);
        } else {
            tvBrushStatus.setText("Not done");
            tvBrushStatus.setTextColor(inactive);
        }

        cardFloss.setCardBackgroundColor(cardBg);
        if (flossDone) {
            tvFlossStatus.setText("Done ✓ 🔥");
            tvFlossStatus.setTextColor(green);
        } else {
            tvFlossStatus.setText("Not done");
            tvFlossStatus.setTextColor(inactive);
        }

        updateAchievements(currentStreakCount);
    }

    private void updateAchievements(int streak) {
        int green    = Color.parseColor("#43A047");
        int inactive = com.google.android.material.color.MaterialColors
                .getColor(tvHygieneStatus, com.google.android.material.R.attr.colorOnSurfaceVariant);

        // Hygiene Star — streak >= 1
        if (streak >= 1) {
            tvHygieneStatus.setText("Earned ✓");
            tvHygieneStatus.setTextColor(green);
        } else {
            tvHygieneStatus.setText("Locked");
            tvHygieneStatus.setTextColor(inactive);
        }

        // Consistent — streak >= 30
        if (streak >= 30) {
            tvConsistentStatus.setText("Earned ✓");
            tvConsistentStatus.setTextColor(green);
        } else {
            tvConsistentStatus.setText(streak > 0 ? streak + "/30 days" : "Locked");
            tvConsistentStatus.setTextColor(inactive);
        }

        // Oral Care Pro — streak >= 60
        if (streak >= 60) {
            tvProStatus.setText("Earned ✓");
            tvProStatus.setTextColor(green);
        } else {
            tvProStatus.setText(streak > 0 ? streak + "/60 days" : "Locked");
            tvProStatus.setTextColor(inactive);
        }

        // Improver — streak >= 90
        if (tvImproverStatus != null) {
            if (streak >= 90) {
                tvImproverStatus.setText("Earned ✓");
                tvImproverStatus.setTextColor(green);
            } else {
                tvImproverStatus.setText(streak > 0 ? streak + "/90 days" : "Locked");
                tvImproverStatus.setTextColor(inactive);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────────────────
    private void performLogout() {
        new Thread(() -> {
            try { ApiService.logout(ProfileActivity.this); }
            catch (Exception e) {
                new com.dentnova.app.utils.SessionManager(ProfileActivity.this).clearSession();
            }
            runOnUiThread(() -> {
                new com.dentnova.app.utils.SessionManager(ProfileActivity.this).clearSession();
                android.content.SharedPreferences prefs =
                        getSharedPreferences("dentnova_prefs", MODE_PRIVATE);
                boolean seenOnboarding = prefs.getBoolean("has_seen_onboarding", false);
                String themeMode = prefs.getString("theme_mode", "system");
                prefs.edit().clear()
                        .putBoolean("has_seen_onboarding", seenOnboarding)
                        .putString("theme_mode", themeMode)
                        .apply();
                FirebaseAuth.getInstance().signOut();
                GoogleSignInOptions gso = new GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
                GoogleSignInClient gsc = GoogleSignIn.getClient(this, gso);
                gsc.signOut().addOnCompleteListener(this, t ->
                        gsc.revokeAccess().addOnCompleteListener(this, t2 ->
                                android.util.Log.d("LOGOUT", "Google session revoked")));
                Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Safe JSON helpers
    // ─────────────────────────────────────────────────────────────────────────
    private static String safeStr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }

    private static boolean safeBool(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return false;
        try { return obj.get(key).getAsBoolean(); }
        catch (Exception e) { return false; }
    }

    private static int safeInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try { return obj.get(key).getAsInt(); }
        catch (Exception e) { return 0; }
    }
}