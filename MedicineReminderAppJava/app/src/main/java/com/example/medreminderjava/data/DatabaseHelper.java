package com.example.medreminderjava.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "med_reminder.db";
    private static final int DATABASE_VERSION = 4;

    private static DatabaseHelper instance;
    private final Context mContext;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        android.util.Log.d("DatabaseHelper", "onCreate: Creating tables");
        String CREATE_DOCTORS_TABLE = "CREATE TABLE " + DbContract.DoctorEntry.TABLE_NAME + " ("
                + DbContract.DoctorEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.DoctorEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + DbContract.DoctorEntry.COLUMN_LOCATION + " TEXT, "
                + DbContract.DoctorEntry.COLUMN_CONTACT + " TEXT, "
                + DbContract.DoctorEntry.COLUMN_EMAIL + " TEXT"
                + ");";

        String CREATE_MEDICINES_TABLE = "CREATE TABLE " + DbContract.MedicineEntry.TABLE_NAME + " ("
                + DbContract.MedicineEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.MedicineEntry.COLUMN_DOCTOR_ID + " INTEGER NOT NULL, "
                + DbContract.MedicineEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY + " INTEGER DEFAULT 1, "
                + DbContract.MedicineEntry.COLUMN_TIMING_RELATION + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_TIMING_MEALS + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME + " INTEGER DEFAULT 1, "
                + DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_START_DATE + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_LUNCH_TIME + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_DINNER_TIME + " TEXT, "
                + "FOREIGN KEY(" + DbContract.MedicineEntry.COLUMN_DOCTOR_ID + ") REFERENCES "
                + DbContract.DoctorEntry.TABLE_NAME + "(" + DbContract.DoctorEntry._ID + ") ON DELETE CASCADE"
                + ");";

        String CREATE_USERS_TABLE = "CREATE TABLE " + DbContract.UserEntry.TABLE_NAME + " ("
                + DbContract.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.UserEntry.COLUMN_NAME + " TEXT, "
                + DbContract.UserEntry.COLUMN_LOCATION + " TEXT, "
                + DbContract.UserEntry.COLUMN_AGE + " TEXT, "
                + DbContract.UserEntry.COLUMN_BLOOD_GROUP + " TEXT, "
                + DbContract.UserEntry.COLUMN_MOBILE + " TEXT UNIQUE, "
                + DbContract.UserEntry.COLUMN_PASSWORD + " TEXT, "
                + DbContract.UserEntry.COLUMN_ALT_MOBILE + " TEXT"
                + ");";

        db.execSQL(CREATE_DOCTORS_TABLE);
        db.execSQL(CREATE_MEDICINES_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
/*
        // Prepopulate with mock doctors with hospital details
        db.execSQL("INSERT INTO " + DbContract.DoctorEntry.TABLE_NAME + " (name, location, contact, email) VALUES " +
                "('Dr. Deshmukh', 'City Hospital, Mumbai', '022-24567890', 'deshmukh@hospital.com')");
        db.execSQL("INSERT INTO " + DbContract.DoctorEntry.TABLE_NAME + " (name, location, contact, email) VALUES " +
                "('Dr. Pawar', 'LifeCare Clinic, Pune', '020-25671234', 'pawar@clinic.in')");

 */
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        android.util.Log.d("DatabaseHelper", "onUpgrade: from " + oldVersion + " to " + newVersion);
        // Force logout to prevent session mismatch if data is wiped
        mContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().apply();

        db.execSQL("DROP TABLE IF EXISTS " + DbContract.UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.MedicineEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.DoctorEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- DOCTOR CRUD ---

    public long addDoctor(String name, String location, String contact, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.DoctorEntry.COLUMN_NAME, name);
        values.put(DbContract.DoctorEntry.COLUMN_LOCATION, location);
        values.put(DbContract.DoctorEntry.COLUMN_CONTACT, contact);
        values.put(DbContract.DoctorEntry.COLUMN_EMAIL, email);
        return db.insert(DbContract.DoctorEntry.TABLE_NAME, null, values);
    }

    public int updateDoctor(long id, String name, String location, String contact, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.DoctorEntry.COLUMN_NAME, name);
        values.put(DbContract.DoctorEntry.COLUMN_LOCATION, location);
        values.put(DbContract.DoctorEntry.COLUMN_CONTACT, contact);
        values.put(DbContract.DoctorEntry.COLUMN_EMAIL, email);
        return db.update(DbContract.DoctorEntry.TABLE_NAME, values,
                DbContract.DoctorEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Doctor> getAllDoctors() {
        List<Doctor> doctors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + DbContract.DoctorEntry.TABLE_NAME + " ORDER BY " + DbContract.DoctorEntry._ID + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_NAME));
                String location = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_LOCATION));
                String contact = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_CONTACT));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_EMAIL));
                doctors.add(new Doctor(id, name, location, contact, email));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return doctors;
    }

    public Doctor getDoctorById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DbContract.DoctorEntry.TABLE_NAME,
                null, DbContract.DoctorEntry._ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        
        Doctor doctor = null;
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_NAME));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_LOCATION));
            String contact = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_CONTACT));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_EMAIL));
            doctor = new Doctor(id, name, location, contact, email);
            cursor.close();
        }
        return doctor;
    }

    public void deleteDoctor(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DbContract.DoctorEntry.TABLE_NAME, DbContract.DoctorEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    // User Operations
    public long registerUser(String name, String location, String age, String bloodGroup, String mobile, String password, String altMobile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.UserEntry.COLUMN_NAME, name);
        values.put(DbContract.UserEntry.COLUMN_LOCATION, location);
        values.put(DbContract.UserEntry.COLUMN_AGE, age);
        values.put(DbContract.UserEntry.COLUMN_BLOOD_GROUP, bloodGroup);
        values.put(DbContract.UserEntry.COLUMN_MOBILE, mobile);
        values.put(DbContract.UserEntry.COLUMN_PASSWORD, password);
        values.put(DbContract.UserEntry.COLUMN_ALT_MOBILE, altMobile);

        android.util.Log.d("DatabaseHelper", "Registering user: " + mobile);
        long result = db.insert(DbContract.UserEntry.TABLE_NAME, null, values);
        android.util.Log.d("DatabaseHelper", "Registration result row ID: " + result);
        
        Cursor countCursor = db.rawQuery("SELECT count(*) FROM " + DbContract.UserEntry.TABLE_NAME, null);
        if (countCursor.moveToFirst()) {
            android.util.Log.d("DatabaseHelper", "Total users in DB: " + countCursor.getInt(0));
        }
        countCursor.close();

        return result;
    }

    public boolean checkUser(String mobile, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = DbContract.UserEntry.COLUMN_MOBILE + " = ? AND " + DbContract.UserEntry.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {mobile, password};
        android.util.Log.d("DatabaseHelper", "Checking user: " + mobile + " / " + password);
        Cursor cursor = db.query(DbContract.UserEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
        int count = (cursor != null) ? cursor.getCount() : 0;
        android.util.Log.d("DatabaseHelper", "User found count: " + count);
        if (cursor != null) cursor.close();
        return count > 0;
    }

    public Cursor getUserByMobile(String mobile) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = DbContract.UserEntry.COLUMN_MOBILE + " = ?";
        String[] selectionArgs = {mobile};
        return db.query(DbContract.UserEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
    }

    public int updateUser(String name, String location, String age, String bloodGroup, String mobile, String altMobile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.UserEntry.COLUMN_NAME, name);
        values.put(DbContract.UserEntry.COLUMN_LOCATION, location);
        values.put(DbContract.UserEntry.COLUMN_AGE, age);
        values.put(DbContract.UserEntry.COLUMN_BLOOD_GROUP, bloodGroup);
        values.put(DbContract.UserEntry.COLUMN_ALT_MOBILE, altMobile);

        return db.update(DbContract.UserEntry.TABLE_NAME, values, 
                DbContract.UserEntry.COLUMN_MOBILE + " = ?", new String[]{mobile});
    }

    /**
     * Deletes all records from all tables. Use with caution!
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + DbContract.MedicineEntry.TABLE_NAME);
        db.execSQL("DELETE FROM " + DbContract.DoctorEntry.TABLE_NAME);
        db.execSQL("DELETE FROM " + DbContract.UserEntry.TABLE_NAME);
        // Clear session
        mContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().apply();
    }

    // --- MEDICINE CRUD ---

    public long addMedicine(long doctorId, String name, int timesPerDay, 
                           String timingRelation, String timingMeals, 
                           int totalQuantity, int dosagePerTime, int remainingQuantity,
                           String breakfastTime, String lunchTime, String dinnerTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.MedicineEntry.COLUMN_DOCTOR_ID, doctorId);
        values.put(DbContract.MedicineEntry.COLUMN_NAME, name);
        values.put(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY, timesPerDay);
        values.put(DbContract.MedicineEntry.COLUMN_TIMING_RELATION, timingRelation);
        values.put(DbContract.MedicineEntry.COLUMN_TIMING_MEALS, timingMeals);
        values.put(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY, totalQuantity);
        values.put(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME, dosagePerTime);
        values.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, remainingQuantity);
        values.put(DbContract.MedicineEntry.COLUMN_START_DATE, System.currentTimeMillis());
        values.put(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME, breakfastTime);
        values.put(DbContract.MedicineEntry.COLUMN_LUNCH_TIME, lunchTime);
        values.put(DbContract.MedicineEntry.COLUMN_DINNER_TIME, dinnerTime);

        return db.insert(DbContract.MedicineEntry.TABLE_NAME, null, values);
    }

    public int updateMedicine(long id, String name, int timesPerDay, 
                              String timingRelation, String timingMeals, 
                              int totalQuantity, int dosagePerTime, int remainingQuantity,
                              String breakfastTime, String lunchTime, String dinnerTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.MedicineEntry.COLUMN_NAME, name);
        values.put(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY, timesPerDay);
        values.put(DbContract.MedicineEntry.COLUMN_TIMING_RELATION, timingRelation);
        values.put(DbContract.MedicineEntry.COLUMN_TIMING_MEALS, timingMeals);
        values.put(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY, totalQuantity);
        values.put(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME, dosagePerTime);
        values.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, remainingQuantity);
        values.put(DbContract.MedicineEntry.COLUMN_START_DATE, System.currentTimeMillis());
        values.put(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME, breakfastTime);
        values.put(DbContract.MedicineEntry.COLUMN_LUNCH_TIME, lunchTime);
        values.put(DbContract.MedicineEntry.COLUMN_DINNER_TIME, dinnerTime);

        return db.update(DbContract.MedicineEntry.TABLE_NAME, values, 
                DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteMedicine(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DbContract.MedicineEntry.TABLE_NAME, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public Medicine getMedicineById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DbContract.MedicineEntry.TABLE_NAME,
                null, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        
        Medicine medicine = null;
        if (cursor != null && cursor.moveToFirst()) {
            medicine = parseMedicine(cursor);
            cursor.close();
        }
        return medicine;
    }

    public List<Medicine> getMedicinesForDoctor(long doctorId) {
        List<Medicine> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(DbContract.MedicineEntry.TABLE_NAME,
                null, DbContract.MedicineEntry.COLUMN_DOCTOR_ID + "=?",
                new String[]{String.valueOf(doctorId)}, null, null, DbContract.MedicineEntry._ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                list.add(parseMedicine(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Medicine> getAllMedicines() {
        List<Medicine> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbContract.MedicineEntry.TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(parseMedicine(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Deducts one single dosage quantity from remaining stock when user takes medicine.
     */
    public int deductMedicineDose(long id) {
        Medicine medicine = getMedicineById(id);
        if (medicine == null) return 0;

        int newRemaining = Math.max(0, medicine.getRemainingQuantity() - medicine.getDosagePerTime());
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, newRemaining);
        
        db.update(DbContract.MedicineEntry.TABLE_NAME, values, 
                DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
        
        return newRemaining;
    }

    /**
     * Finds the minimum remaining days among all medicines prescribed by a specific doctor.
     * Returns -1 if the doctor has no medicines.
     */
    public int getDoctorMinRemainingDays(long doctorId) {
        List<Medicine> medicines = getMedicinesForDoctor(doctorId);
        if (medicines.isEmpty()) {
            return -1;
        }

        int minDays = Integer.MAX_VALUE;
        for (Medicine med : medicines) {
            int days = med.getRemainingDays();
            if (days < minDays) {
                minDays = days;
            }
        }
        return minDays;
    }

    private Medicine parseMedicine(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry._ID));
        long docId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DOCTOR_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_NAME));
        int timesPerDay = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY));
        String timingRelation = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMING_RELATION));
        String timingMeals = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMING_MEALS));
        int totalQty = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY));
        int dosage = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME));
        int remaining = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY));
        long startDate = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_START_DATE));
        String bTime = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME));
        String lTime = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_LUNCH_TIME));
        String dTime = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DINNER_TIME));

        return new Medicine(id, docId, name, timesPerDay, timingRelation, timingMeals, totalQty, dosage, remaining, startDate, bTime, lTime, dTime);
    }
}
