package com.example.medreminderjava.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.medreminderjava.MainActivity;
import com.example.medreminderjava.R;

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
     * Uses setFullScreenIntent and adds interactive buttons: Skip, Snooze, Take.
     */
    public static void sendDoseNotification(Context context, long medicineId, String medicineName, String timingInfo) {
        // Fetch User Name for personalized message
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String mobile = prefs.getString("logged_in_mobile", "");
        String userName = "there";
        if (!mobile.isEmpty()) {
            com.example.medreminderjava.data.DatabaseHelper dbHelper = com.example.medreminderjava.data.DatabaseHelper.getInstance(context);
            android.database.Cursor cursor = dbHelper.getUserByMobile(mobile);
            if (cursor != null && cursor.moveToFirst()) {
                userName = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_NAME));
                cursor.close();
            }
        }

        Intent fullScreenIntent = new Intent(context, com.example.medreminderjava.ReminderAlarmActivity.class);
        fullScreenIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId);
        fullScreenIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName);
        fullScreenIntent.putExtra(AlarmReceiver.EXTRA_TIMING_INFO, timingInfo);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, 
                (int) medicineId, 
                fullScreenIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Action: Take
        Intent takeIntent = new Intent(context, AlarmReceiver.class);
        takeIntent.setAction(AlarmReceiver.ACTION_TAKE);
        takeIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId);
        PendingIntent takePendingIntent = PendingIntent.getBroadcast(context, (int) medicineId + 10, takeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: Skip
        Intent skipIntent = new Intent(context, AlarmReceiver.class);
        skipIntent.setAction(AlarmReceiver.ACTION_SKIP);
        skipIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId);
        PendingIntent skipPendingIntent = PendingIntent.getBroadcast(context, (int) medicineId + 20, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action: Snooze
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(AlarmReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_ID, medicineId);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName);
        snoozeIntent.putExtra(AlarmReceiver.EXTRA_TIMING_INFO, timingInfo);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, (int) medicineId + 30, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DOSE_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
                .setContentTitle("Take Your Pills")
                .setContentText("Hello " + userName + ", it's time to take " + medicineName)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle("Take Your Pills")
                        .bigText("Hello " + userName + ", it's time to take " + medicineName + "\n" + timingInfo))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(0, "Skip", skipPendingIntent)
                .addAction(0, "Snooze", snoozePendingIntent)
                .addAction(0, "Take", takePendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(DOSE_NOTIFICATION_ID_OFFSET + (int) medicineId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void cancelDoseNotification(Context context, long medicineId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(DOSE_NOTIFICATION_ID_OFFSET + (int) medicineId);
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

    /**
     * Sends an SMS notification to the alternate mobile number.
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
}
