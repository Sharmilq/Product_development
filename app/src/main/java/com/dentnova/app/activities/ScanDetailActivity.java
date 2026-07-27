package com.dentnova.app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.dentnova.app.R;

public class ScanDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_detail);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Extract extras from Intent
        String diagnosisLabel = getIntent().getStringExtra("diagnosis_label");
        String dateTimeFormatted = getIntent().getStringExtra("created_at_formatted");
        double overallScore    = getIntent().getDoubleExtra("overall_score", 0.0);
        double inflammationScore = getIntent().getDoubleExtra("inflammation_score", 0.0);
        double cleanlinessScore  = getIntent().getDoubleExtra("cleanliness_score", 0.0);
        String imageBase64     = getIntent().getStringExtra("image_base64");

        if (diagnosisLabel == null) diagnosisLabel = "AI Tooth Scan";
        if (dateTimeFormatted == null) dateTimeFormatted = "";
        if (imageBase64 == null) imageBase64 = "";

        // -- Large scan image -- (strip data URL prefix if present)
        ImageView imgLarge = findViewById(R.id.imgLargeScan);
        String pureBase64 = stripDataUrlPrefix(imageBase64);
        if (!pureBase64.isEmpty()) {
            try {
                byte[] decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedBytes)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_tooth_outline)
                        .error(R.drawable.ic_tooth_outline)
                        .into(imgLarge);
            } catch (Exception e) {
                imgLarge.setImageResource(R.drawable.ic_tooth_outline);
            }
        } else {
            imgLarge.setImageResource(R.drawable.ic_tooth_outline);
        }

        // -- Diagnosis title & description --
        TextView tvTitle = findViewById(R.id.tvDiagnosisTitle);
        TextView tvDesc  = findViewById(R.id.tvDiagnosisDesc);
        tvTitle.setText(diagnosisLabel);

        // Derive full description and colour from the result label
        if (diagnosisLabel.toLowerCase().contains("healthy")) {
            tvTitle.setTextColor(Color.parseColor("#10B981")); // emerald green
            tvDesc.setText("Your gums and oral cleanliness look great. Keep maintaining your daily oral hygiene routine for continued health.");
        } else if (diagnosisLabel.toLowerCase().contains("gingival") || diagnosisLabel.toLowerCase().contains("inflammation")) {
            tvTitle.setTextColor(Color.parseColor("#F59E0B")); // amber orange
            tvDesc.setText("Signs of gum inflammation (gingivitis) were detected. Improve your oral hygiene and consider visiting a dentist for a professional cleaning.");
        } else if (diagnosisLabel.toLowerCase().contains("calculus") || diagnosisLabel.toLowerCase().contains("cleanliness")) {
            tvTitle.setTextColor(Color.parseColor("#EF4444")); // red
            tvDesc.setText("Significant plaque build-up or calculus (tartar) was detected. Hardened calculus cannot be removed by brushing and requires professional dental scaling.");
        } else {
            tvTitle.setTextColor(Color.parseColor("#64748B")); // slate grey
            tvDesc.setText("AI analysis results are displayed below. Please consult a dentist for a professional evaluation.");
        }

        // -- Date & Time --
        TextView tvDateTime = findViewById(R.id.tvDateTimeDetail);
        tvDateTime.setText("Date: " + dateTimeFormatted);

        // -- Scores --
        int overall      = (int) Math.round(overallScore);
        int inflammation = (int) Math.round(inflammationScore);
        int cleanliness  = (int) Math.round(cleanlinessScore);

        // Overall Gum Health
        TextView tvOverall  = findViewById(R.id.tvOverallScoreDetail);
        ProgressBar pbOverall = findViewById(R.id.pbOverallScore);
        tvOverall.setText(overall + "%");
        pbOverall.setProgress(Math.min(100, Math.max(0, overall)));

        // Gum Cleanliness
        TextView tvCleanliness   = findViewById(R.id.tvCleanlinessScoreDetail);
        ProgressBar pbCleanliness = findViewById(R.id.pbCleanlinessScore);
        tvCleanliness.setText(cleanliness + "%");
        pbCleanliness.setProgress(Math.min(100, Math.max(0, cleanliness)));

        // Gingival Inflammation
        TextView tvInflammation   = findViewById(R.id.tvInflammationScoreDetail);
        ProgressBar pbInflammation = findViewById(R.id.pbInflammationScore);
        tvInflammation.setText(inflammation + "%");
        pbInflammation.setProgress(Math.min(100, Math.max(0, inflammation)));

        // -- Recommendations based on diagnosis --
        TextView tvRecs = findViewById(R.id.tvDetailRecommendations);
        if (diagnosisLabel.toLowerCase().contains("healthy")) {
            tvRecs.setText(
                "• Brush twice daily for at least 2 minutes\n\n" +
                "• Floss once daily before bedtime\n\n" +
                "• Rinse with fluoride mouthwash after meals\n\n" +
                "• Schedule a dental checkup every 6 months"
            );
        } else if (diagnosisLabel.toLowerCase().contains("gingival") || diagnosisLabel.toLowerCase().contains("inflammation")) {
            tvRecs.setText(
                "• Brush gently around the gumline using a soft-bristled brush\n\n" +
                "• Floss daily to remove plaque between teeth\n\n" +
                "• Use an antiseptic mouthwash to reduce gum inflammation\n\n" +
                "• Schedule a professional dental cleaning appointment"
            );
        } else {
            tvRecs.setText(
                "• Visit a dentist for professional scaling and cleaning\n\n" +
                "• Use tartar-control toothpaste to prevent new build-up\n\n" +
                "• Brush and floss meticulously at least twice a day\n\n" +
                "• Use an antibacterial rinse to protect your gums"
            );
        }
    }

    /** Strips the "data:image/...;base64," prefix if present, returning pure base64. */
    private String stripDataUrlPrefix(String base64) {
        if (base64 == null || base64.isEmpty()) return "";
        int comma = base64.indexOf(",");
        if (base64.startsWith("data:") && comma >= 0) {
            return base64.substring(comma + 1);
        }
        return base64;
    }
}

