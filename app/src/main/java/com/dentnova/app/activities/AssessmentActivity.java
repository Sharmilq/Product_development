package com.dentnova.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AssessmentActivity — exact Java replica of assessment_screen.dart
 *
 * 13 questions with emoji, options, warning banners for unhealthy answers.
 * Score computation: 100 - 8 per unhealthy answer.
 * On completion → AssessmentResultActivity with score.
 */
public class AssessmentActivity extends AppCompatActivity {

    // ── Question data (exact replica of _questions list in assessment_screen.dart) ──
    private static final String[] EMOJIS = {"🪥","⏱","🧵","🩸","🍬","🍭","🦷","😬","🚬","📅","💊","💧","😴"};
    private static final String[] QUESTIONS = {
            "How often do you brush your teeth?",
            "How long do you brush each time?",
            "Do you floss regularly?",
            "Do your gums bleed when brushing?",
            "How often do you consume sugary foods or drinks?",
            "Do you rinse your mouth after sugary foods?",
            "Do you experience tooth sensitivity?",
            "Do you grind your teeth at night?",
            "Do you smoke or use tobacco?",
            "When was your last dental checkup?",
            "Do you take any medications regularly?",
            "How much water do you drink daily?",
            "Do you experience dry mouth?"
    };
    private static final String[][] OPTIONS = {
            {"Twice a day","Once a day","Sometimes","Rarely"},
            {"2 minutes or more","About 1 minute","Less than 1 minute"},
            {"Daily","Few times a week","Rarely","Never"},
            {"Never","Sometimes","Often"},
            {"Rarely","Few times a week","Daily","Multiple times a day"},
            {"Always","Sometimes","Rarely","Never"},
            {"Never","Sometimes","Often","Always"},
            {"No","Occasionally","Often","I'm not sure"},
            {"Never","Occasionally","Daily"},
            {"Within 6 months","6–12 months ago","1–2 years ago","Over 2 years ago"},
            {"No","Yes"},
            {"More than 2L","1–2L","Less than 1L","Rarely drink water"},
            {"Never","Sometimes","Often","Always"}
    };
    private static final int[][] UNHEALTHY = {
            {2,3},{1,2},{2,3},{1,2},{2,3},{2,3},{2,3},{2},{1,2},{2,3},{},{2,3},{2,3}
    };
    private static final String[] WARNINGS = {
            "Brushing less than twice a day increases plaque and cavity risk.",
            "Dentists recommend brushing for at least 2 minutes each session.",
            "Not flossing significantly increases risk of gum problems.",
            "Frequent bleeding may indicate gingivitis — a dental visit is recommended.",
            "High sugar exposure puts you at high risk for cavities.",
            "Rinsing after sugary foods helps neutralise harmful acids.",
            "Frequent sensitivity may indicate enamel erosion — consult your dentist.",
            "Frequent grinding (bruxism) wears enamel. Ask your dentist about a night guard.",
            "Tobacco use stains teeth, causes gum disease, and raises oral cancer risk.",
            "Dental checkups every 6 months catch issues early. Book one soon!",
            null,
            "Low water intake can cause dry mouth, which increases cavity risk.",
            "Persistent dry mouth raises your risk of tooth decay and gum disease."
    };

    private int currentQuestion = 0;
    private final Map<Integer, Integer> answers = new HashMap<>();
    private boolean warningDismissed = false;

    // Views
    private TextView tvEmoji, tvQuestion, tvCount, tvPercent, tvWarning;
    private LinearProgressIndicator progressBar;
    private LinearLayout llOptions, layoutWarning;
    private MaterialButton btnNext, btnBack;
    private ImageView btnDismissWarning;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvEmoji          = findViewById(R.id.tvEmoji);
        tvQuestion       = findViewById(R.id.tvQuestion);
        tvCount          = findViewById(R.id.tvQuestionCount);
        tvPercent        = findViewById(R.id.tvProgressPercent);
        progressBar      = findViewById(R.id.progressBar);
        llOptions        = findViewById(R.id.llOptions);
        layoutWarning    = findViewById(R.id.layoutWarning);
        tvWarning        = findViewById(R.id.tvWarning);
        btnNext          = findViewById(R.id.btnNext);
        btnBack          = findViewById(R.id.btnBack);
        btnDismissWarning= findViewById(R.id.btnDismissWarning);

        btnDismissWarning.setOnClickListener(v -> {
            warningDismissed = true;
            layoutWarning.setVisibility(View.GONE);
        });

        btnBack.setOnClickListener(v -> {
            if (currentQuestion > 0) {
                currentQuestion--;
                warningDismissed = false;
                renderQuestion();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (!answers.containsKey(currentQuestion)) return;
            if (currentQuestion < QUESTIONS.length - 1) {
                currentQuestion++;
                warningDismissed = false;
                renderQuestion();
            } else {
                submitAssessment();
            }
        });

        renderQuestion();
    }

    /** Renders the current question — replicates Flutter build() of assessment_screen */
    private void renderQuestion() {
        int q = currentQuestion;
        float progress = (float)(q + 1) / QUESTIONS.length;

        tvEmoji.setText(EMOJIS[q]);
        tvQuestion.setText(QUESTIONS[q]);
        tvCount.setText("Question " + (q + 1) + " of " + QUESTIONS.length);
        tvPercent.setText(Math.round(progress * 100) + "%");
        progressBar.setProgress(Math.round(progress * 100));

        // Back button visibility
        btnBack.setVisibility(q > 0 ? View.VISIBLE : View.GONE);
        View spacer = findViewById(R.id.spacerBetweenButtons);
        if (spacer != null) spacer.setVisibility(q > 0 ? View.VISIBLE : View.GONE);

        // Next button text
        btnNext.setText(q < QUESTIONS.length - 1 ? "Next →" : "See Results");

        // Next enabled only when answer selected
        btnNext.setEnabled(answers.containsKey(q));
        btnNext.setAlpha(answers.containsKey(q) ? 1.0f : 0.5f);

        // Build options
        buildOptions(q);

        // Warning banner
        showWarningIfNeeded(q);
    }

