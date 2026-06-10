package com.dentnova.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.net.Uri;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dentnova.app.R;

import java.util.ArrayList;
import java.util.List;

public class EducationActivity extends AppCompatActivity {

    private int selectedQuizAnswer = -1;
    private boolean quizAnswered = false;
    private static final int CORRECT_ANSWER_INDEX = 1;

    private View btnOpt0, btnOpt1, btnOpt2;
    private TextView tvOpt0, tvOpt1, tvOpt2;
    private ImageView icCheck0, icCheck1, icCheck2;

    private View quizFeedbackContainer;
    private ImageView icQuizFeedback;
    private TextView tvQuizFeedback, tvTryAgain;

    private static final int COLOR_PRIMARY = 0xFF00BCD4;
    private static final int COLOR_BG = 0xFFF5F8FA;
    private static final int COLOR_TEXT_PRI = 0xFF1A2B3C;
    private static final int COLOR_TEXT_LIGHT = 0xFFB0C0CC;
    private static final int COLOR_SUCCESS = 0xFF10B981;
    private static final int COLOR_RISK_RED = 0xFFEF4444;
    private static final int COLOR_WARNING = 0xFFF59E0B;
    private static final int COLOR_BORDER = 0xFFE0E8EF;

    private final String[] encouragements = {
            "Great job! 🎉",
            "Well done! 👏",
            "Correct! You're a dental pro! 🦷✨"
    };

    static class Article {
        String title, subtitle;
        int iconRes, color, bgColor;

