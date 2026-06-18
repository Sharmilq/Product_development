package com.dentnova.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dentnova.app.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private LinearLayout brushingList, flossingList, toothbrushList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupTimeCard(
                findViewById(R.id.cardBrushing),
                "Brushing Reminders",
                "Add morning, night or custom brushing reminders",
                R.drawable.ic_notifications_outlined,
                "Add brushing reminder"
        );

        setupTimeCard(
                findViewById(R.id.cardFlossing),
                "Flossing Reminders",
                "Add one or more flossing reminders",
                R.drawable.ic_notifications_outlined,
                "Add flossing reminder"
        );

        setupDateCard(
                findViewById(R.id.cardToothbrush),
                "Toothbrush Replacement",
                "Add replacement dates for your toothbrush",
                R.drawable.ic_calendar_today,
                "Add replacement date"
        );
        loadReminders();
    }

    private void loadReminders() {
        new Thread(() -> {
            try {
                com.dentnova.app.services.ApiService.cleanupExpiredReminders(this);

                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService.getReminders(this);

                if (result.has("success") && result.get("success").getAsBoolean()) {
                    com.google.gson.JsonArray reminders =
                            result.getAsJsonArray("reminders");

                    runOnUiThread(() -> {
                        // Clear existing items to prevent duplication
                        if (brushingList != null) brushingList.removeAllViews();
                        if (flossingList != null) flossingList.removeAllViews();
                        if (toothbrushList != null) toothbrushList.removeAllViews();

                        for (int i = 0; i < reminders.size(); i++) {
                            com.google.gson.JsonObject r = reminders.get(i).getAsJsonObject();

                            if (r.has("enabled") && !r.get("enabled").isJsonNull() && !r.get("enabled").getAsBoolean()) {
                                continue;
                            }

                            String title = r.get("title").getAsString();
                            String time = r.get("time").getAsString();
                            String days = r.get("days").getAsString();
                            int reminderId = r.get("id").getAsInt();

                            if (title.contains("Brushing")) {
                                addReminderItem(brushingList, "⏰ " + time + "\n" + days, reminderId);
                            } else if (title.contains("Flossing")) {
                                addReminderItem(flossingList, "⏰ " + time + "\n" + days, reminderId);
                            } else {
                                addReminderItem(toothbrushList, "📅 " + time + "\n" + days, reminderId);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("RemindersActivity", "Error loading reminders list", e);
            }
        }).start();
    }

    private void setupTimeCard(View card, String title, String subtitle, int icon, String buttonText) {
        ImageView ivIcon = card.findViewById(R.id.ivReminderIcon);
        TextView tvTitle = card.findViewById(R.id.tvReminderTitle);
        TextView tvSubtitle = card.findViewById(R.id.tvReminderSubtitle);
        MaterialButton btn = card.findViewById(R.id.btnSetReminder);
        LinearLayout list = card.findViewById(R.id.llReminderItems);

        ivIcon.setImageResource(icon);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        btn.setText(buttonText);

        btn.setOnClickListener(v -> openTimePicker(list, title));
        if (title.contains("Brushing")) {
            brushingList = list;
        } else if (title.contains("Flossing")) {
            flossingList = list;
        }
    }

    private void setupDateCard(View card, String title, String subtitle, int icon, String buttonText) {
        ImageView ivIcon = card.findViewById(R.id.ivReminderIcon);
        TextView tvTitle = card.findViewById(R.id.tvReminderTitle);
        TextView tvSubtitle = card.findViewById(R.id.tvReminderSubtitle);
        MaterialButton btn = card.findViewById(R.id.btnSetReminder);
        LinearLayout list = card.findViewById(R.id.llReminderItems);
        toothbrushList = list;
        ivIcon.setImageResource(icon);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        btn.setText(buttonText);

        btn.setOnClickListener(v -> openDatePicker(list, title));
    }

    private void openTimePicker(LinearLayout list, String reminderName) {
        String[] allDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        boolean[] checkedDays = new boolean[7];

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select days")
                .setMultiChoiceItems(allDays, checkedDays,
                        (d, which, isChecked) -> checkedDays[which] = isChecked)
                .setPositiveButton("Next", (d, w) -> {
                    StringBuilder selectedDays = new StringBuilder();
                    for (int i = 0; i < allDays.length; i++) {
                        if (checkedDays[i]) {
                            if (selectedDays.length() > 0) selectedDays.append(",");
                            selectedDays.append(allDays[i]);
                        }
                    }

                    TimePickerDialog dialog = new TimePickerDialog(
                            this,
                            (view, hourOfDay, minute) -> {
                                Calendar selected = Calendar.getInstance();
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);

                                String formatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(selected.getTime());

                                new Thread(() -> {
                                    try {
                                        com.google.gson.JsonObject result = com.dentnova.app.services.ApiService
                                                .addReminder(
                                                        RemindersActivity.this,
                                                        reminderName,
                                                        formatted,
                                                        selectedDays.toString()
                                                );
                                        int reminderId = -1;
                                        if (result.has("success") && result.get("success").getAsBoolean() && result.has("id")) {
                                            reminderId = result.get("id").getAsInt();
                                        }
                                        com.dentnova.app.utils.ReminderScheduler.scheduleReminder(
                                                RemindersActivity.this,
                                                reminderName,
                                                hourOfDay,
                                                minute,
                                                reminderId
                                        );
                                        runOnUiThread(this::loadReminders);
                                    } catch (Exception e) {
                                        android.util.Log.e("RemindersActivity", "Error adding time-based reminder", e);
                                    }
                                }).start();
                            },
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                            Calendar.getInstance().get(Calendar.MINUTE),
                            false
                    );
                    dialog.show();
                })
                .show();
    }

    private void openDatePicker(LinearLayout list, String reminderName) {
        Calendar now = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    // Validate: not a past date
                    Calendar todayStart = Calendar.getInstance();
                    todayStart.set(Calendar.HOUR_OF_DAY, 0);
                    todayStart.set(Calendar.MINUTE, 0);
                    todayStart.set(Calendar.SECOND, 0);
                    todayStart.set(Calendar.MILLISECOND, 0);
                    if (selected.before(todayStart)) {
                        Toast.makeText(this, "Please select today or a future date.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(selected.getTime());

                    new Thread(() -> {
                        try {
                            com.google.gson.JsonObject result = com.dentnova.app.services.ApiService.addReminder(
                                    RemindersActivity.this,
                                    reminderName,
                                    formatted,
                                    "ONCE"
                            );
                            int reminderId = -1;
                            if (result.has("success") && result.get("success").getAsBoolean() && result.has("id")) {
                                reminderId = result.get("id").getAsInt();
                            }
                            com.dentnova.app.utils.ReminderScheduler.scheduleDateReminder(
                                    RemindersActivity.this,
                                    reminderName,
                                    selected.getTimeInMillis(),
                                    reminderId
                            );
                            runOnUiThread(this::loadReminders);
                        } catch (Exception e) {
                            android.util.Log.e("RemindersActivity", "Error adding date-based reminder", e);
                        }
                    }).start();

                    Toast.makeText(this, reminderName + " added", Toast.LENGTH_SHORT).show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        // ── PART 2: Prevent past date selection ──
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    /**
     * Adds a reminder row with a visible delete (trash) icon.
     * PART 1: Delete icon for normal reminders.
     */
    private void addReminderItem(LinearLayout list, String text, int reminderId) {
        // Outer horizontal row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_white);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rowParams);
        row.setPadding(18, 14, 18, 14);

        // Text label
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(com.google.android.material.color.MaterialColors.getColor(list,
                com.google.android.material.R.attr.colorOnSurface));
        item.setTextSize(14f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        item.setLayoutParams(textParams);

        // Delete icon (trash)
        ImageView ivDelete = new ImageView(this);
        ivDelete.setImageResource(R.drawable.ic_delete_outline);
        int iconSize = (int) (28 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
        ivDelete.setLayoutParams(iconParams);
        ivDelete.setColorFilter(0xFFEF4444); // Red trash icon
        ivDelete.setContentDescription("Delete reminder");

        row.addView(item);
        row.addView(ivDelete);

        // Delete tap handler
        ivDelete.setOnClickListener(v -> {
            android.util.Log.d("DELETE_REMINDER_CLICKED", "DELETE_REMINDER_CLICKED: ID=" + reminderId);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete this reminder?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                com.dentnova.app.services.ApiService.deleteReminder(
                                        RemindersActivity.this,
                                        reminderId
                                );
                                android.util.Log.d("REMINDER_DELETED_SUPABASE", "REMINDER_DELETED_SUPABASE: ID=" + reminderId);
                                com.dentnova.app.utils.ReminderScheduler.cancelReminderAlarm(
                                        RemindersActivity.this,
                                        reminderId
                                );
                                android.util.Log.d("REMINDER_ALARM_CANCELLED", "REMINDER_ALARM_CANCELLED: ID=" + reminderId);
                                runOnUiThread(() -> list.removeView(row));
                            } catch (Exception e) {
                                android.util.Log.e("RemindersActivity", "Error deleting reminder", e);
                            }
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Also keep long-press as backup
        row.setOnLongClickListener(v -> {
            ivDelete.performClick();
            return true;
        });

        list.addView(row);
    }
}