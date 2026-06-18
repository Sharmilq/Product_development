package com.dentnova.app.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.dentnova.app.R;
import com.dentnova.app.activities.HomeActivity;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "VISIT_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");

        Log.d("VISIT_RECEIVER_TRIGGERED", "VISIT_RECEIVER_TRIGGERED: title=" + title + " body=" + body);

        if (title == null) title = "DentNova Reminder";
        if (body == null) body = "You have a dental reminder.";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "dentnova_reminders";

        NotificationChannel channel =
                new NotificationChannel(
                        channelId,
                        "DentNova Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                );
        channel.setDescription("Reminders for dental visits and oral care");
        channel.enableVibration(true);
        channel.enableLights(true);

        manager.createNotificationChannel(channel);

        Intent openIntent = new Intent(context, HomeActivity.class);
        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification_bell)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
        Log.d("VISIT_NOTIFICATION_SHOWN", "VISIT_NOTIFICATION_SHOWN");
    }
}