    /** Builds option rows — replicates List.generate options in assessment_screen.dart */
    private void buildOptions(int q) {
        llOptions.removeAllViews();
        String[] opts = OPTIONS[q];
        Integer selected = answers.get(q);

        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            View opt = LayoutInflater.from(this).inflate(R.layout.item_assessment_option, llOptions, false);

            TextView tvText  = opt.findViewById(R.id.tvOptionText);
            View radioCircle = opt.findViewById(R.id.radioCircle);
            ImageView ivCheck= opt.findViewById(R.id.ivCheck);

            tvText.setText(opts[i]);

            boolean isSelected = selected != null && selected == i;
            // Apply selected vs unselected styles (mirrors Flutter decoration logic)
            if (isSelected) {
                opt.setBackgroundResource(R.drawable.bg_option_selected);
                tvText.setTextColor(0xFF00BCD4);
                tvText.setTypeface(tvText.getTypeface(), android.graphics.Typeface.BOLD);
                radioCircle.setBackgroundResource(R.drawable.bg_radio_selected);
                ivCheck.setVisibility(View.VISIBLE);
            } else {
                opt.setBackgroundResource(R.drawable.bg_option_unselected);
                tvText.setTextColor(0xFF1A2332);
                tvText.setTypeface(android.graphics.Typeface.DEFAULT);
                radioCircle.setBackgroundResource(R.drawable.bg_radio_unselected);
                ivCheck.setVisibility(View.GONE);
            }

            opt.setOnClickListener(v -> {
                answers.put(q, idx);
                warningDismissed = false;
                renderQuestion();
            });

            llOptions.addView(opt);
        }
    }

    /** Shows warning banner — replicates _shouldShowWarning() logic */
    private void showWarningIfNeeded(int q) {
        if (warningDismissed || WARNINGS[q] == null) {
            layoutWarning.setVisibility(View.GONE);
            return;
        }
        Integer sel = answers.get(q);
        if (sel == null) { layoutWarning.setVisibility(View.GONE); return; }

        boolean isUnhealthy = false;
        for (int u : UNHEALTHY[q]) { if (u == sel) { isUnhealthy = true; break; } }

        if (isUnhealthy) {
            tvWarning.setText(WARNINGS[q]);
            layoutWarning.setVisibility(View.VISIBLE);
        } else {
            layoutWarning.setVisibility(View.GONE);
        }
    }

    /** Computes score (100 - 8 per unhealthy answer) — replicates _computeScore() */
    private int computeScore() {
        int score = 100;
        for (int i = 0; i < QUESTIONS.length; i++) {
            Integer ans = answers.get(i);
            if (ans == null) continue;
            for (int u : UNHEALTHY[i]) {
                if (u == ans) { score -= 8; break; }
            }
        }
        return Math.max(0, Math.min(100, score));
    }

    /** Saves assessment and navigates to result screen */
    private void submitAssessment() {

        executor.execute(() -> {
            int score = 0;
            String label = "Moderate";

            try {
                com.google.gson.JsonObject data = new com.google.gson.JsonObject();

                data.addProperty("q1_brushing_frequency", OPTIONS[0][answers.get(0)]);
                data.addProperty("q2_brushing_duration", OPTIONS[1][answers.get(1)]);
                data.addProperty("q3_flossing", OPTIONS[2][answers.get(2)]);
                data.addProperty("q4_gum_bleeding", OPTIONS[3][answers.get(3)]);
                data.addProperty("q5_sugary_foods", OPTIONS[4][answers.get(4)]);
                data.addProperty("q6_rinse_after_sugar", OPTIONS[5][answers.get(5)]);
                data.addProperty("q7_tooth_sensitivity", OPTIONS[6][answers.get(6)]);
                data.addProperty("q8_teeth_grinding", OPTIONS[7][answers.get(7)]);
                data.addProperty("q9_tobacco", OPTIONS[8][answers.get(8)]);
                data.addProperty("q10_last_checkup", OPTIONS[9][answers.get(9)]);
                data.addProperty("q11_medications", OPTIONS[10][answers.get(10)]);
                data.addProperty("q12_water_intake", OPTIONS[11][answers.get(11)]);
                data.addProperty("q13_dry_mouth", OPTIONS[12][answers.get(12)]);

                com.google.gson.JsonObject prediction =
                        ApiService.predictAssessment(data);

                score = prediction.get("score").getAsInt();
                label = prediction.get("risk").getAsString();

                ApiService.saveAssessment(this, score, label, answers);

            } catch (Exception e) {
                e.printStackTrace();

                score = computeScore();
                label = score < 30 ? "High" : score < 60 ? "Moderate" : "Low";
            }

            int finalScore = score;
            String finalLabel = label;

            runOnUiThread(() -> {
                Intent intent = new Intent(this, AssessmentResultActivity.class);
                intent.putExtra("score", finalScore);
                intent.putExtra("label", finalLabel);

                getSharedPreferences("dentnova_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean(
                                "assessment_done_" + new com.dentnova.app.utils.SessionManager(this).getUserId(),
                                true
                        )
                        .apply();
                for (int i = 0; i < 13; i++) {
                    intent.putExtra("answer_" + i, OPTIONS[i][answers.get(i)]);
                }
                startActivity(intent);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
