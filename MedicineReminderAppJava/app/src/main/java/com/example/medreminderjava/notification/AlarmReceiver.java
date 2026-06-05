package com.example.medreminderjava.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;

import java.util.Calendar;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    public static final String ACTION_ALARM_DOSE = "com.example.medreminderjava.ACTION_ALARM_DOSE";
    public static final String ACTION_ALARM_DAILY_CHECK = "com.example.medreminderjava.ACTION_ALARM_DAILY_CHECK";

    public static final String EXTRA_MEDICINE_ID = "extra_medicine_id";
    public static final String EXTRA_MEDICINE_NAME = "extra_medicine_name";
    public static final String EXTRA_TIMING_INFO = "extra_timing_info";

    // Approximate hour schedules for meals
    private static final int HOUR_BREAKFAST = 8;
    private static final int MINUTE_BREAKFAST = 0;

    private static final int HOUR_LUNCH = 13;
    private static final int MINUTE_LUNCH = 30;

    private static final int HOUR_DINNER = 20;
    private static final int MINUTE_DINNER = 30;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: action = " + action);

        if (action == null) return;

        NotificationHelper.createNotificationChannels(context);

        if (ACTION_ALARM_DOSE.equals(action)) {
            long medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1);
            String medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
            String timingInfo = intent.getStringExtra(EXTRA_TIMING_INFO);

            if (medicineId != -1 && medicineName != null) {
                NotificationHelper.sendDoseNotification(context, medicineId, medicineName, timingInfo);
            }
        } else if (ACTION_ALARM_DAILY_CHECK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Perform remaining days notification check
            checkAllMedicinesAndNotify(context);

            // If boot completed, reschedule all exact dose alarms
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                rescheduleAllAlarms(context);
            }
        }
    }

    /**
     * Scans database and triggers notifications if medicine is running low (10, 5, 2, or 0 days remaining).
     */
    public static void checkAllMedicinesAndNotify(Context context) {
        Log.d(TAG, "checkAllMedicinesAndNotify scanning database...");
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines();

        for (Medicine medicine : medicines) {
            int remainingDays = medicine.getRemainingDays();
            Log.d(TAG, "Medicine '" + medicine.getName() + "': remainingDays = " + remainingDays);

            // Check if days remaining matches any target thresholds
            if (remainingDays == 10 || remainingDays == 5 || remainingDays == 2 || remainingDays == 0) {
                NotificationHelper.sendStockNotification(context, medicine.getId(), medicine.getName(), remainingDays);
            }
        }
    }

    /**
     * Schedules the daily stock alert check.
     */
    public static void scheduleDailyCheck(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_DAILY_CHECK);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                999, // Unique request code for daily check
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9); // Run check at 9:00 AM every day
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If time already passed today, set for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
        Log.d(TAG, "Daily inventory check alarm scheduled.");
    }

    /**
     * Registers AlarmManager alarms for a specific medicine based on meal options.
     */
    public static void scheduleDoseAlarms(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String meals = medicine.getTimingMeals(); // e.g. "Breakfast,Dinner"
        String relation = medicine.getTimingRelation(); // e.g. "Before"

        if (meals == null || meals.trim().isEmpty()) return;

        String[] mealArray = meals.split(",");
        for (String meal : mealArray) {
            meal = meal.trim();
            int hour = 8, minute = 0;

            if ("Breakfast".equalsIgnoreCase(meal)) {
                hour = HOUR_BREAKFAST;
                minute = MINUTE_BREAKFAST;
            } else if ("Lunch".equalsIgnoreCase(meal)) {
                hour = HOUR_LUNCH;
                minute = MINUTE_LUNCH;
            } else if ("Dinner".equalsIgnoreCase(meal)) {
                hour = HOUR_DINNER;
                minute = MINUTE_DINNER;
            }

            int requestCode = getUniqueRequestCode(medicine.getId(), meal);

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_ALARM_DOSE);
            intent.putExtra(EXTRA_MEDICINE_ID, medicine.getId());
            intent.putExtra(EXTRA_MEDICINE_NAME, medicine.getName());
            intent.putExtra(EXTRA_TIMING_INFO, relation + " " + meal);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // If scheduled time already passed today, set for tomorrow
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Set exact repeating alarm
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
            Log.d(TAG, "Scheduled dose alarm for " + medicine.getName() + " at " + hour + ":" + minute + " (Meal: " + meal + ")");
        }
    }

    /**
     * Cancels any scheduled alarms for a specific medicine.
     */
    public static void cancelAlarmsForMedicine(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] meals = new String[]{"Breakfast", "Lunch", "Dinner"};
        for (String meal : meals) {
            int requestCode = getUniqueRequestCode(medicine.getId(), meal);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_ALARM_DOSE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "Cancelled alarm for medicine " + medicine.getName() + " meal: " + meal);
            }
        }
    }

    /**
     * Reschedules all alarms for all medicines in the database (typically on device reboot).
     */
    public static void rescheduleAllAlarms(Context context) {
        Log.d(TAG, "Rescheduling all alarms from database...");
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines();
        for (Medicine medicine : medicines) {
            scheduleDoseAlarms(context, medicine);
        }
        scheduleDailyCheck(context);
    }

    /**
     * Generates a unique integer code for PendingIntent request codes.
     */
    private static int getUniqueRequestCode(long medicineId, String mealName) {
        int mealHash = mealName.hashCode();
        return (int) (medicineId * 31 + Math.abs(mealHash % 1000));
    }
}
