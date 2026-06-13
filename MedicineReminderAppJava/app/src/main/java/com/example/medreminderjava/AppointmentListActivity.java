package com.example.medreminderjava;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.data.Appointment;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.ui.AppointmentAdapter;

import java.util.List;

public class AppointmentListActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    private DatabaseHelper dbHelper;
    private RecyclerView rvAppointments;
    private TextView tvNoAppointments;
    private String userMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_list);

        dbHelper = DatabaseHelper.getInstance(this);
        SharedPreferences sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userMobile = sharedPrefs.getString("logged_in_mobile", "");

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAppointments = findViewById(R.id.rvAppointments);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        loadAppointments();
    }

    private void loadAppointments() {
        List<Appointment> list = dbHelper.getAllAppointments(userMobile);
        if (list.isEmpty()) {
            tvNoAppointments.setVisibility(View.VISIBLE);
            rvAppointments.setVisibility(View.GONE);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
            rvAppointments.setVisibility(View.VISIBLE);
            rvAppointments.setAdapter(new AppointmentAdapter(list, this));
        }
    }

    @Override
    public void onDeleteAppointment(Appointment appointment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteAppointment(appointment.getId());
                    loadAppointments();
                    Toast.makeText(this, "Appointment Deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
