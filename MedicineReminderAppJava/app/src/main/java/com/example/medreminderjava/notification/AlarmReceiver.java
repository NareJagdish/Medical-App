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
import java.util.List;
import java.util.Locale;

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

        if (ACTION_ALARM_DOSE.equals(action)) {
            long medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1);
            String timingInfo = intent.getStringExtra(EXTRA_TIMING_INFO);

            if (medicineId != -1) {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                Medicine m = dbHelper.getMedicineById(medicineId);
                if (m != null) {
                    // Launch full-screen reminder Activity directly
                    Intent alarmIntent = new Intent(context, com.example.medreminderjava.ReminderAlarmActivity.class);
                    alarmIntent.putExtra(EXTRA_MEDICINE_ID, medicineId);
                    alarmIntent.putExtra(EXTRA_MEDICINE_NAME, m.getName());
                    alarmIntent.putExtra(EXTRA_TIMING_INFO, timingInfo);
                    alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    
                    try {
                        context.startActivity(alarmIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start Activity directly", e);
                    }

                    // Send interactive notification with full-screen intent (Handled in NotificationHelper)
                    NotificationHelper.sendFullScreenAlarmNotification(context, medicineId, m.getName(), timingInfo);
                    
                    // Reschedule for next day
                    scheduleDoseAlarms(context, m);
                }
            }
        } else if (ACTION_ALARM_DAILY_CHECK.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            checkAllMedicinesAndNotify(context);
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                rescheduleAllAlarms(context);
            }
        }
    }

    public static void checkAllMedicinesAndNotify(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        if (loggedInMobile.isEmpty()) return;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        List<Medicine> medicines = dbHelper.getAllMedicines(loggedInMobile);

        String primaryMobile = loggedInMobile;
        String altMobile = "";
        Cursor cursor = dbHelper.getUserByMobile(primaryMobile);
        if (cursor != null && cursor.moveToFirst()) {
            altMobile = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.UserEntry.COLUMN_ALT_MOBILE));
            cursor.close();
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        for (Medicine medicine : medicines) {
            int totalInteractions = dbHelper.getInteractionCount(medicine.getId(), yesterday);
            int scheduledTimes = medicine.getTimesPerDay();

            if (totalInteractions < scheduledTimes) {
                int dosesToDeduct = scheduledTimes - totalInteractions;
                for (int i = 0; i < dosesToDeduct; i++) {
                    dbHelper.deductMedicineDose(medicine.getId());
                    dbHelper.logMedicineIntake(medicine.getId(), yesterday, "Auto-Taken");
                }
            }

            int remainingDays = medicine.getRemainingDays();
            if (remainingDays == 10 || remainingDays == 5 || remainingDays == 2 || remainingDays == 1) {
                String smsMessage = "Medicine Stock Alert: Your medicine '" + medicine.getName() + 
                                   "' will be over in " + remainingDays + " day(s). Please restock it.";
                
                NotificationHelper.sendSmsNotification(context, primaryMobile, smsMessage);
                if (altMobile != null && !altMobile.isEmpty()) {
                    NotificationHelper.sendSmsNotification(context, altMobile, smsMessage);
                }
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
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
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
            }

            int requestCode = getUniqueRequestCode(medicine.getId(), meal);

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ACTION_ALARM_DOSE);
            intent.putExtra(EXTRA_MEDICINE_ID, medicine.getId());
            intent.putExtra(EXTRA_MEDICINE_NAME, medicine.getName());
            intent.putExtra(EXTRA_TIMING_INFO, medicine.getTimingRelation());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            try {
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent);
                alarmManager.setAlarmClock(info, pendingIntent);
            } catch (SecurityException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
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
