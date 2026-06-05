package com.example.medreminderjava;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ActivityMedicineFormBinding;
import com.example.medreminderjava.notification.AlarmReceiver;

import java.util.ArrayList;
import java.util.List;

public class MedicineFormActivity extends AppCompatActivity {

    private ActivityMedicineFormBinding binding;
    private DatabaseHelper dbHelper;
    private long doctorId;
    private long medicineId = -1; // -1 indicates ADD mode
    private boolean isEditMode = false;
    private Medicine editMedicine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicineFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        doctorId = getIntent().getLongExtra("doctor_id", -1);
        medicineId = getIntent().getLongExtra("medicine_id", -1);
        isEditMode = (medicineId != -1);

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Medicine" : "Add Medicine");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (isEditMode) {
            loadMedicineData();
        }

        // Button clicks
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void loadMedicineData() {
        editMedicine = dbHelper.getMedicineById(medicineId);
        if (editMedicine == null) {
            Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.etMedicineName.setText(editMedicine.getName());
        binding.etTimesPerDay.setText(String.valueOf(editMedicine.getTimesPerDay()));
        binding.etTotalQuantity.setText(String.valueOf(editMedicine.getTotalQuantity()));
        binding.etDosagePerTime.setText(String.valueOf(editMedicine.getDosagePerTime()));

        if ("Before".equalsIgnoreCase(editMedicine.getTimingRelation())) {
            binding.rbBeforeMeal.setChecked(true);
        } else {
            binding.rbAfterMeal.setChecked(true);
        }

        String meals = editMedicine.getTimingMeals();
        if (meals != null) {
            binding.cbBreakfast.setChecked(meals.contains("Breakfast"));
            binding.cbLunch.setChecked(meals.contains("Lunch"));
            binding.cbDinner.setChecked(meals.contains("Dinner"));
        }
    }

    private void saveMedicine() {
        String name = binding.etMedicineName.getText().toString().trim();
        String timesStr = binding.etTimesPerDay.getText().toString().trim();
        String totalStr = binding.etTotalQuantity.getText().toString().trim();
        String dosageStr = binding.etDosagePerTime.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            binding.etMedicineName.setError("Name is required");
            return;
        }

        int timesPerDay;
        try {
            timesPerDay = Integer.parseInt(timesStr);
            if (timesPerDay <= 0) {
                binding.etTimesPerDay.setError("Must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etTimesPerDay.setError("Invalid number");
            return;
        }

        int totalQty;
        try {
            totalQty = Integer.parseInt(totalStr);
            if (totalQty <= 0) {
                binding.etTotalQuantity.setError("Must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etTotalQuantity.setError("Invalid number");
            return;
        }

        int dosagePerTime;
        try {
            dosagePerTime = Integer.parseInt(dosageStr);
            if (dosagePerTime <= 0) {
                binding.etDosagePerTime.setError("Must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etDosagePerTime.setError("Invalid number");
            return;
        }

        // Meal Selection Validation
        List<String> selectedMeals = new ArrayList<>();
        if (binding.cbBreakfast.isChecked()) selectedMeals.add("Breakfast");
        if (binding.cbLunch.isChecked()) selectedMeals.add("Lunch");
        if (binding.cbDinner.isChecked()) selectedMeals.add("Dinner");

        if (selectedMeals.isEmpty()) {
            Toast.makeText(this, "Please select at least one meal timing", Toast.LENGTH_LONG).show();
            return;
        }

        // Format CSV meals string
        StringBuilder mealsSb = new StringBuilder();
        for (int i = 0; i < selectedMeals.size(); i++) {
            mealsSb.append(selectedMeals.get(i));
            if (i < selectedMeals.size() - 1) {
                mealsSb.append(",");
            }
        }
        String timingMeals = mealsSb.toString();
        String timingRelation = binding.rbBeforeMeal.isChecked() ? "Before" : "After";

        if (isEditMode) {
            // Cancel old alarms
            AlarmReceiver.cancelAlarmsForMedicine(this, editMedicine);

            // Determine remaining quantity.
            // If they changed the total quantity, we set the remaining quantity to match the new total quantity.
            // This functions as an automatic "refill" event. Otherwise, keep current remaining stock.
            int remainingQty = editMedicine.getRemainingQuantity();
            if (totalQty != editMedicine.getTotalQuantity()) {
                remainingQty = totalQty;
            }

            // Update DB
            dbHelper.updateMedicine(medicineId, name, timesPerDay, timingRelation, 
                    timingMeals, totalQty, dosagePerTime, remainingQty);

            // Schedule new alarms
            Medicine updatedMedicine = dbHelper.getMedicineById(medicineId);
            if (updatedMedicine != null) {
                AlarmReceiver.scheduleDoseAlarms(this, updatedMedicine);
            }

            Toast.makeText(this, "Medicine updated", Toast.LENGTH_SHORT).show();
        } else {
            // Add mode: remaining stock defaults to total starting quantity
            long newId = dbHelper.addMedicine(doctorId, name, timesPerDay, timingRelation, 
                    timingMeals, totalQty, dosagePerTime, totalQty);

            // Schedule alarms for the new medicine
            Medicine newMedicine = dbHelper.getMedicineById(newId);
            if (newMedicine != null) {
                AlarmReceiver.scheduleDoseAlarms(this, newMedicine);
            }

            Toast.makeText(this, "Medicine added", Toast.LENGTH_SHORT).show();
        }

        // Run stock warnings check immediately to trigger low stock notification if applicable
        AlarmReceiver.checkAllMedicinesAndNotify(this);

        finish();
    }
}
