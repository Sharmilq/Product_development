package com.dentnova.app.activities;

import android.app.ProgressDialog;
import android.app.AlertDialog;
import com.bumptech.glide.Glide;
import android.widget.AutoCompleteTextView;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import de.hdodenhof.circleimageview.CircleImageView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ProfileSetupActivity — exact Java replica of profile_setup_screen.dart
 *
 * - 88dp circle avatar with camera badge (tapping opens gallery)
 * - Name, Age TextInputEditText
 * - Gender Spinner (Male / Female / Other) — replaces Flutter DropdownButton
 * - ChipGroup for dental concerns (replaces Flutter Wrap + GestureDetector chips)
 * - Continue button → saves profile via ApiService.updateProfile → HomeActivity
 */
public class ProfileSetupActivity extends AppCompatActivity {

    private CircleImageView civAvatar;
    private TextInputEditText etName, etAge;
    private AutoCompleteTextView spinnerGender;
    private TextInputEditText etOtherConcern;
    private ChipGroup chipGroupConcerns;
    private MaterialButton btnContinue;

    private String base64Photo = null;
    private final List<String> selectedConcerns = new ArrayList<>();

    private static final String[] ALL_CONCERNS = {
        "Sensitivity", "Bleeding gums", "Bad breath", "Whitening", "Cavities", "Alignment"
    };
    private void showPhotoOptions() {
        String[] options = {"Take photo", "Choose from gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Profile photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null);
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
                        }
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }
    private static final String[] GENDERS = {"Male", "Female", "Other"};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new TakePicturePreview(), bitmap -> {
                if (bitmap == null) return;

                Bitmap scaled = scaleBitmap(bitmap, 300);
                civAvatar.setImageBitmap(scaled);
                base64Photo = bitmapToBase64(scaled);
            });
    // Gallery picker — replaces Flutter ImagePicker (image_picker: ^1.0.7)
    private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Scale down to match Flutter imageQuality: 70
                Bitmap scaled = scaleBitmap(bmp, 300);
                civAvatar.setImageBitmap(scaled);
                base64Photo = bitmapToBase64(scaled);
            } catch (IOException e) {
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        civAvatar        = findViewById(R.id.civAvatar);
        etName           = findViewById(R.id.etName);
        etAge            = findViewById(R.id.etAge);
        etOtherConcern = findViewById(R.id.etOtherConcern);
        spinnerGender    = findViewById(R.id.spinnerGender);
        chipGroupConcerns= findViewById(R.id.chipGroupConcerns);
        btnContinue      = findViewById(R.id.btnContinue);
        View btnBack = findViewById(R.id.btnBack);
        View btnCancel = findViewById(R.id.btnCancel);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        // Pre-fill defaults matching Flutter _nameCtrl = 'Sharmila', _ageCtrl = '20'
        etName.setText("Name");
        etAge.setText("Age");

        // Gender Spinner setup (replaces Flutter DropdownButton)
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, GENDERS);
        spinnerGender.setAdapter(genderAdapter);
        spinnerGender.setText("Female", false); // default "Female"

        // Avatar tap → gallery
        View layoutAvatarPicker = findViewById(R.id.layoutAvatarPicker);
        layoutAvatarPicker.setOnClickListener(v -> showPhotoOptions());

        // Build concern chips (replaces Flutter _allConcerns.map → _ConcernChip)
        buildConcernChips();
        loadProfile();

        // Pre-select defaults: "Sensitivity" + "Bleeding gums" matching Flutter _concerns

        btnContinue.setOnClickListener(v -> saveProfile());
    }

    /**
     * Builds ChipGroup chips — each chip mirrors Flutter _ConcernChip:
     * Unselected: white bg, #E0E8EF border, #1A2332 text
     * Selected:   #00BCD4 bg, white text
     */
    private void loadProfile() {

        executor.execute(() -> {
            try {

                JsonObject result = ApiService.getProfile(this);

                if (result.has("success") &&
                        result.get("success").getAsBoolean()) {

                    JsonObject profile =
                            result.getAsJsonObject("profile");

                    String photoUrl =
                            profile.has("photo_url") &&
                                    !profile.get("photo_url").isJsonNull()
                                    ? profile.get("photo_url").getAsString()
                                    : "";

                    final String name =
                            profile.has("name")
                                    ? profile.get("name").getAsString()
                                    : "";

                    final String age =
                            profile.has("age") &&
                                    !profile.get("age").isJsonNull()
                                    ? profile.get("age").getAsString()
                                    : "";

                    final String gender =
                            profile.has("gender") &&
                                    !profile.get("gender").isJsonNull()
                                    ? profile.get("gender").getAsString()
                                    : "";

                    runOnUiThread(() -> {

                        etName.setText(name);
                        etAge.setText(age);
                        spinnerGender.setText(gender, false);

                        if (!photoUrl.isEmpty()) {

                            if (!photoUrl.isEmpty()) {

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
                            }
                        }
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("ProfileSetupActivity", "Error loading profile details from Supabase", e);
            }
        });
    }
    private void buildConcernChips() {
        chipGroupConcerns.removeAllViews();
        for (String concern : ALL_CONCERNS) {
            Chip chip = new Chip(this);
            chip.setText(concern);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);

            // Unselected style
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setChipStrokeColorResource(R.color.inputBorder);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(getColor(R.color.textPrimary));
            chip.setTextSize(13f);
            chip.setChipCornerRadius(20f * getResources().getDisplayMetrics().density);
            chip.setPadding(14, 8, 14, 8);

            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    selectedConcerns.add(concern);
                    chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(0xFF00BCD4));
                    chip.setTextColor(0xFFFFFFFF);
                    chip.setChipStrokeColor(
                        android.content.res.ColorStateList.valueOf(0xFF00BCD4));
                } else {
                    selectedConcerns.remove(concern);
                    chip.setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
                    chip.setTextColor(0xFF1A2332);
                    chip.setChipStrokeColor(
                        android.content.res.ColorStateList.valueOf(0xFFE0E8EF));
                }
            });

            chipGroupConcerns.addView(chip);
        }
    }

    /** Syncs chip UI with selectedConcerns list */
    private void refreshChipSelection() {
        for (int i = 0; i < chipGroupConcerns.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupConcerns.getChildAt(i);
            boolean sel = selectedConcerns.contains(chip.getText().toString());
            chip.setChecked(sel);
        }
    }

    /**
     * Saves profile — exact replica of _saveProfile() in profile_setup_screen.dart
     * Collects all fields, calls ApiService.updateProfile, navigates to HomeActivity
     */
    private void saveProfile() {
        String name   = etName.getText() != null ? etName.getText().toString().trim() : "";
        String ageStr = etAge.getText()  != null ? etAge.getText().toString().trim()  : "";
        int    age    = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
        String gender = spinnerGender.getText().toString();

        String other = etOtherConcern.getText() != null ? etOtherConcern.getText().toString().trim() : "";
        String tempConcerns = String.join(", ", selectedConcerns);

        if (!other.isEmpty()) {
            tempConcerns = tempConcerns.isEmpty()
                    ? other
                    : tempConcerns + ", " + other;
        }

        final String concerns = tempConcerns;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Saving…");
        progress.setCancelable(false);
        progress.show();

        btnContinue.setText("Saving...");
        btnContinue.setEnabled(false);

        final String finalBase64 = base64Photo;

        executor.execute(() -> {
            try {
                JsonObject result = ApiService.updateProfile(this, name, age, gender, concerns, finalBase64);
                runOnUiThread(() -> {
                    progress.dismiss();
                    btnContinue.setText("Save changes");
                    btnContinue.setEnabled(true);

                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        String msg = result.has("message") ?
                            result.get("message").getAsString() : "Profile update failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ProfileSetupActivity", "Error saving profile setup data to Supabase", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    btnContinue.setText("Save changes");
                    btnContinue.setEnabled(true);
                    Toast.makeText(this, "Cannot connect to server. Check your WiFi.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /** Scale bitmap down to maxDimension (replaces Flutter imageQuality: 70) */
    private Bitmap scaleBitmap(Bitmap bmp, int maxDim) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        if (w <= maxDim && h <= maxDim) return bmp;
        float scale = (float) maxDim / Math.max(w, h);
        return Bitmap.createScaledBitmap(bmp, (int)(w*scale), (int)(h*scale), true);
    }

    /** Convert bitmap to base64 string — replaces Flutter base64Encode(bytes) */
    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
