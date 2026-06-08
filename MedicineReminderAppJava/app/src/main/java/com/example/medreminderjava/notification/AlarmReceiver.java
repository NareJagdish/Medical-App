package com.example.medreminderjava.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.DbContract;
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

    public static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_LOGGED_IN_MOBILE = "logged_in_mobile";
    
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
                // Deduct dose automatically as requested by user
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                int remaining = dbHelper.deductMedicineDose(medicineId);
                Log.d(TAG, "Automatically deducted dose for " + medicineName + ". Remaining stock: " + remaining);

                NotificationHelper.sendDoseNotification(context, medicineId, medicineName, timingInfo);
                sendSmsToAlternateNumber(context, medicineName, timingInfo);
            }
        } else if (ACTION_ALARM_DAILY_CHECK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            checkAllMedicinesAndNotify(context);
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                rescheduleAllAlarms(context);
            }
        }
    }

    private void sendSmsToAlternateNumber(Context context, String medicineName, String timingInfo) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        
        if (loggedInMobile.isEmpty()) return;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        Cursor cursor = dbHelper.getUserByMobile(loggedInMobile);
        if (cursor != null && cursor.moveToFirst()) {
            String altMobile = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.UserEntry.COLUMN_ALT_MOBILE));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.UserEntry.COLUMN_NAME));
            
            if (altMobile != null && !altMobile.isEmpty()) {
                String message = "Medicine Reminder for " + name + ": It's time to take " + medicineName + " (" + timingInfo + ").";
                NotificationHelper.sendSmsNotification(context, altMobile, message);
                Log.d(TAG, "SMS reminder sent to " + altMobile);
            }
        }
        if (cursor != null) cursor.close();
    }

    public static void checkAllMedicinesAndNotify(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines();

        for (Medicine medicine : medicines) {
            int remainingDays = medicine.getRemainingDays();
            if (remainingDays == 10 || remainingDays == 5 || remainingDays == 2 || remainingDays == 0) {
                NotificationHelper.sendStockNotification(context, medicine.getId(), medicine.getName(), remainingDays);
            }
        }
    }

    public static void scheduleDailyCheck(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_DAILY_CHECK);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public static void scheduleDoseAlarms(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String meals = medicine.getTimingMeals();
        String relation = medicine.getTimingRelation();

        if (meals == null || meals.trim().isEmpty()) return;

        String[] mealArray = meals.split(",");
        for (String meal : mealArray) {
            meal = meal.trim();
            int hour = 8, minute = 0;

            String timeStr = null;
            if ("Breakfast".equalsIgnoreCase(meal)) timeStr = medicine.getBreakfastTime();
            else if ("Lunch".equalsIgnoreCase(meal)) timeStr = medicine.getLunchTime();
            else if ("Dinner".equalsIgnoreCase(meal)) timeStr = medicine.getDinnerTime();

            if (timeStr != null && timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } else {
                // Fallback defaults if no time set in medicine object
                if ("Breakfast".equalsIgnoreCase(meal)) { hour = 8; minute = 0; }
                else if ("Lunch".equalsIgnoreCase(meal)) { hour = 13; minute = 30; }
                else if ("Dinner".equalsIgnoreCase(meal)) { hour = 20; minute = 30; }
            }

            int requestCode = getUniqueRequestCode(medicine.getId(), meal);

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_ALARM_DOSE);
            intent.putExtra(EXTRA_MEDICINE_ID, medicine.getId());
            intent.putExtra(EXTRA_MEDICINE_NAME, medicine.getName());
            intent.putExtra(EXTRA_TIMING_INFO, relation + " " + meal);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            Log.d(TAG, "Scheduled dose alarm for " + medicine.getName() + " at " + hour + ":" + minute + " (Meal: " + meal + ")");
        }
    }

    public static void cancelAlarmsForMedicine(Context context, Medicine medicine) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String[] meals = new String[]{"Breakfast", "Lunch", "Dinner"};
        for (String meal : meals) {
            int requestCode = getUniqueRequestCode(medicine.getId(), meal);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_ALARM_DOSE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    public static void rescheduleAllAlarms(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines();
        for (Medicine medicine : medicines) {
            scheduleDoseAlarms(context, medicine);
        }
        scheduleDailyCheck(context);
    }

    private static int getUniqueRequestCode(long medicineId, String mealName) {
        int mealHash = mealName.hashCode();
        return (int) (medicineId * 31 + Math.abs(mealHash % 1000));
    }
}
