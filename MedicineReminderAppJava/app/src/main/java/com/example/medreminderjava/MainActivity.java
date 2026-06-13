package com.example.medreminderjava;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.notification.NotificationHelper;
import com.example.medreminderjava.ui.DoctorAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements DoctorAdapter.OnDoctorClickListener {

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;

    // UI elements
    private TextView tvUserName, tvUserAge, tvUserId, tvUserBloodGroup;
    private ImageView ivProfileImage;
    private RecyclerView rvDoctors;
    private BottomNavigationView bottomNavigation;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_LOGGED_IN_MOBILE = "logged_in_mobile";
    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        NotificationHelper.createNotificationChannels(this);
        AlarmReceiver.scheduleDailyCheck(this);
        AlarmReceiver.rescheduleAllAlarms(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI
        tvUserName = findViewById(R.id.tvUserName);
        tvUserAge = findViewById(R.id.tvUserAge);
        tvUserId = findViewById(R.id.tvUserId);
        tvUserBloodGroup = findViewById(R.id.tvUserBloodGroup);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        rvDoctors = findViewById(R.id.rvDoctors);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        loadUserProfile();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        ivProfileImage.setOnClickListener(v -> showProfileSelectionDialog());
        findViewById(R.id.fabAddDoctor).setOnClickListener(v -> showAddDoctorDialog());
        findViewById(R.id.btnSimulateAlerts).setOnClickListener(v -> {
            AlarmReceiver.checkAllMedicinesAndNotify(this);
            Toast.makeText(this, "Daily check complete.", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cardEmergency).setOnClickListener(v -> showEmergencyDoctorDialog());
        findViewById(R.id.cardAppointments).setOnClickListener(v -> showBookingDialog());

        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        setupBottomNavigation();
        requestAppPermissions();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                showEditProfileDialog();
                return true;
            } else if (id == R.id.nav_medicines) {
                startActivity(new Intent(this, MedicineListActivity.class));
                return false;
            } else if (id == R.id.nav_appointments) {
                showBookingDialog();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDoctorList();
    }

    private void loadUserProfile() {
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        if (loggedInMobile.isEmpty()) {
            logout();
            return;
        }

        android.database.Cursor cursor = dbHelper.getUserByMobile(loggedInMobile);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_NAME));
            String age = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_AGE));
            String bloodGroup = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_BLOOD_GROUP));
            String userId = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_MOBILE));

            tvUserName.setText(getString(R.string.user_name_format, name));
            tvUserAge.setText(getString(R.string.user_age_format, age));
            tvUserId.setText(getString(R.string.user_id_format, userId));
            tvUserBloodGroup.setText(bloodGroup);
        } else {
            logout();
        }
        if (cursor != null) cursor.close();
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etName);
        com.google.android.material.textfield.TextInputEditText etAge = dialogView.findViewById(R.id.etAge);
        com.google.android.material.textfield.TextInputEditText etBloodGroup = dialogView.findViewById(R.id.etBloodGroup);
        com.google.android.material.textfield.TextInputEditText etAltMobile = dialogView.findViewById(R.id.etAltMobile);
        com.google.android.material.textfield.TextInputLayout tilName = dialogView.findViewById(R.id.tilName);

        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        android.database.Cursor cursor = dbHelper.getUserByMobile(loggedInMobile);
        if (cursor != null && cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_NAME)));
            etAge.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_AGE)));
            etBloodGroup.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_BLOOD_GROUP)));
            etAltMobile.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_ALT_MOBILE)));
        }
        if (cursor != null) cursor.close();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = Objects.requireNonNull(etName.getText()).toString().trim();
            if (name.isEmpty()) {
                tilName.setError("Name required");
                return;
            }
            dbHelper.updateUser(name, "", etAge.getText().toString(), etBloodGroup.getText().toString(), loggedInMobile, etAltMobile.getText().toString());
            loadUserProfile();
            dialog.dismiss();
        });
    }

    private void showAddDoctorDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_doctor, null);
        com.google.android.material.textfield.TextInputEditText etDoctorName = dialogView.findViewById(R.id.etDoctorName);
        com.google.android.material.textfield.TextInputEditText etHospitalLocation = dialogView.findViewById(R.id.etHospitalLocation);
        com.google.android.material.textfield.TextInputEditText etHospitalContact = dialogView.findViewById(R.id.etHospitalContact);
        com.google.android.material.textfield.TextInputEditText etDoctorEmail = dialogView.findViewById(R.id.etDoctorEmail);

        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");

        new AlertDialog.Builder(this)
                .setTitle("Add Doctor")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    dbHelper.addDoctor(loggedInMobile, etDoctorName.getText().toString(), etHospitalLocation.getText().toString(), etHospitalContact.getText().toString(), etDoctorEmail.getText().toString());
                    refreshDoctorList();
                })
                .show();
    }

    private void showProfileSelectionDialog() {
        String[] profiles = {"Self (Primary)", "Father", "Mother", "Spouse", "Child"};
        int[] icons = {android.R.drawable.ic_menu_myplaces, android.R.drawable.ic_menu_my_calendar, android.R.drawable.ic_menu_camera, android.R.drawable.ic_menu_call, android.R.drawable.ic_menu_send};
        new AlertDialog.Builder(this).setTitle("Select Profile").setItems(profiles, (dialog, which) -> {
            ivProfileImage.setImageResource(icons[which]);
            Toast.makeText(this, "Switched to " + profiles[which], Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void showBookingDialog() {
        String[] options = {"Book New Appointment", "Manage Appointments"};
        new AlertDialog.Builder(this)
                .setTitle("Appointments")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        performBooking();
                    } else {
                        startActivity(new Intent(this, AppointmentListActivity.class));
                    }
                })
                .show();
    }

    private void performBooking() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_book_appointment, null);
        android.widget.Spinner spinnerDoctors = dialogView.findViewById(R.id.spinnerDoctors);
        android.widget.Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);
        android.widget.EditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.EditText etTime = dialogView.findViewById(R.id.etTime);
        android.widget.EditText etReason = dialogView.findViewById(R.id.etReason);

        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        List<Doctor> doctors = dbHelper.getAllDoctors(loggedInMobile);
        List<String> doctorNames = new ArrayList<>();
        for (Doctor d : doctors) doctorNames.add(d.getName());
        spinnerDoctors.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctorNames));

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> etDate.setText(day + "/" + (month + 1) + "/" + year), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, min) -> etTime.setText(hour + ":" + min), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Book Appointment")
                .setView(dialogView)
                .setPositiveButton("Book", (dialog, which) -> {
                    int pos = spinnerDoctors.getSelectedItemPosition();
                    if (pos >= 0 && !etDate.getText().toString().isEmpty()) {
                        Doctor selected = doctors.get(pos);
                        String date = etDate.getText().toString();
                        String time = etTime.getText().toString();
                        String reason = etReason.getText().toString().trim();
                        if (reason.isEmpty()) reason = "General Visit";
                        String priority = spinnerPriority.getSelectedItem().toString();
                        
                        dbHelper.addAppointment(loggedInMobile, selected.getId(), date, time, reason, priority);
                        
                        // Fetch user name for the SMS
                        String userName = "Patient";
                        android.database.Cursor cursor = dbHelper.getUserByMobile(loggedInMobile);
                        if (cursor != null && cursor.moveToFirst()) {
                            userName = cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_NAME));
                            cursor.close();
                        }

                        // Send SMS to Hospital/Doctor
                        String smsMessage = "New Appointment Request: Patient " + userName + " needs an appointment for " + reason + " on " + date + " at " + time + ".";
                        NotificationHelper.sendSmsNotification(this, selected.getContact(), smsMessage);

                        Toast.makeText(this, "Request for appointment sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Please select doctor and date", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showEmergencyDoctorDialog() {
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        List<Doctor> doctors = dbHelper.getAllDoctors(loggedInMobile);
        List<String> names = new ArrayList<>();
        for (Doctor d : doctors) names.add(d.getName());
        new AlertDialog.Builder(this).setTitle("Emergency").setItems(names.toArray(new String[0]), (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + doctors.get(which).getContact()));
            startActivity(intent);
        }).show();
    }

    private void refreshDoctorList() {
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        rvDoctors.setAdapter(new DoctorAdapter(dbHelper.getAllDoctors(loggedInMobile), this));
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sharedPrefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        }
        
        // Request exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override public void onDoctorClick(Doctor doctor) { Intent i = new Intent(this, DoctorDetailActivity.class); i.putExtra("doctor_id", doctor.getId()); startActivity(i); }
    @Override public void onEditDoctor(Doctor doctor) {}
    @Override public void onDeleteDoctor(Doctor doctor) { dbHelper.deleteDoctor(doctor.getId()); refreshDoctorList(); }
}
