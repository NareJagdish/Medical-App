package com.example.medreminderjava;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.ui.AdherenceAdapter;

import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPrefs;
    private String userMobile;
    private RecyclerView rvAdherence;
    private AdherenceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userMobile = sharedPrefs.getString("logged_in_mobile", "");

        rvAdherence = findViewById(R.id.rvAdherence);
        rvAdherence.setLayoutManager(new LinearLayoutManager(this));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        loadStatistics();
        loadAdherenceList();

        findViewById(R.id.btnShareReport).setOnClickListener(v -> shareDetailedReport());
        
        // Add click listener for Appointments card (assuming the LinearLayout or TextView can be clicked)
        // Let's wrap the Appointments stats in an ID if not already there, or just use the tvAppointmentCount parent
        findViewById(R.id.tvAppointmentCount).setOnClickListener(v -> {
            Intent intent = new Intent(this, AppointmentListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdherenceList();
        loadStatistics();
    }

    private void loadAdherenceList() {
        List<Medicine> medicines = dbHelper.getAllMedicines(userMobile);
        adapter = new AdherenceAdapter(medicines, this);
        rvAdherence.setAdapter(adapter);
    }

    private void loadStatistics() {
        int takenCount = dbHelper.getLogCount(userMobile, "Taken");
        int missedCount = dbHelper.getLogCount(userMobile, "Missed");
        int appointmentCount = dbHelper.getUpcomingAppointmentCount(userMobile);
        List<Medicine> medicines = dbHelper.getAllMedicines(userMobile);

        ((TextView) findViewById(R.id.tvTakenCount)).setText(String.valueOf(takenCount));
        ((TextView) findViewById(R.id.tvMissedCount)).setText(String.valueOf(missedCount));
        ((TextView) findViewById(R.id.tvTotalMedCount)).setText(String.valueOf(medicines.size()));
        ((TextView) findViewById(R.id.tvAppointmentCount)).setText(String.valueOf(appointmentCount));
    }

    private void shareDetailedReport() {
        List<Medicine> medicines = dbHelper.getAllMedicines(userMobile);
        StringBuilder sb = new StringBuilder("--- DETAILED HEALTH REPORT ---\n\n");
        sb.append("Medicine Adherence:\n");
        sb.append("Taken: ").append(dbHelper.getLogCount(userMobile, "Taken")).append("\n");
        sb.append("Missed: ").append(dbHelper.getLogCount(userMobile, "Missed")).append("\n\n");

        sb.append("Inventory Status:\n");
        for (Medicine m : medicines) {
            sb.append("- ").append(m.getName()).append(": ").append(m.getRemainingQuantity()).append(" units left\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Share Report"));
    }
}
