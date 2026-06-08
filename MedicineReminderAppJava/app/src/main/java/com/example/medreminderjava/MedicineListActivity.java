package com.example.medreminderjava;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ActivityMedicineListBinding;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.ui.MedicineAdapter;

import java.util.List;

public class MedicineListActivity extends AppCompatActivity implements MedicineAdapter.OnMedicineActionListener {

    private ActivityMedicineListBinding binding;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicineListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rvAllMedicines.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines();
    }

    private void loadMedicines() {
        List<Medicine> medicines = dbHelper.getAllMedicines();
        if (medicines.isEmpty()) {
            binding.tvNoMedicines.setVisibility(View.VISIBLE);
            binding.rvAllMedicines.setVisibility(View.GONE);
        } else {
            binding.tvNoMedicines.setVisibility(View.GONE);
            binding.rvAllMedicines.setVisibility(View.VISIBLE);
            MedicineAdapter adapter = new MedicineAdapter(medicines, this);
            binding.rvAllMedicines.setAdapter(adapter);
        }
    }

    @Override
    public void onEditMedicine(Medicine medicine) {
        Intent intent = new Intent(this, MedicineFormActivity.class);
        intent.putExtra("doctor_id", medicine.getDoctorId());
        intent.putExtra("medicine_id", medicine.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteMedicine(Medicine medicine) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setMessage("Are you sure you want to delete '" + medicine.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AlarmReceiver.cancelAlarmsForMedicine(this, medicine);
                    dbHelper.deleteMedicine(medicine.getId());
                    loadMedicines();
                    Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDoseDeducted() {
        loadMedicines();
    }
}