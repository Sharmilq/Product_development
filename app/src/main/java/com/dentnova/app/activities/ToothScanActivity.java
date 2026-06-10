package com.dentnova.app.activities;

import android.Manifest;import android.content.Intent;import android.content.SharedPreferences;import android.content.pm.PackageManager;import android.graphics.Bitmap;import android.net.Uri;import android.os.Bundle;import android.os.Handler;import android.view.View;import android.widget.ImageView;import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;import androidx.activity.result.contract.ActivityResultContracts;import androidx.appcompat.app.AppCompatActivity;import androidx.core.app.ActivityCompat;import androidx.core.content.ContextCompat;

import com.dentnova.app.R;import com.google.android.material.button.MaterialButton;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ToothScanActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;

    private Bitmap capturedBitmap;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    bitmap -> {

                        if (bitmap != null) {

                            capturedBitmap = bitmap;
                            showPreviewScreen();
                        }
                    });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;
                            showPreviewScreen();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tooth_scan);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        viewFlipper = findViewById(R.id.viewFlipper);

        SharedPreferences prefs =
                getSharedPreferences("dentnova_prefs", MODE_PRIVATE);

        int userId = new com.dentnova.app.utils.SessionManager(this).getUserId();

        boolean assessmentDone =
                prefs.getBoolean("assessment_done_" + userId, false);

        if (!assessmentDone) {
            showAssessmentRequired();
        } else {
            showScanScreen();
        }
    }

    private void showAssessmentRequired() {

        View lockedView = getLayoutInflater().inflate(
                R.layout.layout_assessment_required,
                viewFlipper,
                false);

        viewFlipper.removeAllViews();
        viewFlipper.addView(lockedView);

        MaterialButton btnTakeAssessment =
                lockedView.findViewById(R.id.btnTakeAssessment);

        btnTakeAssessment.setOnClickListener(v -> {

            startActivity(
                    new Intent(this, AssessmentActivity.class));

            finish();
        });
    }

    private void showScanScreen() {

        View scanView = getLayoutInflater().inflate(
                R.layout.layout_tooth_scan_unlocked,
                viewFlipper,
                false);

        viewFlipper.removeAllViews();
        viewFlipper.addView(scanView);

        MaterialButton btnCamera =
                scanView.findViewById(R.id.btnCamera);

        MaterialButton btnGallery =
                scanView.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> openCamera());

        btnGallery.setOnClickListener(v ->
                galleryLauncher.launch("image/*"));
        loadScanHistory(scanView);
    }

    private void loadScanHistory(View scanView) {

        android.widget.LinearLayout llHistory =
                scanView.findViewById(R.id.llScanHistory);

        android.widget.TextView tvNoScans =
                scanView.findViewById(R.id.tvNoScans);

        new Thread(() -> {

            try {

                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService.getToothScans(this);

                if (result.has("success") &&
                        result.get("success").getAsBoolean()) {

                    com.google.gson.JsonArray scans =
                            result.getAsJsonArray("scans");

                    runOnUiThread(() -> {

                        llHistory.removeAllViews();

                        if (scans.size() == 0) {
                            tvNoScans.setVisibility(View.VISIBLE);
                            return;
                        }

                        tvNoScans.setVisibility(View.GONE);

                        for (int i = 0; i < scans.size(); i++) {

                            com.google.gson.JsonObject scan =
                                    scans.get(i).getAsJsonObject();

                            android.widget.TextView item =
                                    new android.widget.TextView(this);

                            String label =
                                    scan.get("result_label").getAsString();

                            String date =
                                    scan.get("created_at").getAsString();

                            item.setText(
                                    "• " + label + "\n" + date
                            );

                            item.setTextSize(14f);
                            item.setPadding(0, 0, 0, 32);

                            llHistory.addView(item);
                        }
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("ToothScanActivity", "Error loading tooth scans history", e);
            }

        }).start();
    }

    private void showPreviewScreen() {

        View previewView = getLayoutInflater().inflate(
                R.layout.layout_tooth_scan_preview,
                viewFlipper,
                false);

        viewFlipper.removeAllViews();
        viewFlipper.addView(previewView);

        ImageView imgPreview =
                previewView.findViewById(R.id.imgPreview);

        if (capturedBitmap != null) {
            imgPreview.setImageBitmap(capturedBitmap);
        }

        if (selectedImageUri != null) {
            imgPreview.setImageURI(selectedImageUri);
        }

        previewView.findViewById(R.id.btnRemoveImage)
                .setOnClickListener(v -> showScanScreen());

        MaterialButton btnAnalyze =
                previewView.findViewById(R.id.btnAnalyzeAI);

        btnAnalyze.setOnClickListener(v ->
                showAnalyzingThenResult());
    }

    private void showAnalyzingThenResult() {

        View analyzingView = getLayoutInflater().inflate(
                R.layout.layout_tooth_scan_analyzing,
                viewFlipper,
                false);

        viewFlipper.removeAllViews();
        viewFlipper.addView(analyzingView);

        new Thread(() -> {

            try {


                java.io.File imageFile;

                if (selectedImageUri != null) {

                    java.io.InputStream inputStream =
                            getContentResolver().openInputStream(selectedImageUri);

                    imageFile = new java.io.File(
                            getCacheDir(),
                            "scan_image.jpg"
                    );

                    java.io.FileOutputStream output =
                            new java.io.FileOutputStream(imageFile);

                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = inputStream.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }

                    output.close();
                    inputStream.close();

                } else {

                    imageFile = new java.io.File(
                            getCacheDir(),
                            "captured_scan.jpg"
                    );

                    java.io.FileOutputStream output =
                            new java.io.FileOutputStream(imageFile);

                    capturedBitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            90,
                            output
                    );

                    output.close();
                }
                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService
                                .predictToothScan(
                                        ToothScanActivity.this,
                                        imageFile
                                );

                double finalInflammationScore =
                        result.get("inflammation_score").getAsDouble();

                double finalCleanlinessScore =
                        result.get("cleanliness_score").getAsDouble();

                double finalOverallScore =
                        result.get("overall_score").getAsDouble();

                String finalResultLabel =
                        result.get("result_label").getAsString();


                com.google.gson.JsonObject response =
                        com.dentnova.app.services.ApiService.saveToothScan(
                                ToothScanActivity.this,
                                imageFile,
                                finalOverallScore,
                                finalInflammationScore,
                                finalCleanlinessScore,
                                finalResultLabel
                        );
                android.util.Log.d("SCAN_RESPONSE", response.toString());

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    throw new Exception(response.has("message")
                            ? response.get("message").getAsString()
                            : "Scan save failed");
                }
                final String displayLabel = finalResultLabel;
                final double displayInflammation = finalInflammationScore;
                final double displayCleanliness = finalCleanlinessScore;
                final double displayOverall = finalOverallScore;

                runOnUiThread(() -> {

                    View resultView = getLayoutInflater().inflate(
                            R.layout.layout_tooth_scan_result,
                            viewFlipper,
                            false);

                    viewFlipper.removeAllViews();
                    viewFlipper.addView(resultView);

                    android.widget.TextView tvResult =
                            resultView.findViewById(R.id.tvScanResultLabel);

                    android.widget.TextView tvDesc =
                            resultView.findViewById(R.id.tvScanResultDesc);

                    android.widget.TextView tvPlaque =
                            resultView.findViewById(R.id.tvPlaqueScore);

                    android.widget.TextView tvGum =
                            resultView.findViewById(R.id.tvGumScore);

                    android.widget.TextView tvClean =
                            resultView.findViewById(R.id.tvCleanlinessScore);

                    tvResult.setText(displayLabel);

                    if (displayLabel.toLowerCase().contains("high")) {
                        tvDesc.setText("🔴 Signs of gum inflammation or poor cleanliness detected. Please improve care and consider a dental checkup.");
                    } else if (displayLabel.toLowerCase().contains("moderate")) {
                        tvDesc.setText("🟠 Some cleanliness or gum-health concerns noticed. Better brushing, rinsing, and flossing can improve this.");
                    } else {
                        tvDesc.setText("🟢 Your gums and cleanliness look good. Keep maintaining your routine!");
                    }

                    tvPlaque.setText(
                            "Gingival inflammation: " + displayInflammation + "%"
                    );

                    tvGum.setText(
                            "Gum cleanliness: " + displayCleanliness + "%"
                    );

                    tvClean.setText(
                            "Overall gum health: " + displayOverall + "%"
                    );

                    MaterialButton btnScanAgain =
                            resultView.findViewById(R.id.btnScanAgain);
                    MaterialButton btnShareReport =
                            resultView.findViewById(R.id.btnShareReport);

                    btnShareReport.setOnClickListener(v ->
                            sharePdfReport(
                                    displayLabel,
                                    displayInflammation,
                                    displayCleanliness,
                                    displayOverall,
                                    imageFile
                            )
                    );

                    btnScanAgain.setOnClickListener(v -> {

                        capturedBitmap = null;
                        selectedImageUri = null;

                        showScanScreen();
                    });
                });

            } catch (Exception e) {
                android.util.Log.e("ToothScanActivity", "Error predicting or saving tooth scan", e);
                runOnUiThread(() ->
                        android.widget.Toast.makeText(
                                ToothScanActivity.this,
                                "Failed to analyze scan",
                                android.widget.Toast.LENGTH_LONG
                        ).show());
            }

        }).start();
    }

    private File createPdfReport(
            String label,
            double inflammation,
            double cleanliness,
            double overall,
            java.io.File imageFile
    ) throws Exception {

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

        PdfDocument pdf = new PdfDocument();

        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headingPaint = new Paint();
        Paint boxPaint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setColor(0xFF00BCD4);
        titlePaint.setTextSize(25);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        headingPaint.setColor(0xFF1A2332);
        headingPaint.setTextSize(17);
        headingPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        paint.setColor(0xFF1A2332);
        paint.setTextSize(13);

        boxPaint.setColor(0xFFEAF9FC);

        canvas.drawText("DentNova AI Oral Health Report", 40, 55, titlePaint);

        String date = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        ).format(new Date());

        paint.setColor(0xFF6B7B8D);
        paint.setTextSize(11);
        canvas.drawText("Generated on: " + date, 40, 78, paint);

        paint.setColor(0xFF1A2332);
        paint.setTextSize(13);

        canvas.drawText("Patient Name: " + patientName, 40, 115, paint);
        canvas.drawText("Email: " + patientEmail, 40, 140, paint);
        canvas.drawText("Report Type: AI Tooth Scan Analysis", 40, 165, paint);

        if (imageFile != null && imageFile.exists()) {
            android.graphics.Bitmap bitmap =
                    android.graphics.BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            if (bitmap != null) {
                android.graphics.Bitmap scaled =
                        android.graphics.Bitmap.createScaledBitmap(bitmap, 180, 130, true);

                canvas.drawBitmap(scaled, 375, 105, paint);
            }
        }

        canvas.drawRect(40, 220, 555, 305, boxPaint);

        canvas.drawText("AI Scan Result", 60, 255, headingPaint);

        paint.setColor(0xFF1A2332);
        paint.setTextSize(14);
        canvas.drawText(label, 60, 282, paint);

        canvas.drawText("Analysis Scores", 40, 355, headingPaint);

        paint.setTextSize(13);
        canvas.drawText("Gingival Inflammation: " + inflammation + "%", 60, 390, paint);
        canvas.drawText("Gum Cleanliness: " + cleanliness + "%", 60, 420, paint);
        canvas.drawText("Overall Gum Health: " + overall + "%", 60, 450, paint);

        canvas.drawText("Recommendations", 40, 520, headingPaint);

        paint.setTextSize(13);
        canvas.drawText("• Brush twice daily for 2 minutes.", 60, 555, paint);
        canvas.drawText("• Floss once daily to reduce plaque buildup.", 60, 585, paint);
        canvas.drawText("• Rinse after sugary foods or drinks.", 60, 615, paint);
        canvas.drawText("• Visit a dentist if pain, bleeding, or swelling continues.", 60, 645, paint);

        paint.setColor(0xFF6B7B8D);
        paint.setTextSize(10);
        canvas.drawText(
                "Disclaimer: This AI-assisted report is for awareness only and is not a medical diagnosis.",
                40,
                775,
                paint
        );

        canvas.drawText("Generated by DentNova", 40, 800, paint);

        pdf.finishPage(page);

        File file = new File(getCacheDir(), "DentNova_Oral_Health_Report.pdf");

        FileOutputStream out = new FileOutputStream(file);
        pdf.writeTo(out);
        pdf.close();
        out.close();

        return file;
    }

    private void sharePdfReport(
            String label,
            double inflammation,
            double cleanliness,
            double overall,
            java.io.File imageFile
    ) {
        try {
            File pdfFile = createPdfReport(
                    label,
                    inflammation,
                    cleanliness,
                    overall,
                    imageFile
            );

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Share DentNova Report"));

        } catch (Exception e) {
            android.util.Log.e("ToothScanActivity", "Error creating/sharing PDF report", e);
            android.widget.Toast.makeText(
                    this,
                    "Failed to create PDF report",
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }
    private void openCamera() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            cameraLauncher.launch(null);

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    200);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == 200 &&
                grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            cameraLauncher.launch(null);
        }
    }
}