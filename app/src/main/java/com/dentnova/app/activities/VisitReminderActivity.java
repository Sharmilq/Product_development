package com.dentnova.app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.dentnova.app.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class VisitReminderActivity extends AppCompatActivity {

    private LinearLayout llVisits;
    private LinearLayout llPastVisits;
    private TextView tvPastVisitsHeader;

    private CardView cardNextVisit;

    private TextView tvNextVisitDate;
    private TextView tvNextVisitClinic;
    private TextView tvCountdown;

    private Calendar selectedDateTime = Calendar.getInstance();

    private String clinicName = "";
    private String reason = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_reminder);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        102
                );
            } else {
                android.util.Log.d("NOTIFICATION_PERMISSION_GRANTED", "NOTIFICATION_PERMISSION_GRANTED");
            }
        } else {
            android.util.Log.d("NOTIFICATION_PERMISSION_GRANTED", "NOTIFICATION_PERMISSION_GRANTED (Legacy API)");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        llVisits = findViewById(R.id.llVisits);
        llPastVisits = findViewById(R.id.llPastVisits);
        tvPastVisitsHeader = findViewById(R.id.tvPastVisitsHeader);

        cardNextVisit = findViewById(R.id.cardNextVisit);

        tvNextVisitDate = findViewById(R.id.tvNextVisitDate);
        tvNextVisitClinic = findViewById(R.id.tvNextVisitClinic);
        tvCountdown = findViewById(R.id.tvCountdown);

        MaterialButton btnAddVisit = findViewById(R.id.btnAddVisit);
        btnAddVisit.setOnClickListener(v -> showVisitDialog());
        loadVisits();
    }

    private void loadVisits() {
        new Thread(() -> {
            try {
                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService.getVisitReminder(this);

                if (result.has("success") && result.get("success").getAsBoolean()) {

                    com.google.gson.JsonArray visits = result.getAsJsonArray("visits");

                    runOnUiThread(() -> {
                        llVisits.removeAllViews();
                        if (llPastVisits != null) llPastVisits.removeAllViews();
                        if (tvPastVisitsHeader != null) tvPastVisitsHeader.setVisibility(View.GONE);

                        if (visits.size() == 0) {
                            cardNextVisit.setVisibility(View.GONE);
                            return;
                        }

                        com.google.gson.JsonObject nearestUpcoming = null;
                        long minUpcomingDiff = Long.MAX_VALUE;

                        java.text.SimpleDateFormat sdf =
                                new java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault());

                        for (int i = 0; i < visits.size(); i++) {
                            com.google.gson.JsonObject visit = visits.get(i).getAsJsonObject();
                            String vDate = visit.get("visit_date").getAsString();
                            String vTime = visit.get("visit_time").getAsString();
                            String note = visit.get("note").getAsString();
                            int visitId = visit.get("id").getAsInt();

                            String[] parts = note.split(" - ", 2);
                            String clinic = parts.length > 0 ? parts[0] : "";
                            String visitReason = parts.length > 1 ? parts[1] : "";

                            boolean isPast = false;
                            long diff = 0;
                            try {
                                java.util.Date visitDate = sdf.parse(vDate + " " + vTime);
                                if (visitDate != null) {
                                    diff = visitDate.getTime() - System.currentTimeMillis();
                                    if (diff < 0) isPast = true;
                                }
                            } catch (Exception e) {
                                // Assume upcoming on parse error
                            }

                            if (isPast) {
                                if (tvPastVisitsHeader != null) tvPastVisitsHeader.setVisibility(View.VISIBLE);
                                addVisitCard(llPastVisits, visitId, vDate + " • " + vTime, clinic, visitReason, true);
                            } else {
                                addVisitCard(llVisits, visitId, vDate + " • " + vTime, clinic, visitReason, false);
                                if (diff < minUpcomingDiff) {
                                    minUpcomingDiff = diff;
                                    nearestUpcoming = visit;
                                }
                            }
                        }

                        if (nearestUpcoming != null) {
                            String nextDate = nearestUpcoming.get("visit_date").getAsString();
                            String nextTime = nearestUpcoming.get("visit_time").getAsString();
                            String nextNote = nearestUpcoming.get("note").getAsString();

                            cardNextVisit.setVisibility(View.VISIBLE);
                            tvNextVisitDate.setText(nextDate + " • " + nextTime);
                            tvNextVisitClinic.setText(nextNote);

                            long days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(minUpcomingDiff);
                            if (days < 0) days = 0;
                            tvCountdown.setText(days + " days remaining");
                        } else {
                            cardNextVisit.setVisibility(View.GONE);
                        }
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("VisitReminderActivity", "Error loading dentist visit reminders", e);
            }
        }).start();
    }

    private void showVisitDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etClinic = new EditText(this);
        etClinic.setHint("Clinic name");

        EditText etReason = new EditText(this);
        etReason.setHint("Reason for visit");

        layout.addView(etClinic);
        layout.addView(etReason);

        new AlertDialog.Builder(this)
                .setTitle("Add Dental Visit")
                .setView(layout)
                .setPositiveButton("Continue", (dialog, which) -> {
                    clinicName = etClinic.getText().toString().trim();
                    reason = etReason.getText().toString().trim();
                    openDatePicker();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openDatePicker() {
        Calendar today = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    openTimePicker();
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        // ── PART 2: Block past dates ──
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);

                    // Validate: if today is selected, time must be in future
                    Calendar todayNow = Calendar.getInstance();
                    if (selectedDateTime.before(todayNow)) {
                        Toast.makeText(this, "Please select a future visit time.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveVisitReminder();
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    private void saveVisitReminder() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("NOTIFICATION_PERMISSION_STATUS", "NOTIFICATION_PERMISSION_STATUS: DENIED");
                androidx.core.app.ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        102
                );
                return;
            } else {
                android.util.Log.d("NOTIFICATION_PERMISSION_STATUS", "NOTIFICATION_PERMISSION_STATUS: GRANTED");
            }
        } else {
            android.util.Log.d("NOTIFICATION_PERMISSION_STATUS", "NOTIFICATION_PERMISSION_STATUS: GRANTED (Legacy API)");
        }

        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(selectedDateTime.getTime());

        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(selectedDateTime.getTime());

        android.util.Log.d("VISIT_REMINDER_SAVE_CLICKED", "VISIT_REMINDER_SAVE_CLICKED: date=" + date + ", time=" + time);

        new Thread(() -> {
            try {
                com.google.gson.JsonObject res = com.dentnova.app.services.ApiService.saveVisitReminder(
                        VisitReminderActivity.this,
                        date,
                        time,
                        clinicName + " - " + reason
                );

                if (res.has("success") && res.get("success").getAsBoolean() && res.has("id")) {
                    int visitId = res.get("id").getAsInt();
                    android.util.Log.d("VISIT_SAVE_SUCCESS", "VISIT_SAVE_SUCCESS");
                    android.util.Log.d("VISIT_ID", "VISIT_ID: " + visitId);

                    com.dentnova.app.utils.ReminderScheduler.scheduleVisitNotifications(
                            VisitReminderActivity.this,
                            visitId,
                            date,
                            time
                    );

                    runOnUiThread(() -> {
                        // ── PART 6: Success confirmation message ──
                        new AlertDialog.Builder(VisitReminderActivity.this)
                                .setTitle("Visit Reminder Saved ✅")
                                .setMessage("Visit reminder saved. You will be notified 1 day before and on the visit day.")
                                .setPositiveButton("OK", null)
                                .show();
                        loadVisits();
                    });
                } else {
                    android.util.Log.e("VISIT_SAVE_FAILED", "Failed to save visit: " + res.toString());
                    runOnUiThread(() ->
                            Toast.makeText(VisitReminderActivity.this,
                                    "Failed to save visit reminder. Please try again.",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                android.util.Log.e("VisitReminderActivity", "Error saving dentist visit reminder", e);
                runOnUiThread(() ->
                        Toast.makeText(VisitReminderActivity.this,
                                "Error saving visit reminder.",
                                Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * PART 1: Each visit card has a visible red delete icon.
     * Long-press also works as backup.
     */
    private void addVisitCard(LinearLayout container, int visitId, String dateTime,
                              String clinic, String visitReason, boolean isPast) {

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        card.setRadius(24f);
        card.setCardElevation(2f);
        card.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                container, com.google.android.material.R.attr.colorSurfaceVariant));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 20;
        card.setLayoutParams(params);

        // Inner vertical layout
        LinearLayout layoutContainer = new LinearLayout(this);
        layoutContainer.setOrientation(LinearLayout.VERTICAL);
        layoutContainer.setPadding(24, 24, 24, 24);

        // ── Top row: title + delete icon ──
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🦷 Dental Visit");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                container, com.google.android.material.R.attr.colorOnSurface));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(titleParams);

        // Delete/trash icon — visible, red
        ImageView ivDelete = new ImageView(this);
        ivDelete.setImageResource(R.drawable.ic_delete_outline);
        int iconSize = (int) (26 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        ivDelete.setLayoutParams(iconParams);
        ivDelete.setColorFilter(0xFFEF4444); // Red
        ivDelete.setContentDescription("Delete visit reminder");

        topRow.addView(tvTitle);
        topRow.addView(ivDelete);
        layoutContainer.addView(topRow);

        // Date & time
        TextView tvDate = new TextView(this);
        tvDate.setText(dateTime);
        tvDate.setTextSize(15f);
        tvDate.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                container, com.google.android.material.R.attr.colorOnSurfaceVariant));
        tvDate.setPadding(0, 10, 0, 0);
        layoutContainer.addView(tvDate);

        // Clinic
        TextView tvClinicView = new TextView(this);
        tvClinicView.setText(clinic);
        tvClinicView.setTextSize(15f);
        tvClinicView.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                container, com.google.android.material.R.attr.colorOnSurface));
        tvClinicView.setPadding(0, 10, 0, 0);
        layoutContainer.addView(tvClinicView);

        // Reason
        TextView tvReasonView = new TextView(this);
        tvReasonView.setText(visitReason);
        tvReasonView.setTextSize(14f);
        tvReasonView.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                container, com.google.android.material.R.attr.colorOnSurfaceVariant));
        tvReasonView.setPadding(0, 8, 0, 0);
        layoutContainer.addView(tvReasonView);

        // Notification info label (upcoming visits only)
        if (!isPast) {
            TextView tvReminderInfo = new TextView(this);
            tvReminderInfo.setText("🔔 Reminder set for 1 day before and on visit day");
            tvReminderInfo.setTextSize(13f);
            tvReminderInfo.setTextColor(0xFF0097A7);
            tvReminderInfo.setPadding(0, 8, 0, 0);
            tvReminderInfo.setTypeface(null, android.graphics.Typeface.ITALIC);
            layoutContainer.addView(tvReminderInfo);
        }

        card.addView(layoutContainer);

        // ── Delete handler (icon tap) ──
        ivDelete.setOnClickListener(v -> showDeleteConfirmation(container, card, visitId));

        // ── Long press backup ──
        card.setOnLongClickListener(v -> {
            showDeleteConfirmation(container, card, visitId);
            return true;
        });

        container.addView(card);
    }

    private void showDeleteConfirmation(LinearLayout container, View card, int visitId) {
        android.util.Log.d("DELETE_VISIT_CLICKED", "DELETE_VISIT_CLICKED: ID=" + visitId);

        new AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            com.dentnova.app.services.ApiService.deleteVisitReminder(
                                    VisitReminderActivity.this, visitId);
                            android.util.Log.d("VISIT_DELETED_SUPABASE", "VISIT_DELETED_SUPABASE: ID=" + visitId);

                            com.dentnova.app.utils.ReminderScheduler.cancelVisitNotifications(
                                    VisitReminderActivity.this, visitId);
                            android.util.Log.d("VISIT_ALARMS_CANCELLED", "VISIT_ALARMS_CANCELLED: ID=" + visitId);

                            runOnUiThread(() -> {
                                // ── PART 6: Deletion success message ──
                                Toast.makeText(VisitReminderActivity.this,
                                        "Visit reminder deleted.", Toast.LENGTH_SHORT).show();
                                loadVisits();
                            });
                        } catch (Exception e) {
                            android.util.Log.e("VisitReminderActivity", "Error deleting visit", e);
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("NOTIFICATION_PERMISSION_STATUS", "NOTIFICATION_PERMISSION_STATUS: GRANTED");
            } else {
                android.util.Log.d("NOTIFICATION_PERMISSION_STATUS", "NOTIFICATION_PERMISSION_STATUS: DENIED");
            }
        }
    }
}