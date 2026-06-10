package com.dentnova.app.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
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
import java.util.concurrent.TimeUnit;

public class VisitReminderActivity extends AppCompatActivity {

    private LinearLayout llVisits;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        llVisits = findViewById(R.id.llVisits);

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
                        com.dentnova.app.services.ApiService
                                .getVisitReminder(this);

                if (result.has("success") &&
                        result.get("success").getAsBoolean()) {

                    com.google.gson.JsonArray visits =
                            result.getAsJsonArray("visits");

                    runOnUiThread(() -> {

                        llVisits.removeAllViews();

                        if (visits.size() == 0) {
                            cardNextVisit.setVisibility(View.GONE);
                            return;
                        }

                        com.google.gson.JsonObject nearest =
                                visits.get(0).getAsJsonObject();

                        String nextDate =
                                nearest.get("visit_date").getAsString();

                        String nextTime =
                                nearest.get("visit_time").getAsString();

                        String nextNote =
                                nearest.get("note").getAsString();

                        cardNextVisit.setVisibility(View.VISIBLE);

                        tvNextVisitDate.setText(
                                nextDate + " • " + nextTime
                        );

                        tvNextVisitClinic.setText(nextNote);

                        try {
                            java.text.SimpleDateFormat sdf =
                                    new java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault());

                            java.util.Date visitDate =
                                    sdf.parse(nextDate + " " + nextTime);

                            long diff = visitDate.getTime() - System.currentTimeMillis();
                            long days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff);

                            tvCountdown.setText(days + " days remaining");

                        } catch (Exception e) {
                            tvCountdown.setText("Upcoming visit");
                        }

                        for (int i = 0; i < visits.size(); i++) {

                            com.google.gson.JsonObject visit =
                                    visits.get(i).getAsJsonObject();

                            String note = visit.get("note").getAsString();
                            String[] parts = note.split(" - ", 2);

                            String clinic = parts.length > 0 ? parts[0] : "";
                            String visitReason = parts.length > 1 ? parts[1] : "";

                            addVisitCard(
                                    visit.get("visit_date").getAsString()
                                            + " • "
                                            + visit.get("visit_time").getAsString(),
                                    clinic,
                                    visitReason
                            );
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
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

        dialog.getDatePicker().setMinDate(today.getTimeInMillis());

        dialog.show();
    }

    private void openTimePicker() {

        Calendar now = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {

                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);

                    saveVisitReminder();

                },

                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );

        dialog.show();
    }

    private void saveVisitReminder() {

        String date = new SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
        ).format(selectedDateTime.getTime());

        String time = new SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
        ).format(selectedDateTime.getTime());

        String full = date + " • " + time;
        new Thread(() -> {
            try {
                com.dentnova.app.services.ApiService.saveVisitReminder(
                        VisitReminderActivity.this,
                        date,
                        time,
                        clinicName + " - " + reason
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        long diff = selectedDateTime.getTimeInMillis()
                - System.currentTimeMillis();

        long days = TimeUnit.MILLISECONDS.toDays(diff);

        cardNextVisit.setVisibility(View.VISIBLE);

        tvNextVisitDate.setText(full);

        tvNextVisitClinic.setText(
                clinicName + " • " + reason
        );

        tvCountdown.setText(days + " days remaining");

        addVisitCard(full, clinicName, reason);

        Toast.makeText(
                this,
                "Dental visit reminder added",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void addVisitCard(String dateTime, String clinic, String visitReason) {

        androidx.cardview.widget.CardView card =
                new androidx.cardview.widget.CardView(this);

        card.setRadius(24f);

        card.setCardElevation(2f);

        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 20;

        card.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);

        container.setOrientation(LinearLayout.VERTICAL);

        container.setPadding(24, 24, 24, 24);

        TextView tvTitle = new TextView(this);

        tvTitle.setText("🦷 Dental Visit");

        tvTitle.setTextSize(18f);

        tvTitle.setTextColor(0xFF1A2332);

        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);

        tvDate.setText(dateTime);

        tvDate.setTextSize(15f);

        tvDate.setTextColor(0xFF6B7B8D);

        tvDate.setPadding(0, 10, 0, 0);

        TextView tvClinic = new TextView(this);

        tvClinic.setText(clinic);

        tvClinic.setTextSize(15f);

        tvClinic.setTextColor(0xFF1A2332);

        tvClinic.setPadding(0, 10, 0, 0);

        TextView tvReason = new TextView(this);

        tvReason.setText(visitReason);

        tvReason.setTextSize(14f);

        tvReason.setTextColor(0xFF6B7B8D);

        tvReason.setPadding(0, 8, 0, 0);

        container.addView(tvTitle);
        container.addView(tvDate);
        container.addView(tvClinic);
        container.addView(tvReason);

        card.addView(container);

        llVisits.addView(card);
    }
}