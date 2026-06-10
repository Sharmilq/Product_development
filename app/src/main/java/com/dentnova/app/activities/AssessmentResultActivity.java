package com.dentnova.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Canvas;
import android.net.Uri;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dentnova.app.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class AssessmentResultActivity extends AppCompatActivity {

    private int score;
    private String label;

    // Views
    private CircularProgressIndicator scoreProgress;
    private TextView tvScoreNumber;
    private TextView tvRiskLabel;
    private TextView tvWhyScore;
    private LinearLayout llRiskSummaryContent;
    private LinearLayout llImprovementPlan;
    private LinearLayout llKeyIssues;
    private LinearLayout llRecommendations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment_result);

        // Get score from Intent
        score = getIntent().getIntExtra("score", 0);

        // Compute risk label
        label = getIntent().getStringExtra("label");

        if (label == null) {

            if (score >= 70) {
                label = "Low";
            }
            else if (score >= 40) {
                label = "Moderate";
            }
            else {
                label = "High";
            }
        }

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Result");
        }

        // Bind views
        scoreProgress = findViewById(R.id.scoreProgress);
        tvScoreNumber = findViewById(R.id.tvScoreNumber);
        tvRiskLabel = findViewById(R.id.tvRiskLabel);
        tvWhyScore = findViewById(R.id.tvWhyScore);
        llRiskSummaryContent = findViewById(R.id.llRiskSummaryContent);
        llImprovementPlan = findViewById(R.id.llImprovementPlan);
        llKeyIssues = findViewById(R.id.llKeyIssues);
        llRecommendations = findViewById(R.id.llRecommendations);

        bindScoreCard();
        buildPersonalizedResult();
    }
    private void buildPersonalizedResult() {
        llImprovementPlan.removeAllViews();
        llKeyIssues.removeAllViews();
        llRecommendations.removeAllViews();
        llRiskSummaryContent.removeAllViews();

        String brushing = getIntent().getStringExtra("answer_0");
        String duration = getIntent().getStringExtra("answer_1");
        String flossing = getIntent().getStringExtra("answer_2");
        String bleeding = getIntent().getStringExtra("answer_3");
        String sugar = getIntent().getStringExtra("answer_4");
        String rinse = getIntent().getStringExtra("answer_5");
        String sensitivity = getIntent().getStringExtra("answer_6");
        String grinding = getIntent().getStringExtra("answer_7");
        String tobacco = getIntent().getStringExtra("answer_8");
        String checkup = getIntent().getStringExtra("answer_9");
        String water = getIntent().getStringExtra("answer_11");
        String dryMouth = getIntent().getStringExtra("answer_12");
        addSummaryBox(
                llRiskSummaryContent,
                label,
                score < 40
                        ? "High attention needed"
                        : score < 70
                        ? "Some habits need improvement"
                        : "Good oral health habits detected",

                score < 40
                        ? 0xFFFFEBEE
                        : score < 70
                        ? 0xFFFFF3E0
                        : 0xFFE8F5E9,

                score < 40
                        ? 0xFFEF4444
                        : score < 70
                        ? 0xFFF57C00
                        : 0xFF10B981
        );

        if (!"Twice a day".equals(brushing)) {
            addText(llImprovementPlan, "• Brush twice daily for better plaque control.");
            addText(llRecommendations, "✓ Set a morning and night brushing reminder.");
        }

        if (!"2 minutes or more".equals(duration)) {
            addText(llImprovementPlan, "• Brush for at least 2 minutes each time.");
        }

        if (!"Daily".equals(flossing)) {
            addText(llImprovementPlan, "• Floss once daily to clean between teeth.");
            addText(llRecommendations, "✓ Start flossing at night before brushing.");
        }

        if (!"Never".equals(bleeding)) {
            addText(llKeyIssues, "• Gum bleeding may indicate irritation or gingivitis.");
            addText(llRecommendations, "✓ Use a soft-bristle brush and consider a dental checkup.");
        }

        if ("Daily".equals(sugar) || "Multiple times a day".equals(sugar)) {
            addText(llKeyIssues, "• Frequent sugar intake increases cavity risk.");
            addText(llRecommendations, "✓ Reduce sugary snacks and rinse with water after sweets.");
        }

        if (!"Always".equals(rinse)) {
            addText(llRecommendations, "✓ Rinse your mouth with water after sugary foods.");
        }

        if (!"Never".equals(sensitivity)) {
            addText(llKeyIssues, "• Tooth sensitivity may suggest enamel wear or gum issues.");
            addText(llRecommendations, "✓ Use sensitivity toothpaste and avoid very acidic drinks.");
        }

        if ("Often".equals(grinding) || "I'm not sure".equals(grinding)) {
            addText(llKeyIssues, "• Teeth grinding can wear down enamel.");
            addText(llRecommendations, "✓ Ask your dentist about grinding protection if it continues.");
        }

        if (!"Never".equals(tobacco)) {
            addText(llKeyIssues, "• Tobacco can stain teeth and worsen gum health.");
            addText(llRecommendations, "✓ Reducing tobacco use can improve gum health.");
        }

        if ("1–2 years ago".equals(checkup) || "Over 2 years ago".equals(checkup)) {
            addText(llRecommendations, "✓ Schedule a dental checkup soon.");
        }

        if ("Less than 1L".equals(water) || "Rarely drink water".equals(water)) {
            addText(llImprovementPlan, "• Drink more water to reduce dry mouth and cavity risk.");
        }

        if ("Often".equals(dryMouth) || "Always".equals(dryMouth)) {
            addText(llKeyIssues, "• Dry mouth can increase cavity risk.");
            addText(llRecommendations, "✓ Stay hydrated and discuss persistent dry mouth with a dentist.");
        }

        if (llImprovementPlan.getChildCount() == 0) {
            addText(llImprovementPlan, "✓ Your habits look good. Keep maintaining your routine.");
        }

        if (llKeyIssues.getChildCount() == 0) {
            addText(llKeyIssues, "✓ No major warning signs from your answers.");
        }

        if (llRecommendations.getChildCount() == 0) {
            addText(llRecommendations, "✓ Continue regular brushing, flossing, hydration, and checkups.");
        }
    }

    private void addText(LinearLayout parent, String text) {

        TextView tv = new TextView(this);

        tv.setText(text);
        tv.setTextSize(13f);
        tv.setPadding(24, 20, 24, 20);

        int bgColor;
        int borderColor;
        int textColor;

        String lower = text.toLowerCase();

        // RED
        if (
                lower.contains("bleeding")
                        || lower.contains("tobacco")
                        || lower.contains("high risk")
                        || lower.contains("dentist")
        ) {

            bgColor = 0xFFFFEBEE;
            borderColor = 0xFFEF4444;
            textColor = 0xFFB71C1C;

        }

        // ORANGE
        else if (
                lower.contains("sugar")
                        || lower.contains("floss")
                        || lower.contains("sensitivity")
                        || lower.contains("grinding")
        ) {

            bgColor = 0xFFFFF3E0;
            borderColor = 0xFFF57C00;
            textColor = 0xFFE65100;

        }

        // GREEN
        else if (
                lower.contains("good")
                        || lower.contains("great")
                        || lower.contains("healthy")
                        || lower.contains("maintaining")
        ) {

            bgColor = 0xFFE8F5E9;
            borderColor = 0xFF10B981;
            textColor = 0xFF1B5E20;

        }

        // BLUE
        else {

            bgColor = 0xFFE3F2FD;
            borderColor = 0xFF2196F3;
            textColor = 0xFF0D47A1;
        }

        tv.setTextColor(textColor);

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();

        bg.setColor(bgColor);
        bg.setCornerRadius(24);
        bg.setStroke(3, borderColor);

        tv.setBackground(bg);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 14);

        tv.setLayoutParams(params);

        parent.addView(tv);
    }
    private void addSummaryBox(
            LinearLayout parent,
            String title,
            String message,
            int bgColor,
            int textColor
    ) {

        TextView box = new TextView(this);

        box.setText(
                title.toUpperCase()
                        + "\n\n"
                        + message
        );

        box.setTextColor(textColor);
        box.setTextSize(14f);

        box.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        box.setPadding(24,20,24,20);

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();

        bg.setColor(bgColor);
        bg.setCornerRadius(26);

        box.setBackground(bg);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0,0,0,18);

        box.setLayoutParams(params);

        parent.addView(box);
    }
    private void bindScoreCard() {
        // Score number
        tvScoreNumber.setText(String.valueOf(score));

        // Risk label
        tvRiskLabel.setText(label.toUpperCase());

        // Subtitle
        tvWhyScore.setText("Some areas need attention. Let\u2019s improve together.");

        // Circular progress color based on score
        int color;
        if (score < 30) {
            color = getResources().getColor(R.color.risk_red, getTheme());
        } else if (score < 60) {
            color = getResources().getColor(R.color.warning_orange, getTheme());
        } else {
            color = getResources().getColor(R.color.success_green, getTheme());
        }

        scoreProgress.setProgress(score);
        scoreProgress.setIndicatorColor(color);
        tvRiskLabel.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color));
    }
    private File createAssessmentPdf() throws Exception {

        PdfDocument pdf = new PdfDocument();

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headingPaint = new Paint();
        Paint boxPaint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        com.dentnova.app.utils.SessionManager session =
                new com.dentnova.app.utils.SessionManager(this);

        String patientName = session.getUserName();
        String patientEmail = session.getUserEmail();

        if (patientName == null || patientName.trim().isEmpty()) {
            patientName = "DentNova User";
        }

        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            patientEmail = "Not available";
        }

        String date = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        ).format(new Date());

        titlePaint.setColor(0xFF00BCD4);
        titlePaint.setTextSize(25);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        headingPaint.setColor(0xFF1A2332);
        headingPaint.setTextSize(17);
        headingPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        paint.setColor(0xFF1A2332);
        paint.setTextSize(13);

        boxPaint.setColor(0xFFEAF9FC);

        canvas.drawText(
                "DentNova Oral Health Assessment",
                40,
                55,
                titlePaint
        );

        paint.setColor(0xFF6B7B8D);
        paint.setTextSize(11);

        canvas.drawText(
                "Generated on: " + date,
                40,
                80,
                paint
        );

        paint.setColor(0xFF1A2332);
        paint.setTextSize(13);

        canvas.drawText(
                "Patient Name: " + patientName,
                40,
                120,
                paint
        );

        canvas.drawText(
                "Email: " + patientEmail,
                40,
                145,
                paint
        );

        canvas.drawText(
                "Assessment Type: Oral Health Risk Analysis",
                40,
                170,
                paint
        );

        canvas.drawRect(40, 220, 555, 320, boxPaint);

        canvas.drawText(
                "Assessment Result",
                60,
                255,
                headingPaint
        );

        paint.setTextSize(15);

        canvas.drawText(
                "Oral Health Score: " + score + "/100",
                60,
                285,
                paint
        );

        canvas.drawText(
                "Risk Level: " + label,
                60,
                312,
                paint
        );

        canvas.drawText(
                "Recommendations",
                40,
                390,
                headingPaint
        );

        paint.setTextSize(13);

        canvas.drawText(
                "• Brush twice daily using fluoride toothpaste",
                60,
                430,
                paint
        );

        canvas.drawText(
                "• Floss once daily to remove plaque",
                60,
                460,
                paint
        );

        canvas.drawText(
                "• Reduce sugary foods and drinks",
                60,
                490,
                paint
        );

        canvas.drawText(
                "• Schedule regular dental checkups",
                60,
                520,
                paint
        );

        canvas.drawText(
                "• Maintain hydration for better oral health",
                60,
                550,
                paint
        );

        paint.setColor(0xFF6B7B8D);
        paint.setTextSize(10);

        canvas.drawText(
                "Disclaimer: This AI-assisted report is for awareness only and not a medical diagnosis.",
                40,
                775,
                paint
        );

        canvas.drawText(
                "Generated by DentNova",
                40,
                800,
                paint
        );

        pdf.finishPage(page);

        File file =
                new File(
                        getCacheDir(),
                        "DentNova_Assessment_Report.pdf"
                );

        FileOutputStream out =
                new FileOutputStream(file);

        pdf.writeTo(out);

        pdf.close();
        out.close();

        return file;
    }

    private void shareAssessmentPdf() {

        try {

            File pdfFile = createAssessmentPdf();

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.setType("application/pdf");

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Share Assessment Report"
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Failed to generate PDF",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Report
        menu.add(Menu.NONE, R.id.menu_report, 0, "Report")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        // Share
        menu.add(Menu.NONE, R.id.menu_share, 1, "Share")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        // Retake
        menu.add(Menu.NONE, R.id.menu_retake, 2, "Retake")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;

        } else if (id == R.id.menu_report) {
            new AlertDialog.Builder(this)
                    .setTitle("Assessment Report")
                    .setMessage("Score: " + score + "/100\nRisk: " + label)
                    .setPositiveButton("Close", (d, w) -> d.dismiss())
                    .show();
            return true;

        } else if (id == R.id.menu_share) {
            shareAssessmentPdf();
            return true;

        } else if (id == R.id.menu_retake) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}