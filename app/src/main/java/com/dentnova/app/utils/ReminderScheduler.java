package com.dentnova.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.dentnova.app.receivers.ReminderReceiver;

import java.util.Calendar;

public class ReminderScheduler {

    public static void scheduleReminder(
            Context context,
            String title,
            int hour,
            int minute,
            int id
    ) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("body", "Time for your DentNova reminder 🦷");

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }

    public static void scheduleReminder(
            Context context,
            String title,
            int hour,
            int minute
    ) {
        scheduleReminder(context, title, hour, minute, (int) System.currentTimeMillis());
    }

    public static void scheduleDateReminder(
            Context context,
            String title,
            long timeMillis,
            int id
    ) {

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("body", "Time for your DentNova reminder 🦷");

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
        );
    }

    public static void scheduleDateReminder(
            Context context,
            String title,
            long timeMillis
    ) {
        scheduleDateReminder(context, title, timeMillis, (int) System.currentTimeMillis());
    }

    public static void cancelReminderAlarm(Context context, int id) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            android.util.Log.d("REMINDER_ALARM_CANCELLED", "REMINDER_ALARM_CANCELLED: ID=" + id);
        }
    }

    public static void scheduleVisitNotifications(Context context, int visitId, String dateStr, String timeStr) {
        android.util.Log.d("ReminderScheduler", "VISIT_DATE: " + dateStr);
        android.util.Log.d("ReminderScheduler", "VISIT_TIME: " + timeStr);

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date visitDate = sdf.parse(dateStr + " " + timeStr);
            if (visitDate == null) return;

            long nowMs = System.currentTimeMillis();

            // Same day alarm time
            Calendar sameDayCal = Calendar.getInstance();
            sameDayCal.setTime(visitDate);
            long sameDayMs = sameDayCal.getTimeInMillis();
            android.util.Log.d("VISIT_SAME_DAY_TRIGGER_MILLIS", "VISIT_SAME_DAY_TRIGGER_MILLIS: " + sameDayMs);

            // One day before alarm time
            Calendar dayBeforeCal = Calendar.getInstance();
            dayBeforeCal.setTime(visitDate);
            dayBeforeCal.add(Calendar.DAY_OF_YEAR, -1);
            long dayBeforeMs = dayBeforeCal.getTimeInMillis();
            android.util.Log.d("VISIT_DAY_BEFORE_TRIGGER_MILLIS", "VISIT_DAY_BEFORE_TRIGGER_MILLIS: " + dayBeforeMs);

            // 7. If selected visit date/time is already past
            if (sameDayMs <= nowMs) {
                android.util.Log.d("VISIT_ALARM_SKIPPED_PAST", "VISIT_ALARM_SKIPPED_PAST");
                return;
            }

            // Determine if visit is today
            Calendar today = Calendar.getInstance();
            Calendar visitCal = Calendar.getInstance();
            visitCal.setTime(visitDate);

            boolean isToday = today.get(Calendar.YEAR) == visitCal.get(Calendar.YEAR) &&
                              today.get(Calendar.DAY_OF_YEAR) == visitCal.get(Calendar.DAY_OF_YEAR);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // 9. Exact alarm permission checks on SDK >= 31
            boolean useExact = true;
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                useExact = alarmManager.canScheduleExactAlarms();
            }

            // 1. Same day notification (sameDayMs)
            if (sameDayMs > nowMs) {
                Intent intentSame = new Intent(context, ReminderReceiver.class);
                intentSame.setAction("com.dentnova.app.ACTION_SHOW_REMINDER");
                intentSame.putExtra("title", "Dental Visit Today 🦷");
                intentSame.putExtra("body", "Your dental visit is scheduled today at " + timeStr + ".");

                PendingIntent piSame = PendingIntent.getBroadcast(
                        context,
                        visitId * 10 + 2,
                        intentSame,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (useExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            sameDayMs,
                            piSame
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            sameDayMs,
                            piSame
                    );
                }
                android.util.Log.d("VISIT_SAME_DAY_SCHEDULED", "VISIT_SAME_DAY_SCHEDULED");

                // 12. Test notification log if within 2 minutes
                long diffMs = sameDayMs - nowMs;
                if (diffMs <= 120000) {
                    android.util.Log.d("TEST_NOTIFICATION_LOG", "TEST_NOTIFICATION_LOG: visit is within 2 minutes, scheduling test notification to fire at: " + sameDayMs);
                }
            }

            // 2. Day before notification (dayBeforeMs)
            if (!isToday && dayBeforeMs > nowMs) {
                Intent intentBefore = new Intent(context, ReminderReceiver.class);
                intentBefore.setAction("com.dentnova.app.ACTION_SHOW_REMINDER");
                intentBefore.putExtra("title", "Dental Visit Tomorrow 🦷");
                intentBefore.putExtra("body", "You have a dental visit scheduled tomorrow at " + timeStr + ".");

                PendingIntent piBefore = PendingIntent.getBroadcast(
                        context,
                        visitId * 10 + 1,
                        intentBefore,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (useExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            dayBeforeMs,
                            piBefore
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            dayBeforeMs,
                            piBefore
                    );
                }
                android.util.Log.d("VISIT_DAY_BEFORE_SCHEDULED", "VISIT_DAY_BEFORE_SCHEDULED");
            } else if (isToday) {
                android.util.Log.d("ReminderScheduler", "Day before alarm skipped because visit is today.");
            }

        } catch (Exception e) {
            android.util.Log.e("VisitReminder", "Error scheduling notifications", e);
        }
    }

    public static void cancelVisitNotifications(Context context, int visitId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intentBefore = new Intent(context, ReminderReceiver.class);
        intentBefore.setAction("com.dentnova.app.ACTION_SHOW_REMINDER");
        PendingIntent piBefore = PendingIntent.getBroadcast(
                context,
                visitId * 10 + 1,
                intentBefore,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (piBefore != null) {
            alarmManager.cancel(piBefore);
            piBefore.cancel();
        }

        Intent intentSame = new Intent(context, ReminderReceiver.class);
        intentSame.setAction("com.dentnova.app.ACTION_SHOW_REMINDER");
        PendingIntent piSame = PendingIntent.getBroadcast(
                context,
                visitId * 10 + 2,
                intentSame,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (piSame != null) {
            alarmManager.cancel(piSame);
            piSame.cancel();
        }

        android.util.Log.d("VISIT_NOTIFICATION_CANCELLED", "VISIT_NOTIFICATION_CANCELLED: ID=" + visitId);
    }
}