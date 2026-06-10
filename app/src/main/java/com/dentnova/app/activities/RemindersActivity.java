package com.dentnova.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
                com.google.gson.JsonObject result =
                        com.dentnova.app.services.ApiService.getReminders(this);

                if (result.has("success") && result.get("success").getAsBoolean()) {
                    com.google.gson.JsonArray reminders =
                            result.getAsJsonArray("reminders");

                    runOnUiThread(() -> {
                        for (int i = 0; i < reminders.size(); i++) {
                            com.google.gson.JsonObject r = reminders.get(i).getAsJsonObject();

                            String title = r.get("title").getAsString();
                            String time = r.get("time").getAsString();
                            String days = r.get("days").getAsString();

                            if (title.contains("Brushing")) {
                                addReminderItem(brushingList, "⏰ " + time + "\n" + days, r.get("id").getAsInt());
                            } else if (title.contains("Flossing")) {
                                addReminderItem(flossingList, "⏰ " + time + "\n" + days, r.get("id").getAsInt());
                            } else {
                                addReminderItem(toothbrushList, "📅 " + time+ "\n" + days, r.get("id").getAsInt());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        Calendar now = Calendar.getInstance();
        String[] allDays = {
                "Mon","Tue","Wed","Thu","Fri","Sat","Sun"
        };

        boolean[] checkedDays = new boolean[7];

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select days")
                .setMultiChoiceItems(allDays, checkedDays,
                        (d, which, isChecked) ->
                                checkedDays[which] = isChecked)

                .setPositiveButton("Next", (d, w) -> {

                    StringBuilder selectedDays =
                            new StringBuilder();

                    for (int i = 0; i < allDays.length; i++) {
                        if (checkedDays[i]) {

                            if (selectedDays.length() > 0)
                                selectedDays.append(",");

                            selectedDays.append(allDays[i]);
                        }
                    }

                    TimePickerDialog dialog = new TimePickerDialog(
                            this,
                            (view, hourOfDay, minute) -> {

                                Calendar selected =
                                        Calendar.getInstance();

                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);

                                String formatted =
                                        new SimpleDateFormat(
                                                "hh:mm a",
                                                Locale.getDefault()
                                        ).format(selected.getTime());

                                addReminderItem(
                                        list,
                                        "⏰ " + formatted
                                                + "\n"
                                                + selectedDays,
                                        -1
                                );

                                new Thread(() -> {
                                    try {
                                        com.dentnova.app.services.ApiService
                                                .addReminder(
                                                        RemindersActivity.this,
                                                        reminderName,
                                                        formatted,
                                                        selectedDays.toString()
                                                );
                                        com.dentnova.app.utils.ReminderScheduler.scheduleReminder(
                                                RemindersActivity.this,
                                                reminderName,
                                                hourOfDay,
                                                minute
                                        );
                                    } catch (Exception e) {
                                        e.printStackTrace();
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

                    String formatted = new SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                    ).format(selected.getTime());

                    addReminderItem(list, "📅 " + formatted, -1);
                    new Thread(() -> {
                        try {
                            com.dentnova.app.services.ApiService.addReminder(
                                    RemindersActivity.this,
                                    reminderName,
                                    formatted,
                                    "ONCE"
                            );
                            com.dentnova.app.utils.ReminderScheduler.scheduleDateReminder(
                                    RemindersActivity.this,
                                    reminderName,
                                    selected.getTimeInMillis()
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }).start();

                    Toast.makeText(this, reminderName + " added", Toast.LENGTH_SHORT).show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void addReminderItem(LinearLayout list, String text, int reminderId) {
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(0xFF1A2332);
        item.setTextSize(14f);
        item.setPadding(18, 14, 18, 14);
        item.setBackgroundResource(R.drawable.bg_card_white);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 10);
        item.setLayoutParams(params);

        item.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Reminder Options")
                    .setItems(new String[]{"Delete reminder"}, (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                com.dentnova.app.services.ApiService.deleteReminder(
                                        RemindersActivity.this,
                                        reminderId
                                );

                                runOnUiThread(() -> list.removeView(item));

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    })
                    .show();

            return true;
        });

        list.addView(item);
    }
}