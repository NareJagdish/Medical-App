package com.example.medreminderjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ActivityDoctorDetailBinding;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.ui.MedicineAdapter;

import java.util.List;

public class DoctorDetailActivity extends AppCompatActivity implements MedicineAdapter.OnMedicineActionListener {

    private ActivityDoctorDetailBinding binding;
    private DatabaseHelper dbHelper;
    private long doctorId;
    private Doctor doctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);
        doctorId = getIntent().getLongExtra("doctor_id", -1);

        if (doctorId == -1) {
            Toast.makeText(this, "Error: Invalid doctor selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        doctor = dbHelper.getDoctorById(doctorId);
        if (doctor == null) {
            Toast.makeText(this, "Error: Doctor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(doctor.getName());
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Display Header info
        binding.tvDetailDoctorName.setText(doctor.getName());

        // Setup RecyclerView
        binding.rvMedicines.setLayoutManager(new LinearLayoutManager(this));

        // Add Medicine click
        binding.btnAddNewMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicineFormActivity.class);
            intent.putExtra("doctor_id", doctorId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        // Query medicines list
        List<Medicine> medicines = dbHelper.getMedicinesForDoctor(doctorId);
        
        if (medicines.isEmpty()) {
            binding.tvEmptyMedicines.setVisibility(View.VISIBLE);
            binding.rvMedicines.setVisibility(View.GONE);
            binding.tvDetailRemainingSummary.setText("No medicines prescribed yet.");
            binding.tvDetailRemainingSummary.setTextColor(getColor(R.color.grey_dark));
        } else {
            binding.tvEmptyMedicines.setVisibility(View.GONE);
            binding.rvMedicines.setVisibility(View.VISIBLE);
            
            MedicineAdapter adapter = new MedicineAdapter(medicines, this);
            binding.rvMedicines.setAdapter(adapter);

            // Compute minimum days remaining of medicines for this doctor
            int minRemainingDays = dbHelper.getDoctorMinRemainingDays(doctorId);
            if (minRemainingDays == 0) {
                binding.tvDetailRemainingSummary.setText("Alert: Out of stock! Please refill immediately.");
                binding.tvDetailRemainingSummary.setTextColor(getColor(R.color.alert_danger));
            } else if (minRemainingDays <= 2) {
                binding.tvDetailRemainingSummary.setText("Critical: " + minRemainingDays + " days remaining. Buy medicines!");
                binding.tvDetailRemainingSummary.setTextColor(getColor(R.color.alert_danger));
            } else if (minRemainingDays <= 10) {
                binding.tvDetailRemainingSummary.setText("Warning: " + minRemainingDays + " days remaining. Refill soon.");
                binding.tvDetailRemainingSummary.setTextColor(getColor(R.color.alert_warning));
            } else {
                binding.tvDetailRemainingSummary.setText("Status: " + minRemainingDays + " days remaining.");
                binding.tvDetailRemainingSummary.setTextColor(getColor(R.color.alert_ok));
            }
        }
    }

    @Override
    public void onEditMedicine(Medicine medicine) {
        Intent intent = new Intent(this, MedicineFormActivity.class);
        intent.putExtra("doctor_id", doctorId);
        intent.putExtra("medicine_id", medicine.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteMedicine(Medicine medicine) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setMessage("Are you sure you want to delete '" + medicine.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Cancel scheduled alarms first
                    AlarmReceiver.cancelAlarmsForMedicine(DoctorDetailActivity.this, medicine);
                    
                    // Delete from database
                    dbHelper.deleteMedicine(medicine.getId());
                    
                    refreshUI();
                    Toast.makeText(DoctorDetailActivity.this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDoseDeducted() {
        refreshUI();
    }
}
