package com.example.medreminderjava;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.notification.AlarmReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MedicineFormActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long medicineId = -1;
    private long doctorId = -1;
    private boolean isEditMode = false;

    private EditText etMedicineName, etTimesPerDay, etDosagePerTime, etTotalQuantity;
    private RadioGroup rgTimingRelation;
    private RadioButton rbBefore, rbAfter;
    private CheckBox cbBreakfast, cbLunch, cbDinner;
    private TextView tvBreakfastTime, tvLunchTime, tvDinnerTime;
    private Spinner spinnerDoctors;

    private List<Doctor> doctorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_form);

        dbHelper = DatabaseHelper.getInstance(this);
        medicineId = getIntent().getLongExtra("medicine_id", -1);
        doctorId = getIntent().getLongExtra("doctor_id", -1);
        isEditMode = (medicineId != -1);

        initUI();
        setupListeners();
        loadDoctors();

        if (isEditMode) {
            loadMedicineData();
        }
    }

    private void initUI() {
        etMedicineName = findViewById(R.id.etMedicineName);
        etTimesPerDay = findViewById(R.id.etTimesPerDay);
        etDosagePerTime = findViewById(R.id.etDosagePerTime);
        etTotalQuantity = findViewById(R.id.etTotalQuantity);
        rgTimingRelation = findViewById(R.id.rgTimingRelation);
        rbBefore = findViewById(R.id.rbBefore);
        rbAfter = findViewById(R.id.rbAfter);
        cbBreakfast = findViewById(R.id.cbBreakfast);
        cbLunch = findViewById(R.id.cbLunch);
        cbDinner = findViewById(R.id.cbDinner);
        tvBreakfastTime = findViewById(R.id.tvBreakfastTime);
        tvLunchTime = findViewById(R.id.tvLunchTime);
        tvDinnerTime = findViewById(R.id.tvDinnerTime);
        spinnerDoctors = findViewById(R.id.spinnerDoctors);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit Medicine" : "Add Medicine");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        tvBreakfastTime.setOnClickListener(v -> showTimePicker(tvBreakfastTime));
        tvLunchTime.setOnClickListener(v -> showTimePicker(tvLunchTime));
        tvDinnerTime.setOnClickListener(v -> showTimePicker(tvDinnerTime));

        findViewById(R.id.btnSave).setOnClickListener(v -> saveMedicine());
    }

    private void loadDoctors() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String mobile = prefs.getString("logged_in_mobile", "");
        doctorList = dbHelper.getAllDoctors(mobile);

        List<String> names = new ArrayList<>();
        names.add("No Doctor (Self)");
        int selectedIndex = 0;
        for (int i = 0; i < doctorList.size(); i++) {
            names.add(doctorList.get(i).getName());
            if (doctorList.get(i).getId() == doctorId) {
                selectedIndex = i + 1;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(adapter);
        spinnerDoctors.setSelection(selectedIndex);
    }

    private void showTimePicker(TextView target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, min) -> {
            String amPm = (hour < 12) ? "AM" : "PM";
            int h12 = (hour == 0 || hour == 12) ? 12 : hour % 12;
            String time = String.format(Locale.getDefault(), "%02d:%02d %s", h12, min, amPm);
            target.setText(time);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void loadMedicineData() {
        Medicine m = dbHelper.getMedicineById(medicineId);
        if (m == null) return;

        etMedicineName.setText(m.getName());
        etTimesPerDay.setText(String.valueOf(m.getTimesPerDay()));
        etDosagePerTime.setText(String.valueOf(m.getDosagePerTime()));
        etTotalQuantity.setText(String.valueOf(m.getTotalQuantity()));

        if ("Before".equalsIgnoreCase(m.getTimingRelation())) rbBefore.setChecked(true);
        else rbAfter.setChecked(true);

        String meals = m.getTimingMeals();
        if (meals != null) {
            cbBreakfast.setChecked(meals.contains("Breakfast"));
            cbLunch.setChecked(meals.contains("Lunch"));
            cbDinner.setChecked(meals.contains("Dinner"));
        }

        tvBreakfastTime.setText(m.getBreakfastTime());
        tvLunchTime.setText(m.getLunchTime());
        tvDinnerTime.setText(m.getDinnerTime());
        
        // Handle doctor selection
        if (m.getDoctorId() > 0) {
            for (int i = 0; i < doctorList.size(); i++) {
                if (doctorList.get(i).getId() == m.getDoctorId()) {
                    spinnerDoctors.setSelection(i + 1);
                    break;
                }
            }
        }
    }

    private void saveMedicine() {
        String name = etMedicineName.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "Enter medicine name", Toast.LENGTH_SHORT).show(); return; }

        int times = 1;
        try { times = Integer.parseInt(etTimesPerDay.getText().toString()); } catch (Exception e) {}

        int dosage = 1;
        try { dosage = Integer.parseInt(etDosagePerTime.getText().toString()); } catch (Exception e) {}

        int total = 30;
        try { total = Integer.parseInt(etTotalQuantity.getText().toString()); } catch (Exception e) {}

        String relation = rbBefore.isChecked() ? "Before" : "After";

        StringBuilder mealsBuilder = new StringBuilder();
        if (cbBreakfast.isChecked()) mealsBuilder.append("Breakfast,");
        if (cbLunch.isChecked()) mealsBuilder.append("Lunch,");
        if (cbDinner.isChecked()) mealsBuilder.append("Dinner");
        String meals = mealsBuilder.toString();
        if (meals.endsWith(",")) meals = meals.substring(0, meals.length() - 1);

        if (meals.isEmpty()) { Toast.makeText(this, "Select at least one meal time", Toast.LENGTH_SHORT).show(); return; }

        long selectedDocId = -1;
        int docPos = spinnerDoctors.getSelectedItemPosition();
        if (docPos > 0 && doctorList != null && (docPos - 1) < doctorList.size()) {
            selectedDocId = doctorList.get(docPos - 1).getId();
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userMobile = prefs.getString("logged_in_mobile", "");

        if (isEditMode) {
            dbHelper.updateMedicine(medicineId, selectedDocId, name, times, relation, meals, total, dosage, total, "", "",
                    tvBreakfastTime.getText().toString(), tvLunchTime.getText().toString(), tvDinnerTime.getText().toString());
        } else {
            medicineId = dbHelper.addMedicine(userMobile, selectedDocId, name, times, relation, meals, total, dosage, total, "", "",
                    tvBreakfastTime.getText().toString(), tvLunchTime.getText().toString(), tvDinnerTime.getText().toString());
        }

        Medicine m = dbHelper.getMedicineById(medicineId);
        if (m != null) {
            AlarmReceiver.cancelAlarmsForMedicine(this, m);
            AlarmReceiver.scheduleDoseAlarms(this, m);
        }

        Toast.makeText(this, "Medicine Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
