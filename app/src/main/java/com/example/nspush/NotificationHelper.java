package com.example.nspush;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

final class NotificationHelper {
    static final String CHANNEL_ID = "nightscout_readings";
    private static final int READING_NOTIFICATION_ID = 1001;
    private static final int ERROR_NOTIFICATION_ID = 1002;

    private NotificationHelper() {
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Nightscout readings",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Latest glucose readings from Nightscout");
        channel.enableVibration(true);
        channel.enableLights(true);
        channel.setLightColor(Color.GREEN);
        manager.createNotificationChannel(channel);
    }

    static void showReading(Context context, NightscoutEntry entry, int low, int high) {
        ensureChannel(context);
        if (!canNotify(context)) {
            return;
        }
        String status = status(entry, low, high);
        String arrow = entry.trendArrow();
        String title = "Glucose " + entry.sgv + " mg/dL" + (arrow.isEmpty() ? "" : " " + arrow);
        String text = status + " · " + entry.formattedTime();
        if (entry.isStale(System.currentTimeMillis())) {
            text = "Stale data · " + text;
        }
        notify(context, READING_NOTIFICATION_ID, title, text);
    }

    static void showError(Context context, String message) {
        ensureChannel(context);
        if (!canNotify(context)) {
            return;
        }
        notify(context, ERROR_NOTIFICATION_ID, "Nightscout check failed", message);
    }

    private static String status(NightscoutEntry entry, int low, int high) {
        if (entry.sgv < low) {
            return "LOW";
        }
        if (entry.sgv > high) {
            return "HIGH";
        }
        return "In range";
    }

    private static void notify(Context context, int id, String title, String text) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());
        manager.notify(id, builder.build());
    }

    private static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
