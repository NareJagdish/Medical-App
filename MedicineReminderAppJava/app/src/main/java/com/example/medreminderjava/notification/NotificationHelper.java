package com.example.medreminderjava.notification;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;

public class NotificationHelper {

    public static final String CHANNEL_DOSE_ID = "channel_dose_alerts";
    public static final String CHANNEL_STOCK_ID = "channel_stock_alerts";

    /**
     * Sends an SMS notification to the given mobile number.
     */
    public static void sendSmsNotification(Context context, String mobileNumber, String message) {
        if (mobileNumber == null || mobileNumber.isEmpty()) return;

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            
            if (smsManager != null) {
                smsManager.sendTextMessage(mobileNumber, null, message, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Triggers a high-priority heads-up notification with a full-screen intent.
     * This is the standard way to show an alarm screen from the background on modern Android.
     */
    public static void sendFullScreenAlarmNotification(Context context, long medicineId, String medicineName, String timingInfo) {
        android.content.Intent alarmIntent = new android.content.Intent(context, com.example.medreminderjava.ReminderAlarmActivity.class);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName);
        alarmIntent.putExtra(AlarmReceiver.EXTRA_TIMING_INFO, timingInfo);
        alarmIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

        android.app.PendingIntent fullScreenPendingIntent = android.app.PendingIntent.getActivity(
                context, 
                (int) medicineId, 
                alarmIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, CHANNEL_DOSE_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Care Bell Alarm")
                .setContentText("Time for " + medicineName)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(1000 + (int) medicineId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes notification channels for Android O and above.
     * Kept for system compatibility even if status bar notifications are disabled.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager == null) return;

            android.app.NotificationChannel doseChannel = new android.app.NotificationChannel(
                    CHANNEL_DOSE_ID,
                    "Medicine Dose Alarms",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(doseChannel);

            android.app.NotificationChannel stockChannel = new android.app.NotificationChannel(
                    CHANNEL_STOCK_ID,
                    "Medicine Stock Reminders",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(stockChannel);
        }
    }
}
