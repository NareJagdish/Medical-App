package com.example.medreminderjava;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.notification.AlarmReceiver;
import com.example.medreminderjava.ui.MedicineAdapter;

import java.util.List;

public class MedicineListActivity extends AppCompatActivity implements MedicineAdapter.OnMedicineActionListener {

    private DatabaseHelper dbHelper;
    private RecyclerView rvAllMedicines;
    private TextView tvNoMedicines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        dbHelper = DatabaseHelper.getInstance(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAllMedicines = findViewById(R.id.rvAllMedicines);
        tvNoMedicines = findViewById(R.id.tvNoMedicines);

        rvAllMedicines.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines();
    }

    private void loadMedicines() {
        SharedPreferences sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String loggedInMobile = sharedPrefs.getString("logged_in_mobile", "");
        List<Medicine> medicines = dbHelper.getAllMedicines(loggedInMobile);
        if (medicines.isEmpty()) {
            tvNoMedicines.setVisibility(View.VISIBLE);
            rvAllMedicines.setVisibility(View.GONE);
        } else {
            tvNoMedicines.setVisibility(View.GONE);
            rvAllMedicines.setVisibility(View.VISIBLE);
            MedicineAdapter adapter = new MedicineAdapter(medicines, this);
            rvAllMedicines.setAdapter(adapter);
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
                .show();
    }

    @Override
    public void onDoseDeducted() {
        loadMedicines();
    }
}
