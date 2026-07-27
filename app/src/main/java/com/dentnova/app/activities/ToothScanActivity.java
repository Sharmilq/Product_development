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

        androidx.recyclerview.widget.RecyclerView rvHistory =
                scanView.findViewById(R.id.llScanHistory);

        android.widget.TextView tvNoScans =
                scanView.findViewById(R.id.tvNoScans);

        rvHistory.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));

        new Thread(() -> {

            try {

                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService.getToothScans(this);

                if (result.has("success") &&
                        result.get("success").getAsBoolean()) {

                    com.google.gson.JsonArray scans =
                            result.getAsJsonArray("scans");

                    // Debug Log: Supabase and Android counts (before UI thread dispatch)
                    int supabaseRows = scans != null ? scans.size() : 0;
                    android.util.Log.d("SCAN_HISTORY_DEBUG", "Total rows returned from Supabase: " + supabaseRows);
                    android.util.Log.d("SCAN_HISTORY_DEBUG", "Total rows received by Android: " + supabaseRows);

                    runOnUiThread(() -> {

                        if (scans == null || scans.size() == 0) {
                            tvNoScans.setVisibility(View.VISIBLE);
                            rvHistory.setVisibility(View.GONE);
                            return;
                        }

                        tvNoScans.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);

                        com.dentnova.app.adapters.ToothScanAdapter adapter =
                                new com.dentnova.app.adapters.ToothScanAdapter(this, scans);

                        rvHistory.setAdapter(adapter);

                        // Call notifyDataSetChanged() after adapter is loaded/bound
                        adapter.notifyDataSetChanged();

                        // Debug Log: Adapter and RecyclerView counts (after UI thread dispatch)
                        int adapterRows = adapter.getItemCount();
                        int rvItemCount = rvHistory.getAdapter() != null ? rvHistory.getAdapter().getItemCount() : 0;
                        android.util.Log.d("SCAN_HISTORY_DEBUG", "Total rows inside the adapter: " + adapterRows);
                        android.util.Log.d("SCAN_HISTORY_DEBUG", "RecyclerView item count: " + rvItemCount);
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

                android.util.Log.d("TOOTH_SCAN_ANDROID_DEBUG", "JSON received from backend: " + result.toString());

                // Fail loudly if backend didn't return a class — never silently default to Healthy
                if (!result.has("class")) {
                    android.util.Log.e("TOOTH_SCAN_ANDROID_DEBUG",
                            "Backend response missing 'class' field! Full response: " + result.toString());
                    throw new java.io.IOException(
                            "Backend did not return a prediction class. Check server logs. Response: " + result.toString());
                }

                final String predictedClass = result.get("class").getAsString();
                final double confidence = result.has("confidence") ? result.get("confidence").getAsDouble() : 0.0;
                final double finalInflammationScore = result.has("inflammation_score") ? result.get("inflammation_score").getAsDouble() : 0.0;
                final double finalCleanlinessScore = result.has("cleanliness_score") ? result.get("cleanliness_score").getAsDouble() : 0.0;
                final double finalOverallScore = result.has("overall_score") ? result.get("overall_score").getAsDouble() : 0.0;
                final String finalResultLabel = result.has("result_label") ? result.get("result_label").getAsString() : "";

                android.util.Log.d("TOOTH_SCAN_ANDROID_DEBUG", "Parsed values -> Class: " + predictedClass 
                        + ", Conf: " + confidence 
                        + ", Inflammation: " + finalInflammationScore 
                        + ", Cleanliness: " + finalCleanlinessScore 
                        + ", Overall: " + finalOverallScore);

                // Only save to Supabase history if the scan is valid
                if (!"Invalid".equalsIgnoreCase(predictedClass)) {
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
                } else {
                    android.util.Log.d("SCAN_RESPONSE", "Invalid image scan. Skipped saving to history.");
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

                    android.widget.TextView tvRecs =
                            resultView.findViewById(R.id.tvRecommendations);

                    // Dynamic UI rendering based on the predicted class
                    if ("Healthy".equalsIgnoreCase(predictedClass)) {
                        tvResult.setText("Healthy Gums (" + (int)(confidence * 100) + "% confidence)");
                        tvResult.setTextColor(android.graphics.Color.parseColor("#10B981")); // Emerald Green
                        tvDesc.setText("🟢 Your gums and cleanliness look good. Keep maintaining your routine!");
                        tvRecs.setText("✓ Brush twice daily for 2 minutes\n\n✓ Floss once daily\n\n✓ Rinse after meals\n\n✓ Regular dental checkups every 6 months");
                    } 
                    else if ("Gingivitis".equalsIgnoreCase(predictedClass)) {
                        tvResult.setText("Gingivitis Detected (" + (int)(confidence * 100) + "% confidence)");
                        tvResult.setTextColor(android.graphics.Color.parseColor("#F59E0B")); // Amber Orange
                        tvDesc.setText("🟠 Signs of gum inflammation (gingivitis) detected. Please improve oral hygiene and consider visiting a dentist.");
                        tvRecs.setText("✓ Brush carefully around the gumline using a soft-bristled brush\n\n✓ Floss daily to remove plaque between teeth\n\n✓ Use an antiseptic mouthwash to reduce inflammation\n\n✓ Schedule a professional cleaning with a dentist");
                    } 
                    else if ("Calculus".equalsIgnoreCase(predictedClass)) {
                        tvResult.setText("Dental Calculus Detected (" + (int)(confidence * 100) + "% confidence)");
                        tvResult.setTextColor(android.graphics.Color.parseColor("#EF4444")); // Red
                        tvDesc.setText("🔴 Significant plaque buildup or calculus (tartar) detected. Hardened calculus cannot be brushed away and requires professional removal.");
                        tvRecs.setText("✓ Visit a dentist for a professional scaling and cleaning\n\n✓ Use tartar-control toothpaste to prevent new buildup\n\n✓ Brush and floss meticulously twice a day\n\n✓ Use an antibacterial rinse to protect gums");
                    } 
                    else { // Invalid
                        tvResult.setText("Invalid Image");
                        tvResult.setTextColor(android.graphics.Color.parseColor("#9CA3AF")); // Gray
                        tvDesc.setText("⚠️ The uploaded image does not appear to be a clear photo of teeth or the oral cavity. Please take a clear, well-lit photo of your teeth.");
                        tvRecs.setText("✓ Ensure the camera is focused on your teeth\n\n✓ Use adequate lighting or flashlight\n\n✓ Avoid blurry or distant images\n\n✓ Do not upload faces, pets, or random objects");
                    }

                    tvPlaque.setText(
                            "Gingival inflammation: " + (int)displayInflammation + "%"
                    );

                    tvGum.setText(
                            "Gum cleanliness: " + (int)displayCleanliness + "%"
                    );

                    tvClean.setText(
                            "Overall gum health: " + (int)displayOverall + "%"
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
                final String errorMsg;
                if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
                    errorMsg = "Cannot reach server. Ensure phone and PC are on the same Wi-Fi and server is running.";
                } else if (e instanceof java.io.IOException) {
                    errorMsg = "Network error: " + e.getMessage();
                } else {
                    errorMsg = "Failed to analyze scan: " + e.getMessage();
                }
                runOnUiThread(() ->
                        android.widget.Toast.makeText(
                                ToothScanActivity.this,
                                errorMsg,
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