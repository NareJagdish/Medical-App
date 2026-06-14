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
    private static final int DATABASE_VERSION = 10;

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
                + DbContract.DoctorEntry.COLUMN_USER_MOBILE + " TEXT NOT NULL, "
                + DbContract.DoctorEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + DbContract.DoctorEntry.COLUMN_LOCATION + " TEXT, "
                + DbContract.DoctorEntry.COLUMN_CONTACT + " TEXT, "
                + DbContract.DoctorEntry.COLUMN_EMAIL + " TEXT"
                + ");";

        String CREATE_MEDICINES_TABLE = "CREATE TABLE " + DbContract.MedicineEntry.TABLE_NAME + " ("
                + DbContract.MedicineEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.MedicineEntry.COLUMN_USER_MOBILE + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_DOCTOR_ID + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY + " INTEGER DEFAULT 1, "
                + DbContract.MedicineEntry.COLUMN_TIMING_RELATION + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_TIMING_MEALS + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME + " INTEGER DEFAULT 1, "
                + DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY + " INTEGER, "
                + DbContract.MedicineEntry.COLUMN_START_DATE + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_END_DATE + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_LUNCH_TIME + " TEXT, "
                + DbContract.MedicineEntry.COLUMN_DINNER_TIME + " TEXT"
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

        String CREATE_LOGS_TABLE = "CREATE TABLE " + DbContract.MedicineLogEntry.TABLE_NAME + " ("
                + DbContract.MedicineLogEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.MedicineLogEntry.COLUMN_MEDICINE_ID + " INTEGER, "
                + DbContract.MedicineLogEntry.COLUMN_DATE + " TEXT, "
                + DbContract.MedicineLogEntry.COLUMN_STATUS + " TEXT"
                + ");";

        String CREATE_APPOINTMENTS_TABLE = "CREATE TABLE " + DbContract.AppointmentEntry.TABLE_NAME + " ("
                + DbContract.AppointmentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbContract.AppointmentEntry.COLUMN_USER_MOBILE + " TEXT, "
                + DbContract.AppointmentEntry.COLUMN_DOCTOR_ID + " INTEGER, "
                + DbContract.AppointmentEntry.COLUMN_DATE + " TEXT, "
                + DbContract.AppointmentEntry.COLUMN_TIME + " TEXT, "
                + DbContract.AppointmentEntry.COLUMN_REASON + " TEXT, "
                + DbContract.AppointmentEntry.COLUMN_PRIORITY + " TEXT"
                + ");";

        db.execSQL(CREATE_DOCTORS_TABLE);
        db.execSQL(CREATE_MEDICINES_TABLE);
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_LOGS_TABLE);
        db.execSQL(CREATE_APPOINTMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        mContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.MedicineEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.DoctorEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.MedicineLogEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS prescriptions");
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.AppointmentEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- DOCTOR CRUD ---

    public long addDoctor(String userMobile, String name, String location, String contact, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbContract.DoctorEntry.COLUMN_USER_MOBILE, userMobile);
        values.put(DbContract.DoctorEntry.COLUMN_NAME, name);
        values.put(DbContract.DoctorEntry.COLUMN_LOCATION, location);
        values.put(DbContract.DoctorEntry.COLUMN_CONTACT, contact);
        values.put(DbContract.DoctorEntry.COLUMN_EMAIL, email);
        return db.insert(DbContract.DoctorEntry.TABLE_NAME, null, values);
    }

    public List<Doctor> getAllDoctors(String userMobile) {
        List<Doctor> doctors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = DbContract.DoctorEntry.COLUMN_USER_MOBILE + " = ?";
        String[] selectionArgs = {userMobile};
        Cursor cursor = db.query(DbContract.DoctorEntry.TABLE_NAME, null, selection, selectionArgs, null, null, DbContract.DoctorEntry._ID + " ASC");

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
        Cursor cursor = db.query(DbContract.DoctorEntry.TABLE_NAME, null, DbContract.DoctorEntry._ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        Doctor doctor = null;
        if (cursor != null && cursor.moveToFirst()) {
            doctor = new Doctor(id, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_LOCATION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_CONTACT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DoctorEntry.COLUMN_EMAIL)));
            cursor.close();
        }
        return doctor;
    }

    public void deleteDoctor(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete all medicines prescribed by this doctor
        db.delete(DbContract.MedicineEntry.TABLE_NAME, DbContract.MedicineEntry.COLUMN_DOCTOR_ID + "=?", new String[]{String.valueOf(id)});
        // Delete the doctor record
        db.delete(DbContract.DoctorEntry.TABLE_NAME, DbContract.DoctorEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    // --- USER CRUD ---

    public long registerUser(String name, String location, String age, String bloodGroup, String mobile, String password, String altMobile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DbContract.UserEntry.COLUMN_NAME, name);
        v.put(DbContract.UserEntry.COLUMN_LOCATION, location);
        v.put(DbContract.UserEntry.COLUMN_AGE, age);
        v.put(DbContract.UserEntry.COLUMN_BLOOD_GROUP, bloodGroup);
        v.put(DbContract.UserEntry.COLUMN_MOBILE, mobile);
        v.put(DbContract.UserEntry.COLUMN_PASSWORD, password);
        v.put(DbContract.UserEntry.COLUMN_ALT_MOBILE, altMobile);
        return db.insert(DbContract.UserEntry.TABLE_NAME, null, v);
    }

    public boolean checkUser(String mobile, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(DbContract.UserEntry.TABLE_NAME, null, DbContract.UserEntry.COLUMN_MOBILE + "=? AND " + DbContract.UserEntry.COLUMN_PASSWORD + "=?", new String[]{mobile, password}, null, null, null);
        boolean exists = (c != null && c.getCount() > 0);
        if (c != null) c.close();
        return exists;
    }

    public Cursor getUserByMobile(String mobile) {
        return getReadableDatabase().query(DbContract.UserEntry.TABLE_NAME, null, DbContract.UserEntry.COLUMN_MOBILE + "=?", new String[]{mobile}, null, null, null);
    }

    public int updateUser(String name, String loc, String age, String bg, String mobile, String alt) {
        ContentValues v = new ContentValues();
        v.put(DbContract.UserEntry.COLUMN_NAME, name);
        v.put(DbContract.UserEntry.COLUMN_LOCATION, loc);
        v.put(DbContract.UserEntry.COLUMN_AGE, age);
        v.put(DbContract.UserEntry.COLUMN_BLOOD_GROUP, bg);
        v.put(DbContract.UserEntry.COLUMN_ALT_MOBILE, alt);
        return getWritableDatabase().update(DbContract.UserEntry.TABLE_NAME, v, DbContract.UserEntry.COLUMN_MOBILE + "=?", new String[]{mobile});
    }

    // --- MEDICINE CRUD ---

    public long addMedicine(String userMobile, long docId, String name, int times, String rel, String meals, int total, int dosage, int rem, String start, String end, String b, String l, String d) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DbContract.MedicineEntry.COLUMN_USER_MOBILE, userMobile);
        if (docId != -1) v.put(DbContract.MedicineEntry.COLUMN_DOCTOR_ID, docId);
        v.put(DbContract.MedicineEntry.COLUMN_NAME, name);
        v.put(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY, times);
        v.put(DbContract.MedicineEntry.COLUMN_TIMING_RELATION, rel);
        v.put(DbContract.MedicineEntry.COLUMN_TIMING_MEALS, meals);
        v.put(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY, total);
        v.put(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME, dosage);
        v.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, rem);
        v.put(DbContract.MedicineEntry.COLUMN_START_DATE, start);
        v.put(DbContract.MedicineEntry.COLUMN_END_DATE, end);
        v.put(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME, b);
        v.put(DbContract.MedicineEntry.COLUMN_LUNCH_TIME, l);
        v.put(DbContract.MedicineEntry.COLUMN_DINNER_TIME, d);
        return db.insert(DbContract.MedicineEntry.TABLE_NAME, null, v);
    }

    public int updateMedicine(long id, long docId, String name, int times, String rel, String meals, int total, int dosage, int rem, String start, String end, String b, String l, String d) {
        ContentValues v = new ContentValues();
        if (docId != -1) v.put(DbContract.MedicineEntry.COLUMN_DOCTOR_ID, docId);
        else v.putNull(DbContract.MedicineEntry.COLUMN_DOCTOR_ID);
        v.put(DbContract.MedicineEntry.COLUMN_NAME, name);
        v.put(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY, times);
        v.put(DbContract.MedicineEntry.COLUMN_TIMING_RELATION, rel);
        v.put(DbContract.MedicineEntry.COLUMN_TIMING_MEALS, meals);
        v.put(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY, total);
        v.put(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME, dosage);
        v.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, rem);
        v.put(DbContract.MedicineEntry.COLUMN_START_DATE, start);
        v.put(DbContract.MedicineEntry.COLUMN_END_DATE, end);
        v.put(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME, b);
        v.put(DbContract.MedicineEntry.COLUMN_LUNCH_TIME, l);
        v.put(DbContract.MedicineEntry.COLUMN_DINNER_TIME, d);
        return getWritableDatabase().update(DbContract.MedicineEntry.TABLE_NAME, v, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteMedicine(long id) {
        getWritableDatabase().delete(DbContract.MedicineEntry.TABLE_NAME, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public Medicine getMedicineById(long id) {
        Cursor c = getReadableDatabase().query(DbContract.MedicineEntry.TABLE_NAME, null, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        Medicine m = null; if (c != null && c.moveToFirst()) { m = parseMedicine(c); c.close(); } return m;
    }

    public List<Medicine> getMedicinesForDoctor(long doctorId) {
        List<Medicine> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(DbContract.MedicineEntry.TABLE_NAME, null, DbContract.MedicineEntry.COLUMN_DOCTOR_ID + "=?", new String[]{String.valueOf(doctorId)}, null, null, DbContract.MedicineEntry._ID + " ASC");
        if (c.moveToFirst()) { do { list.add(parseMedicine(c)); } while (c.moveToNext()); }
        c.close(); return list;
    }

    public List<Medicine> getAllMedicines(String userMobile) {
        List<Medicine> list = new ArrayList<>();
        String query = "SELECT * FROM " + DbContract.MedicineEntry.TABLE_NAME + " WHERE " + DbContract.MedicineEntry.COLUMN_USER_MOBILE + "=?";
        Cursor c = getReadableDatabase().rawQuery(query, new String[]{userMobile});
        if (c.moveToFirst()) { do { list.add(parseMedicine(c)); } while (c.moveToNext()); }
        c.close(); return list;
    }

    public List<Medicine> getAllMedicinesUnfiltered() {
        List<Medicine> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + DbContract.MedicineEntry.TABLE_NAME, null);
        if (c.moveToFirst()) { do { list.add(parseMedicine(c)); } while (c.moveToNext()); }
        c.close(); return list;
    }

    public int deductMedicineDose(long id) {
        Medicine m = getMedicineById(id);
        if (m == null) return 0;
        int rem = Math.max(0, m.getRemainingQuantity() - m.getDosagePerTime());
        ContentValues v = new ContentValues(); v.put(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY, rem);
        getWritableDatabase().update(DbContract.MedicineEntry.TABLE_NAME, v, DbContract.MedicineEntry._ID + "=?", new String[]{String.valueOf(id)});
        return rem;
    }

    public int getDoctorMinRemainingDays(long doctorId) {
        List<Medicine> medicines = getMedicinesForDoctor(doctorId);
        if (medicines.isEmpty()) return -1;
        int min = Integer.MAX_VALUE;
        for (Medicine med : medicines) { int d = med.getRemainingDays(); if (d < min) min = d; }
        return min;
    }

    // --- LOGS CRUD ---

    public void logMedicineIntake(long medId, String date, String status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(DbContract.MedicineLogEntry.COLUMN_MEDICINE_ID, medId);
        v.put(DbContract.MedicineLogEntry.COLUMN_DATE, date);
        v.put(DbContract.MedicineLogEntry.COLUMN_STATUS, status);
        db.insert(DbContract.MedicineLogEntry.TABLE_NAME, null, v);
    }

    public int getInteractionCount(long medId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DbContract.MedicineLogEntry.TABLE_NAME +
                " WHERE " + DbContract.MedicineLogEntry.COLUMN_MEDICINE_ID + "=? AND " + DbContract.MedicineLogEntry.COLUMN_DATE + "=?" +
                " AND " + DbContract.MedicineLogEntry.COLUMN_STATUS + " IN ('Taken', 'Missed', 'Auto-Taken')";
        Cursor c = db.rawQuery(query, new String[]{String.valueOf(medId), date});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public String getLogStatus(long medId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + DbContract.MedicineLogEntry.COLUMN_STATUS + " FROM " + DbContract.MedicineLogEntry.TABLE_NAME +
                " WHERE " + DbContract.MedicineLogEntry.COLUMN_MEDICINE_ID + "=? AND " + DbContract.MedicineLogEntry.COLUMN_DATE + "=?";
        Cursor c = db.rawQuery(query, new String[]{String.valueOf(medId), date});
        String status = "";
        if (c.moveToFirst()) status = c.getString(0);
        c.close();
        return status;
    }

    public int getLogCount(String userMobile, String status) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DbContract.MedicineLogEntry.TABLE_NAME + " l " +
                "INNER JOIN " + DbContract.MedicineEntry.TABLE_NAME + " m ON l." + DbContract.MedicineLogEntry.COLUMN_MEDICINE_ID + "=m." + DbContract.MedicineEntry._ID + " " +
                "LEFT JOIN " + DbContract.DoctorEntry.TABLE_NAME + " d ON m." + DbContract.MedicineEntry.COLUMN_DOCTOR_ID + "=d." + DbContract.DoctorEntry._ID + " " +
                "WHERE (d." + DbContract.DoctorEntry.COLUMN_USER_MOBILE + "=? OR m." + DbContract.MedicineEntry.COLUMN_DOCTOR_ID + " IS NULL) AND l." + DbContract.MedicineLogEntry.COLUMN_STATUS + "=?";
        Cursor c = db.rawQuery(query, new String[]{userMobile, status});
        int count = 0; if (c.moveToFirst()) count = c.getInt(0);
        c.close(); return count;
    }

    public void addAppointment(String mobile, long docId, String date, String time, String reason, String priority) {
        ContentValues v = new ContentValues();
        v.put(DbContract.AppointmentEntry.COLUMN_USER_MOBILE, mobile);
        v.put(DbContract.AppointmentEntry.COLUMN_DOCTOR_ID, docId);
        v.put(DbContract.AppointmentEntry.COLUMN_DATE, date);
        v.put(DbContract.AppointmentEntry.COLUMN_TIME, time);
        v.put(DbContract.AppointmentEntry.COLUMN_REASON, reason);
        v.put(DbContract.AppointmentEntry.COLUMN_PRIORITY, priority);
        getWritableDatabase().insert(DbContract.AppointmentEntry.TABLE_NAME, null, v);
    }

    public void deleteAppointment(long id) {
        getWritableDatabase().delete(DbContract.AppointmentEntry.TABLE_NAME, DbContract.AppointmentEntry._ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Appointment> getAllAppointments(String mobile) {
        List<Appointment> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DbContract.AppointmentEntry.TABLE_NAME + " WHERE " + DbContract.AppointmentEntry.COLUMN_USER_MOBILE + "=? ORDER BY " + DbContract.AppointmentEntry.COLUMN_DATE + " ASC", new String[]{mobile});
        if (c.moveToFirst()) {
            do {
                list.add(new Appointment(
                        c.getLong(c.getColumnIndexOrThrow(DbContract.AppointmentEntry._ID)),
                        c.getString(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_USER_MOBILE)),
                        c.getLong(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_DOCTOR_ID)),
                        c.getString(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_DATE)),
                        c.getString(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_TIME)),
                        c.getString(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_REASON)),
                        c.getString(c.getColumnIndexOrThrow(DbContract.AppointmentEntry.COLUMN_PRIORITY))
                ));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public int getUpcomingAppointmentCount(String mobile) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbContract.AppointmentEntry.TABLE_NAME + " WHERE " + DbContract.AppointmentEntry.COLUMN_USER_MOBILE + "=?", new String[]{mobile});
        int count = 0; if (c.moveToFirst()) count = c.getInt(0);
        c.close(); return count;
    }

    private Medicine parseMedicine(Cursor c) {
        return new Medicine(c.getLong(c.getColumnIndexOrThrow(DbContract.MedicineEntry._ID)),
                c.getLong(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DOCTOR_ID)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_NAME)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMES_PER_DAY)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMING_RELATION)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TIMING_MEALS)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_TOTAL_QUANTITY)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DOSAGE_PER_TIME)),
                c.getInt(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_REMAINING_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_START_DATE)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_END_DATE)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_BREAKFAST_TIME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_LUNCH_TIME)),
                c.getString(c.getColumnIndexOrThrow(DbContract.MedicineEntry.COLUMN_DINNER_TIME)));
    }
}
