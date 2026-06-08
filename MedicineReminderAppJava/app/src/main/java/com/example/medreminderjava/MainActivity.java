package com.example.medreminderjava;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.databinding.ActivityMainBinding;
import com.example.medreminderjava.databinding.DialogEditProfileBinding;
import com.example.medreminderjava.databinding.DialogAddDoctorBinding;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.notification.NotificationHelper;
import com.example.medreminderjava.ui.DoctorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements DoctorAdapter.OnDoctorClickListener {

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGGED_IN_MOBILE = "logged_in_mobile";

    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        NotificationHelper.createNotificationChannels(this);
        AlarmReceiver.scheduleDailyCheck(this);

        setSupportActionBar(binding.toolbar);
        loadUserProfile();

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.fabAddDoctor.setOnClickListener(v -> showAddDoctorDialog());
        binding.btnSimulateAlerts.setOnClickListener(v -> {
            AlarmReceiver.checkAllMedicinesAndNotify(this);
            Toast.makeText(this, "Daily check complete.", Toast.LENGTH_SHORT).show();
        });

        // Use Case 2: Emergency option click listener
        binding.cardEmergency.setOnClickListener(v -> showEmergencyDoctorDialog());

        binding.rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        requestAppPermissions();
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

            binding.tvUserName.setText(getString(R.string.user_name_format, name));
            binding.tvUserAge.setText(getString(R.string.user_age_format, age));
            binding.tvUserId.setText(getString(R.string.user_id_format, userId));
            binding.tvUserBloodGroup.setText(bloodGroup);
        } else {
            // User not found in database, session is invalid
            logout();
        }
        if (cursor != null) cursor.close();
    }

    private void showEditProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(this));
        String loggedInMobile = sharedPrefs.getString(KEY_LOGGED_IN_MOBILE, "");
        
        android.database.Cursor cursor = dbHelper.getUserByMobile(loggedInMobile);
        if (cursor != null && cursor.moveToFirst()) {
            dialogBinding.etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_NAME)));
            dialogBinding.etLocation.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_LOCATION)));
            dialogBinding.etAge.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_AGE)));
            dialogBinding.etBloodGroup.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_BLOOD_GROUP)));
            dialogBinding.etAltMobile.setText(cursor.getString(cursor.getColumnIndexOrThrow(com.example.medreminderjava.data.DbContract.UserEntry.COLUMN_ALT_MOBILE)));
            dialogBinding.etUserId.setText(loggedInMobile);
        }
        if (cursor != null) cursor.close();

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilName.setError(null);
                dialogBinding.tilAltMobile.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        dialogBinding.etName.addTextChangedListener(watcher);
        dialogBinding.etAltMobile.addTextChangedListener(watcher);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = Objects.requireNonNull(dialogBinding.etName.getText()).toString().trim();
            String location = Objects.requireNonNull(dialogBinding.etLocation.getText()).toString().trim();
            String age = Objects.requireNonNull(dialogBinding.etAge.getText()).toString().trim();
            String bloodGroup = Objects.requireNonNull(dialogBinding.etBloodGroup.getText()).toString().trim();
            String altMobile = Objects.requireNonNull(dialogBinding.etAltMobile.getText()).toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                dialogBinding.tilName.setError("Name required");
                isValid = false;
            } else if (name.matches(".*\\d.*")) {
                dialogBinding.tilName.setError("Name shouldn't have digits");
                isValid = false;
            }

            if (!altMobile.isEmpty() && altMobile.length() != 10) {
                dialogBinding.tilAltMobile.setError("Must be 10 digits");
                isValid = false;
            }

            if (isValid) {
                dbHelper.updateUser(name, location, age, bloodGroup, loggedInMobile, altMobile);
                loadUserProfile();
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    // Use Case 1: Add new doctor with hospital details
    private void showAddDoctorDialog() {
        DialogAddDoctorBinding dialogBinding = DialogAddDoctorBinding.inflate(LayoutInflater.from(this));
        
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilDoctorName.setError(null);
                dialogBinding.tilHospitalContact.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        dialogBinding.etDoctorName.addTextChangedListener(watcher);
        dialogBinding.etHospitalContact.addTextChangedListener(watcher);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Doctor")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = Objects.requireNonNull(dialogBinding.etDoctorName.getText()).toString().trim();
            String location = Objects.requireNonNull(dialogBinding.etHospitalLocation.getText()).toString().trim();
            String contact = Objects.requireNonNull(dialogBinding.etHospitalContact.getText()).toString().trim();
            String email = Objects.requireNonNull(dialogBinding.etDoctorEmail.getText()).toString().trim();

            boolean isValid = true;
            if (name.isEmpty()) {
                dialogBinding.tilDoctorName.setError("Name required");
                isValid = false;
            } else if (name.matches(".*\\d.*")) {
                dialogBinding.tilDoctorName.setError("Name shouldn't have digits");
                isValid = false;
            }

            if (contact.isEmpty()) {
                dialogBinding.tilHospitalContact.setError("Contact required");
                isValid = false;
            } else if (contact.length() != 10) {
                dialogBinding.tilHospitalContact.setError("Must be 10 digits");
                isValid = false;
            }

            if (isValid) {
                dbHelper.addDoctor(name, location, contact, email);
                refreshDoctorList();
                Toast.makeText(this, "Doctor Added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    // Use Case 2: Display doctor names and call on click
    private void showEmergencyDoctorDialog() {
        List<Doctor> doctors = dbHelper.getAllDoctors();
        if (doctors.isEmpty()) {
            Toast.makeText(this, "No doctors added yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> doctorNames = new ArrayList<>();
        for (Doctor d : doctors) {
            doctorNames.add(d.getName() + " (" + d.getContact() + ")");
        }

        new AlertDialog.Builder(this)
                .setTitle("Emergency Call")
                .setItems(doctorNames.toArray(new String[0]), (dialog, which) -> {
                    Doctor selected = doctors.get(which);
                    String phone = selected.getContact();
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No contact number available", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void refreshDoctorList() {
        List<Doctor> doctors = dbHelper.getAllDoctors();
        DoctorAdapter adapter = new DoctorAdapter(doctors, this);
        binding.rvDoctors.setAdapter(adapter);
    }

    @Override
    public void onDoctorClick(Doctor doctor) {
        Intent intent = new Intent(this, DoctorDetailActivity.class);
        intent.putExtra("doctor_id", doctor.getId());
        startActivity(intent);
    }

    @Override
    public void onEditDoctor(Doctor doctor) {
        DialogAddDoctorBinding dialogBinding = DialogAddDoctorBinding.inflate(LayoutInflater.from(this));
        dialogBinding.etDoctorName.setText(doctor.getName());
        dialogBinding.etHospitalLocation.setText(doctor.getLocation());
        dialogBinding.etHospitalContact.setText(doctor.getContact());
        dialogBinding.etDoctorEmail.setText(doctor.getEmail());

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilDoctorName.setError(null);
                dialogBinding.tilHospitalContact.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        dialogBinding.etDoctorName.addTextChangedListener(watcher);
        dialogBinding.etHospitalContact.addTextChangedListener(watcher);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Doctor")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = Objects.requireNonNull(dialogBinding.etDoctorName.getText()).toString().trim();
            String l = Objects.requireNonNull(dialogBinding.etHospitalLocation.getText()).toString().trim();
            String c = Objects.requireNonNull(dialogBinding.etHospitalContact.getText()).toString().trim();
            String e = Objects.requireNonNull(dialogBinding.etDoctorEmail.getText()).toString().trim();
            
            boolean isValid = true;
            if (n.isEmpty()) {
                dialogBinding.tilDoctorName.setError("Name required");
                isValid = false;
            } else if (n.matches(".*\\d.*")) {
                dialogBinding.tilDoctorName.setError("Name shouldn't have digits");
                isValid = false;
            }

            if (c.isEmpty()) {
                dialogBinding.tilHospitalContact.setError("Contact required");
                isValid = false;
            } else if (c.length() != 10) {
                dialogBinding.tilHospitalContact.setError("Must be 10 digits");
                isValid = false;
            }

            if (isValid) {
                dbHelper.updateDoctor(doctor.getId(), n, l, c, e);
                refreshDoctorList();
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onDeleteDoctor(Doctor doctor) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Doctor")
                .setMessage("Delete Dr. " + doctor.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteDoctor(doctor.getId());
                    refreshDoctorList();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
