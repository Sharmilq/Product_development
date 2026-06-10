package com.dentnova.app.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.dentnova.app.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HomeActivity — exact Java replica of home_screen.dart
 *
 * Bottom nav tabs (replaces Flutter BottomNavigationBar):
 *   navCheck        → AssessmentActivity
 *   navScan         → ToothScanActivity
 *   navHome         → shows HomeFragment content (default)
 *   navNotifications→ shows NotificationsFragment (inline, replaces _buildNotificationsTab)
 *   navProfile      → ProfileActivity
 *
 * Brushing Timer overlay — replaces Flutter _buildBrushingTimerOverlay()
 *   60s countdown dialog with CircularProgressIndicator
 *   Colors: green (done), red (<30s), cyan (running)
 */
public class HomeActivity extends AppCompatActivity {

    private static final int TOTAL_SECONDS = 120;
    private TextView tvLastCheck, tvNextVisit, tvBrushReplace;

    private BottomNavigationView bottomNav;
    private FrameLayout contentContainer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Timer state
    private CountDownTimer countDownTimer;
    private int secondsLeft = TOTAL_SECONDS;
    private boolean timerRunning = false;

    // Data
    private JsonObject profile;
    private JsonArray assessments;
    private JsonArray notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }

        contentContainer = findViewById(R.id.contentContainer);
        bottomNav        = findViewById(R.id.bottomNav);

        setupBottomNav();
        showHomeContent();  // default tab = Home (index 2)
        loadData();
    }

    // ── Load all data (replicates initState() calls) ──────────────────────
    private void loadData() {
        executor.execute(() -> {
            int userId = new SessionManager(this).getUserId();
            String email = new SessionManager(this).getEmail();
            android.util.Log.d("CURRENT_USER_ID", "CURRENT_USER_ID: " + userId);
            android.util.Log.d("CURRENT_USER_EMAIL", "CURRENT_USER_EMAIL: " + email);

            try {
                JsonObject profileResponse = ApiService.getProfile(this);

                if (profileResponse.has("success") &&
                        profileResponse.get("success").getAsBoolean()) {
                    profile = profileResponse.getAsJsonObject("profile");
                }
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error fetching user profile", e);
            }

            try {
                JsonObject ah = ApiService.getAssessmentHistory(this);
                assessments = ah.has("assessments")
                        ? ah.getAsJsonArray("assessments")
                        : new JsonArray();
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error fetching assessment history", e);
                assessments = new JsonArray();
            }

            try {
                JsonObject nn = ApiService.getNotifications(this);
                android.util.Log.d("NOTIF_RAW", nn.toString());
                notifications = nn.has("notifications")
                        ? nn.getAsJsonArray("notifications")
                        : new JsonArray();

            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error fetching notifications", e);
                notifications = new JsonArray();
            }
            try {
                JsonObject vr = ApiService.getVisitReminder(this);

                if (vr.has("success") && vr.get("success").getAsBoolean()) {
                    JsonArray visits = vr.getAsJsonArray("visits");

                    if (visits.size() > 0) {
                        JsonObject latest = visits.get(0).getAsJsonObject();

                        getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("home_visit_date_" + userId, latest.get("visit_date").getAsString())
                                .apply();
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error fetching visit reminders", e);
            }
            runOnUiThread(this::refreshHomeUI);

        });
    }

    // ── Bottom nav setup ─────────────────────────────────────────────────
    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navCheck) {
                startActivity(new Intent(this, AssessmentActivity.class));
            } else if (id == R.id.navScan) {
                startActivity(new Intent(this, ToothScanActivity.class));
            } else if (id == R.id.navHome) {
                showHomeContent();
            } else if (id == R.id.navNotifications) {
                showNotificationsContent();
            } else if (id == R.id.navProfile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });
        bottomNav.setSelectedItemId(R.id.navHome);
    }

    // ── Home tab content ─────────────────────────────────────────────────
    private void showHomeContent() {
        contentContainer.removeAllViews();
        View homeView = LayoutInflater.from(this).inflate(R.layout.fragment_home, contentContainer, false);
        homeView.setTag("home_content");
        contentContainer.addView(homeView);

        // Wire up brushing timer card tap
        View cardBrushTimer = homeView.findViewById(R.id.cardBrushingTimer);
        if (cardBrushTimer != null) {
            cardBrushTimer.setOnClickListener(v -> showBrushingTimerDialog());
        }

        // Wire quick action grid items
        wireQuickActions(homeView);
        populateHomeData(homeView);
    }

    private void refreshHomeUI() {
        // Re-populate if home tab is visible
        View homeView = contentContainer.findViewWithTag("home_content");
        if (homeView != null) populateHomeData(homeView);
    }

    private void populateHomeData(View homeView) {
        // Greeting + name
        TextView tvGreeting = homeView.findViewById(R.id.tvGreeting);
        TextView tvUserName = homeView.findViewById(R.id.tvUserName);
        if (tvGreeting != null) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : hour < 21 ? "Good evening" : "Good night";
            tvGreeting.setText(greeting + ", 👋");
        }
        if (tvUserName != null && profile != null && profile.has("name")) {
            tvUserName.setText(profile.get("name").getAsString());
        }

        // Avatar photo
        ImageView civAvatar = homeView.findViewById(R.id.civAvatar);

        if (civAvatar != null && profile != null &&
                profile.has("photo_url") &&
                !profile.get("photo_url").isJsonNull()) {

            String photoUrl = profile.get("photo_url").getAsString();

            if (!photoUrl.trim().isEmpty()) {
                if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                    try {
                        Glide.with(HomeActivity.this)
                                .load(photoUrl)
                                .into(civAvatar);
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Error loading photo URL via Glide", e);
                    }
                } else {
                    try {
                        byte[] decoded =
                                android.util.Base64.decode(
                                        photoUrl,
                                        android.util.Base64.DEFAULT
                                    );

                        android.graphics.Bitmap bmp =
                                android.graphics.BitmapFactory
                                        .decodeByteArray(
                                                decoded,
                                                0,
                                                decoded.length
                                        );

                        civAvatar.setImageBitmap(bmp);
                    } catch (IllegalArgumentException e) {
                        android.util.Log.e("HomeActivity", "Bad Base64 photo_url", e);
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Error decoding Base64 photo", e);
                    }
                }
            }
        }

        // Score card
        CircularProgressIndicator scoreProgress = homeView.findViewById(R.id.scoreProgress);
        TextView tvScore      = homeView.findViewById(R.id.tvScore);
        TextView tvScoreLabel = homeView.findViewById(R.id.tvScoreLabel);
        TextView tvScoreSub   = homeView.findViewById(R.id.tvScoreSub);
        TextView tvStartNow   = homeView.findViewById(R.id.tvStartNow);
        tvLastCheck =
                homeView.findViewById(R.id.tvLastCheck);

        tvNextVisit =
                homeView.findViewById(R.id.tvNextVisit);

        tvBrushReplace =
                homeView.findViewById(R.id.tvBrushReplace);

        if (assessments != null && assessments.size() > 0) {
            JsonObject latest = assessments.get(0).getAsJsonObject();
            int score = latest.has("score") ? latest.get("score").getAsInt() : 0;
            String label = latest.has("risk") ? latest.get("risk").getAsString() : "";

            if (scoreProgress != null) scoreProgress.setProgress(score);
            if (tvScore      != null) tvScore.setText(String.valueOf(score));
            if (tvScoreLabel != null) tvScoreLabel.setText(label);
            if (tvScoreSub   != null) tvScoreSub.setText("Assessment completed");
            if (tvStartNow   != null) tvStartNow.setText("Re-assess →");
            if (tvLastCheck != null) {
                tvLastCheck.setText(
                        assessments != null && assessments.size() > 0
                                ? "Completed"
                                : "—"
                );
            }

            if (tvNextVisit != null) {
                int userId = new SessionManager(HomeActivity.this).getUserId();
                String visit =
                        getSharedPreferences(
                                "dentnova_prefs",
                                MODE_PRIVATE
                        ).getString(
                                "home_visit_date_" + userId,
                                "—"
                        );

                tvNextVisit.setText(visit);
            }

            if (tvBrushReplace != null) {
                tvBrushReplace.setText("Scheduled");
            }
        }

        // Avatar click → ProfileActivity
        if (civAvatar != null) {
            civAvatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        // Assessment card tap
        View btnStart = homeView.findViewById(R.id.btnStartAssessment);
        if (btnStart != null) btnStart.setOnClickListener(v ->
                startActivity(new Intent(this, AssessmentActivity.class)));

        // Recent assessments
        populateAssessments(homeView);
    }

    private void populateAssessments(View homeView) {
        View cardNoAss = homeView.findViewById(R.id.cardNoAssessments);
        LinearLayout llAss = homeView.findViewById(R.id.llAssessments);
        if (llAss == null) return;

        if (assessments == null || assessments.size() == 0) {
            if (cardNoAss != null) cardNoAss.setVisibility(View.VISIBLE);
            llAss.setVisibility(View.GONE);
        } else {
            if (cardNoAss != null) cardNoAss.setVisibility(View.GONE);
            llAss.setVisibility(View.VISIBLE);
            llAss.removeAllViews();

            int count = Math.min(2, assessments.size());
            for (int i = 0; i < count; i++) {
                JsonObject a = assessments.get(i).getAsJsonObject();
                View row = LayoutInflater.from(this).inflate(R.layout.item_assessment_row, llAss, false);

                TextView tvScore = row.findViewById(R.id.tvAssessmentScore);
                TextView tvLabel = row.findViewById(R.id.tvAssessmentLabel);
                TextView tvDate  = row.findViewById(R.id.tvAssessmentDate);

                if (tvScore != null && a.has("score")) tvScore.setText(a.get("score").getAsString());
                if (tvLabel != null && a.has("risk")) tvLabel.setText(a.get("risk").getAsString());
                if (tvDate  != null && a.has("created_at")) tvDate.setText(a.get("created_at").getAsString());

                llAss.addView(row);
            }
        }
    }

    /** Wire 6 quick action cards: Assessment|Scan|Education|Reminders|Visit|HowItWorks */
    private void wireQuickActions(View homeView) {
        // These are populated dynamically in the GridLayout
        // For simplicity, using a programmatic approach matching Flutter's _buildQuickActionsGrid()
        android.widget.GridLayout grid = homeView.findViewById(R.id.gridQuickActions);
        if (grid == null) return;

        String[] labels   = {"Assessment", "Tooth Scan", "Education", "Reminders", "Visit Reminder", "How it Works"};
        String[] subs     = {"Tap to open", "Tap to open", "Tap to open", "Tap to open", "Schedule checkup", "App overview"};
        int[]    icons    = {    R.drawable.qa_assessment,
                R.drawable.qa_scan,
                R.drawable.qa_education,
                R.drawable.qa_reminder,
                R.drawable.qa_visit,
                R.drawable.qa_info};
        int[]    colors   = {0xFF3B82F6, 0xFF10B981, 0xFF8B5CF6, 0xFF00BCD4, 0xFFEF4444, 0xFFF59E0B};
        int[]    bgColors = {0xFFEFF6FF, 0xFFECFDF5, 0xFFF5F3FF, 0xFFE0F7FA, 0xFFFEF2F2, 0xFFFFFBEB};
        Class<?>[] targets = {AssessmentActivity.class, ToothScanActivity.class,
                EducationActivity.class, RemindersActivity.class,
                VisitReminderActivity.class, HowItWorksActivity.class};

        float dp = getResources().getDisplayMetrics().density;
        int itemW = (int) ((getResources().getDisplayMetrics().widthPixels - 40 * dp - 12 * dp) / 2);

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            View card = LayoutInflater.from(this).inflate(R.layout.item_quick_action_card, grid, false);

            // Set icon, colors, text
            ImageView iv  = card.findViewById(R.id.ivActionIcon);
            TextView tvL  = card.findViewById(R.id.tvActionLabel);
            TextView tvS  = card.findViewById(R.id.tvActionSub);

            if (iv     != null) iv.setImageResource(icons[i]);
            if (tvL    != null) tvL.setText(labels[i]);
            if (tvS    != null) tvS.setText(subs[i]);

            android.widget.GridLayout.LayoutParams lp =
                    new android.widget.GridLayout.LayoutParams();
            lp.width  = itemW;
            lp.height = (int) (120 * dp);
            lp.columnSpec = android.widget.GridLayout.spec(i % 2);
            lp.rowSpec    = android.widget.GridLayout.spec(i / 2);
            lp.setMargins((int)(6*dp),(int)(6*dp),(int)(6*dp),(int)(6*dp));
            card.setLayoutParams(lp);

            card.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, targets[idx])));
            grid.addView(card);
        }
    }

    // ── Notifications tab ────────────────────────────────────────────────
    private void showNotificationsContent() {
        android.util.Log.d("NOTIF_CLICK", "Notifications tab opened");

        contentContainer.removeAllViews();

        View notifView = LayoutInflater.from(this)
                .inflate(R.layout.fragment_notifications, contentContainer, false);

        contentContainer.addView(notifView);

        View btnBackNotif = notifView.findViewById(R.id.btnBackNotif);
        if (btnBackNotif != null) {
            btnBackNotif.setOnClickListener(v -> showHomeContent());
        }

        LinearLayout llNotifs = notifView.findViewById(R.id.llNotifications);
        View emptyState = notifView.findViewById(R.id.layoutEmptyNotifs);

        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (llNotifs != null) llNotifs.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                JsonObject nn = ApiService.getNotifications(this);

                notifications = nn.has("notifications")
                        ? nn.getAsJsonArray("notifications")
                        : new JsonArray();

            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "Error loading notifications in notifications view", e);
                notifications = new JsonArray();
            }

            runOnUiThread(() -> {

                if (llNotifs == null || emptyState == null) return;

                if (notifications == null || notifications.size() == 0) {
                    emptyState.setVisibility(View.VISIBLE);
                    llNotifs.setVisibility(View.GONE);
                    return;
                }

                emptyState.setVisibility(View.GONE);
                llNotifs.setVisibility(View.VISIBLE);
                llNotifs.removeAllViews();

                for (int i = 0; i < notifications.size(); i++) {

                    JsonObject n = notifications.get(i).getAsJsonObject();

                    View row = LayoutInflater.from(this)
                            .inflate(R.layout.item_notification_row, llNotifs, false);

                    TextView tvT = row.findViewById(R.id.tvNotifTitle);
                    TextView tvB = row.findViewById(R.id.tvNotifBody);
                    TextView tvTime = row.findViewById(R.id.tvNotifTime);

                    if (tvT != null && n.has("title")) {
                        tvT.setText(n.get("title").getAsString());
                    }

                    if (tvB != null && n.has("body")) {
                        tvB.setText(n.get("body").getAsString());
                    }

                    if (tvTime != null && n.has("created_at")) {
                        tvTime.setText(n.get("created_at").getAsString());
                    }

                    llNotifs.addView(row);
                }
            });
        });
    }
    // ── Brushing Timer Dialog ─────────────────────────────────────────────
    /**
     * Exact replica of _buildBrushingTimerOverlay() from home_screen.dart
     * Modal dialog with:
     *   - Circular progress (130dp) changing color based on time
     *   - MM:SS countdown text
     *   - "Brush in small circles..." hint text
     *   - Reset + Start/Pause buttons
     */
    private void showBrushingTimerDialog() {
        secondsLeft  = TOTAL_SECONDS;
        timerRunning = false;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_brushing_timer);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setDimAmount(0.5f);
        }

        CircularProgressIndicator progressRing = dialog.findViewById(R.id.timerProgress);
        TextView tvTime    = dialog.findViewById(R.id.tvTimerTime);
        TextView tvHint    = dialog.findViewById(R.id.tvTimerHint);
        View btnClose      = dialog.findViewById(R.id.btnCloseTimer);
        View btnReset      = dialog.findViewById(R.id.btnResetTimer);
        TextView btnStart  = dialog.findViewById(R.id.btnStartPause);

        // Initial state
        updateTimerUI(progressRing, tvTime, tvHint, btnStart);

        btnClose.setOnClickListener(v -> {
            stopTimer();
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            stopTimer();
            secondsLeft  = TOTAL_SECONDS;
            timerRunning = false;
            updateTimerUI(progressRing, tvTime, tvHint, btnStart);
        });

        btnStart.setOnClickListener(v -> {

            if (secondsLeft == 0) {
                secondsLeft = TOTAL_SECONDS;
                updateTimerUI(progressRing, tvTime, tvHint, btnStart);
            }

            if (timerRunning) {
                stopTimer();
                btnStart.setText("Resume");
                tvHint.setText("Timer paused");
            } else {
                startTimer(progressRing, tvTime, tvHint, btnStart);

                if (secondsLeft == TOTAL_SECONDS) {
                    btnStart.setText("Pause");
                } else {
                    btnStart.setText("Pause");
                }

                tvHint.setText("Brush in small circles...");
            }
        });

        dialog.setOnDismissListener(d -> stopTimer());
        dialog.show();
    }

    private void startTimer(CircularProgressIndicator ring, TextView tvTime, TextView tvHint, TextView btnStart) {
        timerRunning = true;
        countDownTimer = new CountDownTimer((long) secondsLeft * 1000, 1000) {
            @Override
            public void onTick(long ms) {
                secondsLeft = (int)(ms / 1000);
                updateTimerUI(ring, tvTime, tvHint, btnStart);
            }
            @Override
            public void onFinish() {
                secondsLeft  = 0;
                timerRunning = false;
                updateTimerUI(ring, tvTime, tvHint, btnStart);
                tvHint.setText("Great brushing! Your teeth thank you 😊");
                btnStart.setText("Restart");
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) { countDownTimer.cancel(); countDownTimer = null; }
        timerRunning = false;
    }

    private void updateTimerUI(CircularProgressIndicator ring, TextView tvTime, TextView tvHint, TextView btnStart) {
        if (tvTime == null) return;
        int m = secondsLeft / 60, s = secondsLeft % 60;
        tvTime.setText(String.format("%02d:%02d", m, s));

        boolean done = secondsLeft == 0;
        // Color: successGreen (done), riskRed (<30s), primary (normal) — matches Flutter logic
        int color = done ? 0xFF43A047 : secondsLeft < 30 ? 0xFFE53935 : 0xFF00BCD4;
        if (ring != null) {
            ring.setIndicatorColor(color);
            ring.setProgress((int)(((float) secondsLeft / TOTAL_SECONDS) * 100));
        }
        if (tvHint != null && done) tvHint.setText("Done! 🎉");
        if (btnStart != null && done) btnStart.setText("Start");
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        executor.shutdown();
    }
}
