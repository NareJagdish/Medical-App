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
import android.view.View;
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

public class MainActivity extends AppCompatActivity implements DoctorAdapter.OnDoctorClickListener {

    private ActivityMainBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_NAME = "name";
    private static final String KEY_AGE = "age";
    private static final String KEY_USER_ID = "user_id";

    private static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 101;

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
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDoctorList();
    }

    private void loadUserProfile() {
        String name = sharedPrefs.getString(KEY_NAME, "Rajesh Deshmukh");
        String age = sharedPrefs.getString(KEY_AGE, "45");
        String userId = sharedPrefs.getString(KEY_USER_ID, "UID-784201");

        binding.tvUserName.setText("Name: " + name);
        binding.tvUserAge.setText("Age: " + age + " years");
        binding.tvUserId.setText("User ID: " + userId);
    }

    private void showEditProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(LayoutInflater.from(this));
        dialogBinding.etName.setText(sharedPrefs.getString(KEY_NAME, ""));
        dialogBinding.etAge.setText(sharedPrefs.getString(KEY_AGE, ""));
        dialogBinding.etUserId.setText(sharedPrefs.getString(KEY_USER_ID, ""));

        new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    String n = dialogBinding.etName.getText().toString().trim();
                    String a = dialogBinding.etAge.getText().toString().trim();
                    String u = dialogBinding.etUserId.getText().toString().trim();
                    if (!n.isEmpty() && !a.isEmpty() && !u.isEmpty()) {
                        sharedPrefs.edit().putString(KEY_NAME, n).putString(KEY_AGE, a).putString(KEY_USER_ID, u).apply();
                        loadUserProfile();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Use Case 1: Add new doctor with hospital details
    private void showAddDoctorDialog() {
        DialogAddDoctorBinding dialogBinding = DialogAddDoctorBinding.inflate(LayoutInflater.from(this));

        new AlertDialog.Builder(this)
                .setTitle("Add Doctor")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = dialogBinding.etDoctorName.getText().toString().trim();
                    String location = dialogBinding.etHospitalLocation.getText().toString().trim();
                    String contact = dialogBinding.etHospitalContact.getText().toString().trim();
                    String email = dialogBinding.etDoctorEmail.getText().toString().trim();

                    if (!name.isEmpty() && !contact.isEmpty()) {
                        dbHelper.addDoctor(name, location, contact, email);
                        refreshDoctorList();
                        Toast.makeText(this, "Doctor Added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Name and Contact are required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        new AlertDialog.Builder(this)
                .setTitle("Edit Doctor")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    String n = dialogBinding.etDoctorName.getText().toString().trim();
                    String l = dialogBinding.etHospitalLocation.getText().toString().trim();
                    String c = dialogBinding.etHospitalContact.getText().toString().trim();
                    String e = dialogBinding.etDoctorEmail.getText().toString().trim();
                    if (!n.isEmpty()) {
                        dbHelper.updateDoctor(doctor.getId(), n, l, c, e);
                        refreshDoctorList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