        Article(String title, String subtitle, int iconRes, int color, int bgColor) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.color = color;
            this.bgColor = bgColor;
        }
    }

    private final List<Article> articles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Education");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buildArticles();
        setupQuiz();
        setupArticleList();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void buildArticles() {
        articles.clear();

        articles.add(new Article("Gum care", "Healthy gums, healthy life",
                R.drawable.ic_gum_care, 0xFF10B981, 0xFFECFDF5));

        articles.add(new Article("Tooth sensitivity", "Causes and treatments",
                R.drawable.ic_tooth_sensitivity, 0xFF3B82F6, 0xFFEFF6FF));

        articles.add(new Article("Whitening myths", "Facts vs fiction",
                R.drawable.ic_whitening, 0xFFF59E0B, 0xFFFFFBEB));

        articles.add(new Article("Flossing", "Why, when and how to floss",
                R.drawable.ic_flossing, 0xFF8B5CF6, 0xFFF5F3FF));

        articles.add(new Article("Brushing techniques", "Master the perfect brush",
                R.drawable.ic_brushing, 0xFF00BCD4, 0xFFE0F7FA));
    }

    private void setupQuiz() {
        btnOpt0 = findViewById(R.id.btn_opt0);
        btnOpt1 = findViewById(R.id.btn_opt1);
        btnOpt2 = findViewById(R.id.btn_opt2);

        tvOpt0 = findViewById(R.id.tv_opt0);
        tvOpt1 = findViewById(R.id.tv_opt1);
        tvOpt2 = findViewById(R.id.tv_opt2);

        icCheck0 = findViewById(R.id.ic_check0);
        icCheck1 = findViewById(R.id.ic_check1);
        icCheck2 = findViewById(R.id.ic_check2);

        quizFeedbackContainer = findViewById(R.id.quiz_feedback_container);
        icQuizFeedback = findViewById(R.id.ic_quiz_feedback);
        tvQuizFeedback = findViewById(R.id.tv_quiz_feedback);
        tvTryAgain = findViewById(R.id.tv_try_again);

        btnOpt0.setOnClickListener(v -> onQuizOptionTapped(0));
        btnOpt1.setOnClickListener(v -> onQuizOptionTapped(1));
        btnOpt2.setOnClickListener(v -> onQuizOptionTapped(2));

        tvTryAgain.setOnClickListener(v -> resetQuiz());

        resetQuiz();
    }

    private void onQuizOptionTapped(int index) {
        if (quizAnswered) return;
        selectedQuizAnswer = index;
        quizAnswered = true;
        refreshQuizUI();
    }

    private void resetQuiz() {
        selectedQuizAnswer = -1;
        quizAnswered = false;
        refreshQuizUI();
    }

    private void refreshQuizUI() {
        View[] btns = {btnOpt0, btnOpt1, btnOpt2};
        TextView[] tvs = {tvOpt0, tvOpt1, tvOpt2};
        ImageView[] checks = {icCheck0, icCheck1, icCheck2};

        for (int i = 0; i < 3; i++) {
            boolean isSelected = selectedQuizAnswer == i;
            boolean isCorrect = i == CORRECT_ANSWER_INDEX;

            int bgColor = COLOR_BG;
            int borderColor = COLOR_BORDER;
            int textColor = COLOR_TEXT_PRI;

            if (quizAnswered) {
                if (isCorrect) {
                    bgColor = 0x1A10B981;
                    borderColor = COLOR_SUCCESS;
                    textColor = COLOR_SUCCESS;
                } else if (isSelected) {
                    bgColor = 0x14EF4444;
                    borderColor = COLOR_RISK_RED;
                    textColor = COLOR_RISK_RED;
                } else {
                    textColor = COLOR_TEXT_LIGHT;
                }
            }

            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();

            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(20));
            bg.setColor(bgColor);
            bg.setStroke(dpToPx(1), borderColor);

            btns[i].setBackground(bg);
            tvs[i].setTextColor(textColor);
            checks[i].setVisibility(quizAnswered && isCorrect ? View.VISIBLE : View.GONE);
        }

        if (quizAnswered) {
            boolean correct = selectedQuizAnswer == CORRECT_ANSWER_INDEX;

            quizFeedbackContainer.setVisibility(View.VISIBLE);
            tvTryAgain.setVisibility(View.VISIBLE);

            android.graphics.drawable.GradientDrawable fb =
                    new android.graphics.drawable.GradientDrawable();

            fb.setCornerRadius(dpToPx(10));
            fb.setColor(correct ? 0x1A10B981 : 0xFFFFF8E1);
            quizFeedbackContainer.setBackground(fb);

            icQuizFeedback.setImageResource(correct
                    ? R.drawable.ic_celebration
                    : R.drawable.ic_info_outline);

            icQuizFeedback.setColorFilter(correct ? COLOR_SUCCESS : COLOR_WARNING);
            tvQuizFeedback.setTextColor(correct ? COLOR_SUCCESS : COLOR_WARNING);

            tvQuizFeedback.setText(correct
                    ? encouragements[(int) (System.currentTimeMillis() % encouragements.length)]
                    : "The correct answer is 2 minutes. Brush twice daily for best results!");
        } else {
            quizFeedbackContainer.setVisibility(View.GONE);
            tvTryAgain.setVisibility(View.GONE);
        }
    }

    private void setupArticleList() {
        RecyclerView rv = findViewById(R.id.rv_articles);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ArticleAdapter());
    }

    private class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_article, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Article a = articles.get(position);

            h.tvTitle.setText(a.title);
            h.tvSubtitle.setText(a.subtitle);
            h.ivIcon.setImageResource(a.iconRes);
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(EducationActivity.this, ArticleDetailActivity.class);
                intent.putExtra("article_index", position);
                startActivity(intent);
            });

            android.graphics.drawable.GradientDrawable iconBg =
                    new android.graphics.drawable.GradientDrawable();

            iconBg.setCornerRadius(dpToPx(12));
            iconBg.setColor(a.bgColor);
            h.iconContainer.setBackground(iconBg);
        }

        @Override
        public int getItemCount() {
            return articles.size();
        }

        class VH extends RecyclerView.ViewHolder {
            View iconContainer;
            ImageView ivIcon;
            TextView tvTitle, tvSubtitle;

            VH(@NonNull View itemView) {
                super(itemView);
                iconContainer = itemView.findViewById(R.id.icon_container);
                ivIcon = itemView.findViewById(R.id.iv_article_icon);
                tvTitle = itemView.findViewById(R.id.tv_article_title);
                tvSubtitle = itemView.findViewById(R.id.tv_article_subtitle);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}