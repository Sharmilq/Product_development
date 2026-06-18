package com.dentnova.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.net.Uri;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dentnova.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EducationActivity extends AppCompatActivity {

    private int selectedQuizAnswer = -1;
    private boolean quizAnswered = false;
    private int lastFactIndex = -1;

    private View btnOpt0, btnOpt1, btnOpt2, btnOpt3;
    private TextView tvOpt0, tvOpt1, tvOpt2, tvOpt3;
    private ImageView icCheck0, icCheck1, icCheck2, icCheck3;

    private TextView tvQuizQuestion;
    private TextView tvQuizExplanation;
    private TextView tvNextQuestion;

    private View quizFeedbackContainer;
    private ImageView icQuizFeedback;
    private TextView tvQuizFeedback;

    private NestedScrollView scrollView;

    private static final int COLOR_PRIMARY = 0xFF00BCD4;
    private static final int COLOR_BG = 0xFFF5F8FA;
    private static final int COLOR_TEXT_PRI = 0xFF1A2B3C;
    private static final int COLOR_TEXT_LIGHT = 0xFFB0C0CC;
    private static final int COLOR_SUCCESS = 0xFF10B981;
    private static final int COLOR_RISK_RED = 0xFFEF4444;
    private static final int COLOR_WARNING = 0xFFF59E0B;
    private static final int COLOR_BORDER = 0xFFE0E8EF;

    private final String[] dentalFacts = {
            "Saliva helps protect your teeth by neutralizing acids from food.",
            "Tooth enamel is the hardest substance your body produces.",
            "The average person brushes for only 45–70 seconds — dentists recommend 2 full minutes.",
            "Flossing removes up to 40% of plaque that your toothbrush misses.",
            "Your mouth hosts over 700 species of bacteria — most are harmless.",
            "A tooth can survive outside the mouth if kept in milk within 30 minutes of being knocked out.",
            "Gum disease (periodontitis) is linked to heart disease and diabetes.",
            "Fluoride strengthens enamel by replacing minerals lost to acid attacks.",
            "Children lose their first baby tooth around age 6–7 years.",
            "Drinking water after sugary food or drinks helps rinse away cavity-causing acids.",
            "Electric toothbrushes remove up to 21% more plaque than manual ones.",
            "Grinding your teeth (bruxism) can wear enamel down by up to 25% over time."
    };

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

    static class QuizQuestion {
        String question;
        String[] options;
        int correctIndex;
        String explanation;

        QuizQuestion(String question, String[] options, int correctIndex, String explanation) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    private final List<Article> articles = new ArrayList<>();
    private final List<QuizQuestion> quizQuestions = new ArrayList<>();
    private QuizQuestion currentQuestion;
    private int lastQuestionIndex = -1;

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
        buildQuizQuestions();
        setupDidYouKnow();
        setupQuiz();
        setupArticleList();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupDidYouKnow() {
        TextView tvFact = findViewById(R.id.tv_did_you_know_fact);
        View card = findViewById(R.id.card_did_you_know);
        if (tvFact == null || card == null) return;

        // Show a random fact on open
        tvFact.setText(pickRandomFact());

        // Tap card to refresh to another random fact
        card.setOnClickListener(v -> {
            String next = pickRandomFact();
            tvFact.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                tvFact.setText(next);
                tvFact.animate().alpha(1f).setDuration(200).start();
            }).start();
        });
    }

    private String pickRandomFact() {
        java.util.Random rng = new java.util.Random();
        int idx;
        if (dentalFacts.length > 1) {
            do {
                idx = rng.nextInt(dentalFacts.length);
            } while (idx == lastFactIndex);
        } else {
            idx = 0;
        }
        lastFactIndex = idx;
        return dentalFacts[idx];
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

    private void buildQuizQuestions() {
        quizQuestions.clear();

        // Topic 1: Tooth decay
        quizQuestions.add(new QuizQuestion(
                "What is the primary cause of tooth decay?",
                new String[]{"Acid from bacteria eating sugar", "Drinking too much water", "Eating crisp vegetables", "Brushing too hard"},
                0,
                "Tooth decay is caused by acid produced when mouth bacteria feed on dietary sugars."
        ));

        // Topic 2: Plaque
        quizQuestions.add(new QuizQuestion(
                "What is dental plaque?",
                new String[]{"Stained tooth enamel", "A sticky film of bacteria", "Hardened calcium deposit", "Food debris"},
                1,
                "Plaque is a sticky, colorless film of bacteria that constantly forms on your teeth."
        ));

        // Topic 3: Gum disease
        quizQuestions.add(new QuizQuestion(
                "What is the first stage of gum disease?",
                new String[]{"Periodontitis", "Gingivitis", "Enamel loss", "Tooth decay"},
                1,
                "Gingivitis is the early, reversible stage of gum disease, marked by red, swollen gums that bleed easily."
        ));

        // Topic 4: Flossing
        quizQuestions.add(new QuizQuestion(
                "Why is flossing daily necessary?",
                new String[]{"It whitens teeth", "It cleans areas a toothbrush cannot reach", "It strengthens jaw bone", "It replaces brushing"},
                1,
                "Flossing removes plaque and food particles from tight spaces between teeth that toothbrush bristles miss."
        ));

        // Topic 5: Mouthwash
        quizQuestions.add(new QuizQuestion(
                "How does therapeutic mouthwash help?",
                new String[]{"Replaces the need to floss", "Reduces plaque and kills bacteria", "Replaces the need to brush", "Whitens teeth instantly"},
                1,
                "Mouthwash helps reduce bacteria, prevent cavities, and reach areas that brushing and flossing might miss."
        ));

        // Topic 6: Sugar
        quizQuestions.add(new QuizQuestion(
                "Why does sugar lead to cavities?",
                new String[]{"Sugar dissolves enamel directly", "Bacteria turn sugar into harmful acid", "Sugar stains teeth yellow", "Sugar blocks saliva flow"},
                1,
                "Bacteria in the mouth feed on sugar and produce acid, which attacks and weakens tooth enamel."
        ));

        // Topic 7: Dental visits
        quizQuestions.add(new QuizQuestion(
                "How often should you see a dentist for checkups?",
                new String[]{"Every 6 months", "Only when in pain", "Every 2 years", "Once every 5 years"},
                0,
                "Regular visits every 6 months help detect and prevent issues before they become serious."
        ));

        // Topic 8: Brushing
        quizQuestions.add(new QuizQuestion(
                "How long should you brush your teeth?",
                new String[]{"30 seconds", "1 minute", "2 minutes", "5 minutes"},
                2,
                "Dentists recommend brushing for at least 2 minutes twice a day for effective cleaning."
        ));

        // Topic 9: Oral cancer
        quizQuestions.add(new QuizQuestion(
                "Which of these is a key risk factor for oral cancer?",
                new String[]{"Tobacco and heavy alcohol use", "Drinking cold water", "Using fluoride toothpaste", "Eating dairy products"},
                0,
                "Tobacco use of any kind and heavy alcohol consumption significantly increase the risk of oral cancer."
        ));

        // Topic 10: Sensitive teeth
        quizQuestions.add(new QuizQuestion(
                "What can cause sudden tooth sensitivity?",
                new String[]{"Exposed dentin from receded gums", "Stronger tooth enamel", "Using a soft toothbrush", "Drinking tap water"},
                0,
                "When gum tissue recedes, the underlying dentin layer is exposed, leading to temperature sensitivity."
        ));

        // Topic 11: Wisdom teeth
        quizQuestions.add(new QuizQuestion(
                "Why do wisdom teeth often need removal?",
                new String[]{"They have no enamel", "They often get impacted due to lack of space", "They cause bad breath", "They are too small to clean"},
                1,
                "Wisdom teeth can become trapped or impacted if there is not enough room in the jaw for them to erupt."
        ));

        // Topic 12: Fluoride
        quizQuestions.add(new QuizQuestion(
                "What role does fluoride play in toothpaste?",
                new String[]{"It replaces flossing", "It strengthens enamel and prevents cavities", "It makes teeth shiny", "It freshens breath"},
                1,
                "Fluoride remineralizes weakened tooth enamel, making it more resistant to future acid attacks."
        ));

        // Topic 13: Tooth sensitivity
        quizQuestions.add(new QuizQuestion(
                "Which toothpaste ingredient helps with tooth sensitivity?",
                new String[]{"Potassium nitrate", "Activated charcoal", "Baking soda", "Hydrogen peroxide"},
                0,
                "Potassium nitrate blocks pathways from the tooth surface to the nerve, reducing sensitivity."
        ));

        // Topic 14: Cavities
        quizQuestions.add(new QuizQuestion(
                "What is a dental cavity?",
                new String[]{"A stained spot", "A permanent hole in a tooth", "A swollen gum area", "A loose tooth root"},
                1,
                "A cavity is a permanently damaged area in the hard surface of your tooth that develops into a tiny hole."
        ));

        // Topic 15: Oral hygiene
        quizQuestions.add(new QuizQuestion(
                "What is the best foundation for great oral hygiene?",
                new String[]{"Brushing, flossing, and regular checkups", "Using whitening strips daily", "Avoiding all solid foods", "Drinking mouthwash only"},
                0,
                "Combining twice-daily brushing, daily flossing, and twice-yearly checkups is the gold standard for oral health."
        ));

        Collections.shuffle(quizQuestions, new Random());
    }

    private void pickRandomQuestion() {
        if (quizQuestions.isEmpty()) return;
        Random random = new Random();
        int index;
        if (quizQuestions.size() > 1) {
            do {
                index = random.nextInt(quizQuestions.size());
            } while (index == lastQuestionIndex);
        } else {
            index = 0;
        }
        lastQuestionIndex = index;
        currentQuestion = quizQuestions.get(index);
    }

    private void setupQuiz() {
        btnOpt0 = findViewById(R.id.btn_opt0);
        btnOpt1 = findViewById(R.id.btn_opt1);
        btnOpt2 = findViewById(R.id.btn_opt2);
        btnOpt3 = findViewById(R.id.btn_opt3);

        tvOpt0 = findViewById(R.id.tv_opt0);
        tvOpt1 = findViewById(R.id.tv_opt1);
        tvOpt2 = findViewById(R.id.tv_opt2);
        tvOpt3 = findViewById(R.id.tv_opt3);

        icCheck0 = findViewById(R.id.ic_check0);
        icCheck1 = findViewById(R.id.ic_check1);
        icCheck2 = findViewById(R.id.ic_check2);
        icCheck3 = findViewById(R.id.ic_check3);

        tvQuizQuestion = findViewById(R.id.tv_quiz_question);
        tvQuizExplanation = findViewById(R.id.tv_quiz_explanation);
        tvNextQuestion = findViewById(R.id.tv_next_question);

        quizFeedbackContainer = findViewById(R.id.quiz_feedback_container);
        icQuizFeedback = findViewById(R.id.ic_quiz_feedback);
        tvQuizFeedback = findViewById(R.id.tv_quiz_feedback);

        scrollView = findViewById(R.id.scroll_education);

        btnOpt0.setOnClickListener(v -> onQuizOptionTapped(0));
        btnOpt1.setOnClickListener(v -> onQuizOptionTapped(1));
        btnOpt2.setOnClickListener(v -> onQuizOptionTapped(2));
        btnOpt3.setOnClickListener(v -> onQuizOptionTapped(3));

        tvNextQuestion.setOnClickListener(v -> {
            pickRandomQuestion();
            resetQuiz();
        });

        // buildQuizQuestions() is called once in onCreate before setupQuiz()
        pickRandomQuestion();
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
        View[] btns = {btnOpt0, btnOpt1, btnOpt2, btnOpt3};
        TextView[] tvs = {tvOpt0, tvOpt1, tvOpt2, tvOpt3};
        ImageView[] checks = {icCheck0, icCheck1, icCheck2, icCheck3};

        if (currentQuestion != null) {
            tvQuizQuestion.setText(currentQuestion.question);
            for (int i = 0; i < 4; i++) {
                tvs[i].setText(currentQuestion.options[i]);
            }
        }

        int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(btnOpt0, com.google.android.material.R.attr.colorOnSurface);
        int colorSurface = com.google.android.material.color.MaterialColors.getColor(btnOpt0, com.google.android.material.R.attr.colorSurface);
        int colorOutline = com.google.android.material.color.MaterialColors.getColor(btnOpt0, com.google.android.material.R.attr.colorOutline);
        int colorOnSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(btnOpt0, com.google.android.material.R.attr.colorOnSurfaceVariant);

        for (int i = 0; i < 4; i++) {
            boolean isSelected = selectedQuizAnswer == i;
            boolean isCorrect = currentQuestion != null && i == currentQuestion.correctIndex;

            int bgColor = colorSurface;
            int borderColor = colorOutline;
            int textColor = colorOnSurface;

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
                    textColor = colorOnSurfaceVariant;
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
            boolean correct = currentQuestion != null && selectedQuizAnswer == currentQuestion.correctIndex;

            quizFeedbackContainer.setVisibility(View.VISIBLE);
            tvNextQuestion.setVisibility(View.VISIBLE);
            tvQuizExplanation.setVisibility(View.VISIBLE);

            android.graphics.drawable.GradientDrawable fb =
                    new android.graphics.drawable.GradientDrawable();

            fb.setCornerRadius(dpToPx(10));
            fb.setColor(correct ? 0x1A10B981 : 0x1AF59E0B);
            quizFeedbackContainer.setBackground(fb);

            icQuizFeedback.setImageResource(correct
                    ? R.drawable.ic_celebration
                    : R.drawable.ic_info_outline);

            icQuizFeedback.setColorFilter(correct ? COLOR_SUCCESS : COLOR_WARNING);
            tvQuizFeedback.setTextColor(correct ? COLOR_SUCCESS : COLOR_WARNING);

            tvQuizFeedback.setText(correct
                    ? "✅ Correct!\n🦷 Excellent!"
                    : "💡 Good Try!\n🪥 Keep learning!");

            if (currentQuestion != null) {
                tvQuizExplanation.setText(currentQuestion.explanation);
            }

            // No auto-scroll — user stays at current position
        } else {
            quizFeedbackContainer.setVisibility(View.GONE);
            tvNextQuestion.setVisibility(View.GONE);
            tvQuizExplanation.setVisibility(View.GONE);
        }
    }

    private void setupArticleList() {
        RecyclerView rv = findViewById(R.id.rv_articles);
        // Custom LLM that disables its own scroll so NestedScrollView renders ALL items
        LinearLayoutManager llm = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(false);
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