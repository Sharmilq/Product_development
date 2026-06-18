package com.dentnova.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dentnova.app.services.ApiService;
import com.dentnova.app.utils.ReminderScheduler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * BootReceiver — reschedules all active visit reminder alarms after device reboot.
 * Runs on BOOT_COMPLETED / LOCKED_BOOT_COMPLETED.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.d("BOOT_RECEIVER", "BOOT_COMPLETED received — rescheduling visit alarms");

        // Run in background thread since network calls are needed
        new Thread(() -> {
            try {
                JsonObject result = ApiService.getVisitReminder(context);
                if (!result.has("success") || !result.get("success").getAsBoolean()) {
                    android.util.Log.e("BOOT_RECEIVER", "Failed to fetch visit reminders");
                    return;
                }

                JsonArray visits = result.getAsJsonArray("visits");
                if (visits == null || visits.size() == 0) {
                    android.util.Log.d("BOOT_RECEIVER", "No visit reminders to reschedule");
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
                long now = System.currentTimeMillis();

                for (int i = 0; i < visits.size(); i++) {
                    try {
                        JsonObject visit = visits.get(i).getAsJsonObject();
                        int visitId = visit.get("id").getAsInt();
                        String visitDate = visit.get("visit_date").getAsString();
                        String visitTime = visit.get("visit_time").getAsString();

                        // Only reschedule future visits
                        java.util.Date dt = sdf.parse(visitDate + " " + visitTime);
                        if (dt != null && dt.getTime() > now) {
                            ReminderScheduler.scheduleVisitNotifications(context, visitId, visitDate, visitTime);
                            android.util.Log.d("BOOT_RECEIVER", "Rescheduled visit ID=" + visitId);
                        } else {
                            android.util.Log.d("BOOT_RECEIVER", "Skipping past visit ID=" + visitId);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BOOT_RECEIVER", "Error rescheduling visit", e);
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("BOOT_RECEIVER", "Error in BootReceiver", e);
            }
        }).start();
    }
}
