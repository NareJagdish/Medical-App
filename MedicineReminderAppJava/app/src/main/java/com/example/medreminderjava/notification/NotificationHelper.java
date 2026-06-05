package com.example.medreminderjava.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.medreminderjava.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_DOSE_ID = "channel_dose_alerts";
    public static final String CHANNEL_STOCK_ID = "channel_stock_alerts";

    private static final int DOSE_NOTIFICATION_ID_OFFSET = 1000;
    private static final int STOCK_NOTIFICATION_ID_OFFSET = 2000;

    /**
     * Initializes notification channels for Android O and above.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Channel for dosage reminders (High importance for alarms)
            NotificationChannel doseChannel = new NotificationChannel(
                    CHANNEL_DOSE_ID,
                    "Medicine Dose Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            doseChannel.setDescription("Reminds you when it is time to take your medicines.");
            doseChannel.enableVibration(true);

            // Channel for low stock warnings (Default importance)
            NotificationChannel stockChannel = new NotificationChannel(
                    CHANNEL_STOCK_ID,
                    "Medicine Stock Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            stockChannel.setDescription("Alerts you when your medicine stock is running low (10, 5, 2, or 0 days left).");
            stockChannel.enableVibration(true);

            manager.createNotificationChannel(doseChannel);
            manager.createNotificationChannel(stockChannel);
        }
    }

    /**
     * Sends a notification reminding the user to take a specific medicine.
     */
    public static void sendDoseNotification(Context context, long medicineId, String medicineName, String timingInfo) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) medicineId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DOSE_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Medicine Reminder")
                .setContentText("It is time to take " + medicineName + " (" + timingInfo + ").")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            // Check for notification permission on Android 13+ is done in activities, 
            // but we call notify inside a try-catch or after checking permission to avoid crash
            notificationManager.notify(DOSE_NOTIFICATION_ID_OFFSET + (int) medicineId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a notification warning the user about low remaining days of medicine.
     */
    public static void sendStockNotification(Context context, long medicineId, String medicineName, int remainingDays) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) medicineId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Medicine Stock Warning";
        String contentText;
        int icon = android.R.drawable.ic_dialog_info;

        if (remainingDays == 0) {
            title = "Medicine Finished Alert!";
            contentText = "Your medicine '" + medicineName + "' has run out! Please bring medicines.";
            icon = android.R.drawable.ic_dialog_alert;
        } else {
            contentText = "Your medicine '" + medicineName + "' has only " + remainingDays + " days remaining. Please refill soon!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_STOCK_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(STOCK_NOTIFICATION_ID_OFFSET + (int) medicineId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
