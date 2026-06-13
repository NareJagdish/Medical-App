package com.example.medreminderjava.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.DbContract;
import com.example.medreminderjava.data.Medicine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    public static final String ACTION_ALARM_DOSE = "com.example.medreminderjava.ACTION_ALARM_DOSE";
    public static final String ACTION_ALARM_DAILY_CHECK = "com.example.medreminderjava.ACTION_ALARM_DAILY_CHECK";
    public static final String ACTION_TAKE = "com.example.medreminderjava.ACTION_TAKE";
    public static final String ACTION_SKIP = "com.example.medreminderjava.ACTION_SKIP";
    public static final String ACTION_SNOOZE = "com.example.medreminderjava.ACTION_SNOOZE";

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
            String timingInfo = intent.getStringExtra(EXTRA_TIMING_INFO);

            if (medicineId != -1) {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                Medicine m = dbHelper.getMedicineById(medicineId);
                if (m != null) {
                    if (!isDateInRange(m.getStartDate(), m.getEndDate())) {
                        Log.d(TAG, "Medicine " + m.getName() + " is not in date range. Skipping alarm.");
                        return;
                    }

                    // Launch full-screen reminder Activity directly
                    Intent alarmIntent = new Intent(context, com.example.medreminderjava.ReminderAlarmActivity.class);
                    alarmIntent.putExtra(EXTRA_MEDICINE_ID, medicineId);
                    alarmIntent.putExtra(EXTRA_MEDICINE_NAME, m.getName());
                    alarmIntent.putExtra(EXTRA_TIMING_INFO, timingInfo);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    
                    try {
                        context.startActivity(alarmIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start Activity from background", e);
                    }

                    // Send interactive notification (matching image request)
                    NotificationHelper.sendDoseNotification(context, medicineId, m.getName(), timingInfo);
                    sendSmsToAlternateNumber(context, m.getName(), timingInfo);

                    // Reschedule for next day (to maintain exact timing)
                    scheduleDoseAlarms(context, m);
                }
            }
        }
else if (ACTION_TAKE.equals(action)) {
            handleTakeAction(context, intent);
        } else if (ACTION_SKIP.equals(action)) {
            handleSkipAction(context, intent);
        } else if (ACTION_SNOOZE.equals(action)) {
            handleSnoozeAction(context, intent);
        } else if (ACTION_ALARM_DAILY_CHECK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            checkAllMedicinesAndNotify(context);
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                rescheduleAllAlarms(context);
            }
        }
    }

    private void handleTakeAction(Context context, Intent intent) {
        long medId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1);
        if (medId != -1) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            dbHelper.deductMedicineDose(medId);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.logMedicineIntake(medId, today, "Taken");
            NotificationHelper.cancelDoseNotification(context, medId);
            Log.d(TAG, "Notification ACTION_TAKE processed for medId: " + medId);
        }
    }

    private void handleSkipAction(Context context, Intent intent) {
        long medId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1);
        if (medId != -1) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.logMedicineIntake(medId, today, "Missed");
            NotificationHelper.cancelDoseNotification(context, medId);
            Log.d(TAG, "Notification ACTION_SKIP processed for medId: " + medId);
        }
    }

    private void handleSnoozeAction(Context context, Intent intent) {
        long medId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1);
        String name = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        String info = intent.getStringExtra(EXTRA_TIMING_INFO);
        
        if (medId != -1) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
            snoozeIntent.setAction(ACTION_ALARM_DOSE);
            snoozeIntent.putExtra(EXTRA_MEDICINE_ID, medId);
            snoozeIntent.putExtra(EXTRA_MEDICINE_NAME, name);
            snoozeIntent.putExtra(EXTRA_TIMING_INFO, info);

            PendingIntent pi = PendingIntent.getBroadcast(context, (int) medId + 5000, snoozeIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                }
            } catch (SecurityException e) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                Log.e(TAG, "Snooze failed, fallback to inexact alarm", e);
            }
            
            NotificationHelper.cancelDoseNotification(context, medId);
            Log.d(TAG, "Notification ACTION_SNOOZE processed for medId: " + medId);
        }
    }

    private boolean isDateInRange(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date start = sdf.parse(startDateStr);
            Date end = sdf.parse(endDateStr);
            Date today = sdf.parse(sdf.format(new Date()));

            if (start == null || end == null || today == null) return true;
            return !today.before(start) && !today.after(end);
        } catch (Exception e) {
            return true; // If parsing fails, allow alarm
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
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        if (loggedInMobile.isEmpty()) return;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines(loggedInMobile);

        // Auto-decrement for yesterday if no response was recorded for some or all doses
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        for (Medicine medicine : medicines) {
            // Count how many times the user interacted (Taken or Missed) yesterday
            int manualInteractions = dbHelper.getManualLogCount(medicine.getId(), yesterday);
            int scheduledTimes = medicine.getTimesPerDay();

            // If there are doses with no response, auto-decrement them
            if (manualInteractions < scheduledTimes) {
                int dosesToDeduct = scheduledTimes - manualInteractions;
                for (int i = 0; i < dosesToDeduct; i++) {
                    dbHelper.deductMedicineDose(medicine.getId());
                }
                // Log the auto-deduction event
                dbHelper.logMedicineIntake(medicine.getId(), yesterday, "Auto-Taken");
                Log.d(TAG, "Auto-decremented " + dosesToDeduct + " dose(s) for " + medicine.getName() + " for date: " + yesterday);
            }

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
        // Run right after midnight (00:01) to process "yesterday"
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);
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
                try {
                    String[] parts = timeStr.split(":");
                    hour = Integer.parseInt(parts[0]);

                    // Handle "00 AM" or "00 PM" in minute part
                    String minutePart = parts[1];
                    if (minutePart.contains(" ")) {
                        String[] minAmPm = minutePart.split(" ");
                        minute = Integer.parseInt(minAmPm[0]);
                        String amPm = minAmPm[1];
                        if ("PM".equalsIgnoreCase(amPm) && hour < 12) hour += 12;
                        if ("AM".equalsIgnoreCase(amPm) && hour == 12) hour = 0;
                    } else {
                        minute = Integer.parseInt(minutePart);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing time: " + timeStr, e);
                }
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
            intent.putExtra(EXTRA_TIMING_INFO, medicine.getTimingRelation()); // This now contains "1 [DoseUnit] ([PillType])"

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            try {
                // setAlarmClock is the most reliable way on all modern Android versions
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
                alarmManager.setAlarmClock(info, pendingIntent);
            } catch (SecurityException e) {
                // Fallback for some restricted devices or missing permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
                Log.e(TAG, "setAlarmClock failed, using fallback", e);
            }
            Log.d(TAG, "Scheduled next dose alarm for " + medicine.getName() + " at " + hour + ":" + minute + " (Meal: " + meal + ")");
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
        List<Medicine> medicines = dbHelper.getAllMedicinesUnfiltered();
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
