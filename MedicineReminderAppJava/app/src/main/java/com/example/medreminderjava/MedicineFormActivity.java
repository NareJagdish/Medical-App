package com.example.medreminderjava;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ActivityMedicineFormBinding;
import com.example.medreminderjava.notification.AlarmReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MedicineFormActivity extends AppCompatActivity {

    private ActivityMedicineFormBinding binding;
    private DatabaseHelper dbHelper;
    private long doctorId;
    private long medicineId = -1; // -1 indicates ADD mode
    private boolean isEditMode = false;
    private Medicine editMedicine;

    private String breakfastTime = "08:00";
    private String lunchTime = "13:30";
    private String dinnerTime = "20:30";

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

        setupListeners();

        if (isEditMode) {
            loadMedicineData();
        }

        // Button clicks
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void setupListeners() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilMedicineName.setError(null);
                binding.tilTimesPerDay.setError(null);
                binding.tilDosagePerTime.setError(null);
                binding.tilTotalQuantity.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        binding.etMedicineName.addTextChangedListener(watcher);
        binding.etTimesPerDay.addTextChangedListener(watcher);
        binding.etDosagePerTime.addTextChangedListener(watcher);
        binding.etTotalQuantity.addTextChangedListener(watcher);

        binding.cbBreakfast.setOnCheckedChangeListener((v, isChecked) -> 
                binding.btnBreakfastTime.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        binding.cbLunch.setOnCheckedChangeListener((v, isChecked) -> 
                binding.btnLunchTime.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        binding.cbDinner.setOnCheckedChangeListener((v, isChecked) -> 
                binding.btnDinnerTime.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        binding.btnBreakfastTime.setOnClickListener(v -> showTimePicker("Breakfast"));
        binding.btnLunchTime.setOnClickListener(v -> showTimePicker("Lunch"));
        binding.btnDinnerTime.setOnClickListener(v -> showTimePicker("Dinner"));
    }

    private void showTimePicker(String mealType) {
        String currentTime;
        if (mealType.equals("Breakfast")) currentTime = breakfastTime;
        else if (mealType.equals("Lunch")) currentTime = lunchTime;
        else currentTime = dinnerTime;

        String[] parts = currentTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            if (mealType.equals("Breakfast")) {
                breakfastTime = selectedTime;
                binding.btnBreakfastTime.setText(selectedTime);
            } else if (mealType.equals("Lunch")) {
                lunchTime = selectedTime;
                binding.btnLunchTime.setText(selectedTime);
            } else {
                dinnerTime = selectedTime;
                binding.btnDinnerTime.setText(selectedTime);
            }
        }, hour, minute, true);
        timePickerDialog.show();
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

        if (editMedicine.getBreakfastTime() != null) {
            breakfastTime = editMedicine.getBreakfastTime();
            binding.btnBreakfastTime.setText(breakfastTime);
        }
        if (editMedicine.getLunchTime() != null) {
            lunchTime = editMedicine.getLunchTime();
            binding.btnLunchTime.setText(lunchTime);
        }
        if (editMedicine.getDinnerTime() != null) {
            dinnerTime = editMedicine.getDinnerTime();
            binding.btnDinnerTime.setText(dinnerTime);
        }
    }

    private void saveMedicine() {
        String name = Objects.requireNonNull(binding.etMedicineName.getText()).toString().trim();
        String timesStr = Objects.requireNonNull(binding.etTimesPerDay.getText()).toString().trim();
        String totalStr = Objects.requireNonNull(binding.etTotalQuantity.getText()).toString().trim();
        String dosageStr = Objects.requireNonNull(binding.etDosagePerTime.getText()).toString().trim();

        boolean isValid = true;
        if (name.isEmpty()) {
            binding.tilMedicineName.setError("Name is required");
            isValid = false;
        }

        int timesPerDay = 0;
        try {
            timesPerDay = Integer.parseInt(timesStr);
            if (timesPerDay <= 0) {
                binding.tilTimesPerDay.setError("Must be > 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            binding.tilTimesPerDay.setError("Invalid number");
            isValid = false;
        }

        int totalQty = 0;
        try {
            totalQty = Integer.parseInt(totalStr);
            if (totalQty <= 0) {
                binding.tilTotalQuantity.setError("Must be > 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            binding.tilTotalQuantity.setError("Invalid number");
            isValid = false;
        }

        int dosagePerTime = 0;
        try {
            dosagePerTime = Integer.parseInt(dosageStr);
            if (dosagePerTime <= 0) {
                binding.tilDosagePerTime.setError("Must be > 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            binding.tilDosagePerTime.setError("Invalid number");
            isValid = false;
        }

        if (!isValid) return;

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

            int remainingQty = editMedicine.getRemainingQuantity();
            if (totalQty != editMedicine.getTotalQuantity()) {
                remainingQty = totalQty;
            }

            // Update DB
            dbHelper.updateMedicine(medicineId, name, timesPerDay, timingRelation, 
                    timingMeals, totalQty, dosagePerTime, remainingQty, 
                    breakfastTime, lunchTime, dinnerTime);

            // Schedule new alarms
            Medicine updatedMedicine = dbHelper.getMedicineById(medicineId);
            if (updatedMedicine != null) {
                AlarmReceiver.scheduleDoseAlarms(this, updatedMedicine);
            }

            Toast.makeText(this, "Medicine updated", Toast.LENGTH_SHORT).show();
        } else {
            // Add mode
            long newId = dbHelper.addMedicine(doctorId, name, timesPerDay, timingRelation, 
                    timingMeals, totalQty, dosagePerTime, totalQty, 
                    breakfastTime, lunchTime, dinnerTime);

            // Schedule alarms for the new medicine
            Medicine newMedicine = dbHelper.getMedicineById(newId);
            if (newMedicine != null) {
                AlarmReceiver.scheduleDoseAlarms(this, newMedicine);
            }

            Toast.makeText(this, "Medicine added", Toast.LENGTH_SHORT).show();
        }

        // Run stock warnings check immediately
        AlarmReceiver.checkAllMedicinesAndNotify(this);

        finish();
    }
}
