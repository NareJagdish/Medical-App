package com.example.medreminderjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.ui.MedicineAdapter;

import java.util.List;

public class DoctorDetailActivity extends AppCompatActivity implements MedicineAdapter.OnMedicineActionListener {

    private DatabaseHelper dbHelper;
    private long doctorId;
    private Doctor doctor;
    private RecyclerView rvMedicines;
    private TextView tvEmptyMedicines, tvDetailRemainingSummary, tvDetailDoctorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        dbHelper = DatabaseHelper.getInstance(this);
        doctorId = getIntent().getLongExtra("doctor_id", -1);

        if (doctorId == -1) { finish(); return; }
        doctor = dbHelper.getDoctorById(doctorId);
        if (doctor == null) { finish(); return; }

        rvMedicines = findViewById(R.id.rvMedicines);
        tvEmptyMedicines = findViewById(R.id.tvEmptyMedicines);
        tvDetailRemainingSummary = findViewById(R.id.tvDetailRemainingSummary);
        tvDetailDoctorName = findViewById(R.id.tvDetailDoctorName);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(doctor.getName());
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvDetailDoctorName.setText(doctor.getName());
        rvMedicines.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAddNewMedicine).setOnClickListener(v -> {
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
        List<Medicine> medicines = dbHelper.getMedicinesForDoctor(doctorId);
        if (medicines.isEmpty()) {
            tvEmptyMedicines.setVisibility(View.VISIBLE);
            rvMedicines.setVisibility(View.GONE);
            tvDetailRemainingSummary.setText("No medicines prescribed yet.");
        } else {
            tvEmptyMedicines.setVisibility(View.GONE);
            rvMedicines.setVisibility(View.VISIBLE);
            rvMedicines.setAdapter(new MedicineAdapter(medicines, this));
            int minDays = dbHelper.getDoctorMinRemainingDays(doctorId);
            if (minDays == 0) tvDetailRemainingSummary.setText("Alert: Out of stock!");
            else tvDetailRemainingSummary.setText("Status: " + minDays + " days remaining.");
        }
    }

    @Override public void onEditMedicine(Medicine medicine) {
        Intent intent = new Intent(this, MedicineFormActivity.class);
        intent.putExtra("doctor_id", doctorId);
        intent.putExtra("medicine_id", medicine.getId());
        startActivity(intent);
    }

    @Override public void onDeleteMedicine(Medicine medicine) {
        new AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete '" + medicine.getName() + "'?").setPositiveButton("Delete", (dialog, which) -> {
            AlarmReceiver.cancelAlarmsForMedicine(this, medicine);
            dbHelper.deleteMedicine(medicine.getId());
            refreshUI();
        }).show();
    }

    @Override public void onDoseDeducted() { refreshUI(); }
}
