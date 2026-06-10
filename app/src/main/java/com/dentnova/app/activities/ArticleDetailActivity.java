package com.dentnova.app.activities;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.dentnova.app.R;

public class ArticleDetailActivity extends AppCompatActivity {

    String[][] data = {
            {"Gum care", "Healthy gums, healthy life",
                    "Healthy gums are firm, pink, and don't bleed. Gum disease is preventable with consistent care.",
                    "Watch for redness, swelling or bleeding.|Don't smoke — it's a major gum disease risk.|Use a soft toothbrush; hard bristles damage gums.|Visit your dentist if bleeding lasts over a week.",
                    "Massage gums gently while brushing to boost circulation.|Rinse with warm salt water if gums feel inflamed.|Use interdental brushes for thorough cleaning.|Stay hydrated — dry mouth increases gum disease risk."},

            {"Tooth sensitivity", "Causes and treatments",
                    "Tooth sensitivity occurs when enamel wears down or gums recede, exposing the dentin layer underneath.",
                    "Use desensitizing toothpaste for sensitive teeth.|Avoid acidic foods that weaken enamel.|Visit your dentist to identify the root cause.",
                    "Use fluoride toothpaste daily.|Avoid very cold drinks if sensitivity is high.|Use a soft toothbrush.|Book a dental checkup if sensitivity continues."},

            {"Whitening myths", "Facts vs fiction",
                    "Many whitening claims are exaggerated. Here's what actually works — and what to avoid.",
                    "Charcoal toothpaste is abrasive — not recommended.|Lemon and baking soda erode enamel; avoid DIY.|Professional whitening is safer and more effective.|Daily care prevents most stains in the first place.",
                    "Limit coffee, tea, and red wine.|Brush regularly with fluoride toothpaste.|Avoid DIY bleaching hacks.|Ask a dentist before whitening treatment."},

            {"Flossing", "Why, when and how to floss",
                    "Flossing removes plaque and food particles your toothbrush can't reach — between teeth and under the gumline.",
                    "Floss at least once a day, ideally before bed.|Use about 18 inches of floss; wind around middle fingers.|Slide gently — never snap floss into the gum.|Curve floss into a C-shape around each tooth.",
                    "Floss before brushing for better fluoride absorption.|Use waxed floss if your teeth are tightly spaced.|Try floss picks if traditional floss feels awkward.|Be consistent: daily flossing beats occasional deep cleans."},

            {"Brushing techniques", "Master the perfect brush",
                    "Proper technique matters more than pressure. Soft brush + 2 minutes + correct angle = clean, healthy teeth.",
                    "Hold brush at a 45° angle to the gumline.|Use small circular motions, not back-and-forth scrubbing.|Brush all surfaces: outer, inner, and chewing.|Don't forget your tongue — bacteria hide there.",
                    "Use a soft-bristled brush.|Replace your toothbrush every 3 months.|Don't press too hard.|Brush for a full 2 minutes."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        int index = getIntent().getIntExtra("article_index", 0);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ((TextView)findViewById(R.id.tvTitle)).setText(data[index][0]);
        ((TextView)findViewById(R.id.tvMainTitle)).setText(data[index][0]);
        ((TextView)findViewById(R.id.tvSubtitle)).setText(data[index][1]);
        ((TextView)findViewById(R.id.tvContent)).setText(data[index][2]);

        addBullets((LinearLayout)findViewById(R.id.llTakeaways), data[index][3].split("\\|"));
        addTips((LinearLayout)findViewById(R.id.llTips), data[index][4].split("\\|"));
        View cardVideo = findViewById(R.id.cardVideo);

        cardVideo.setOnClickListener(v -> {

            String youtubeUrl = "";

            switch (index) {

                case 0:
                    youtubeUrl = "https://youtu.be/X9ULGWn5t90?si=ESohzsBtfZoy4fkp";
                    break;

                case 1:
                    youtubeUrl = "https://youtu.be/-VeqwXMQ2N0?si=QJJgrwYKEu1UXBa5";
                    break;

                case 2:
                    youtubeUrl = "https://youtu.be/jRvLDKFYfys?si=gYl0on-ttEVyPnnq";
                    break;

                case 3:
                    youtubeUrl = "https://youtu.be/m3pBA4cgdxw?si=4BmQ65AdDTCEAp-T";
                    break;

                case 4:
                    youtubeUrl = "https://youtu.be/7kGXQDwT6IA?si=aF9lBpzibWNqwsvs";
                    break;
            }

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(youtubeUrl)
            );

            startActivity(intent);
        });
    }

    private void addBullets(LinearLayout parent, String[] items) {
        parent.removeAllViews();
        for (String item : items) {
            TextView tv = new TextView(this);
            tv.setText("•  " + item);
            tv.setTextSize(16);
            tv.setTextColor(0xFF1A2332);
            tv.setPadding(0, 8, 0, 8);
            parent.addView(tv);
        }
    }

    private void addTips(LinearLayout parent, String[] items) {
        parent.removeAllViews();
        for (int i = 0; i < items.length; i++) {
            TextView tv = new TextView(this);
            tv.setText((i + 1) + "   " + items[i]);
            tv.setTextSize(16);
            tv.setTextColor(0xFF1A2332);
            tv.setPadding(0, 10, 0, 10);
            parent.addView(tv);
        }
    }
}