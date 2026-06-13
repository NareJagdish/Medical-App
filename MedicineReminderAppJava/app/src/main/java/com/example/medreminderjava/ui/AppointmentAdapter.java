package com.example.medreminderjava.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.Appointment;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private final List<Appointment> appointments;
    private final OnAppointmentActionListener listener;

    public interface OnAppointmentActionListener {
        void onDeleteAppointment(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> appointments, OnAppointmentActionListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment app = appointments.get(position);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(holder.itemView.getContext());
        Doctor doc = dbHelper.getDoctorById(app.getDoctorId());
        
        holder.tvDoctorName.setText(doc != null ? "Dr. " + doc.getName() : "Unknown Doctor");
        holder.tvAppDate.setText(app.getDate() + " at " + app.getTime());
        holder.tvReason.setText("Reason: " + app.getReason());
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAppointment(app);
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvAppDate, tvReason;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvAppDate = itemView.findViewById(R.id.tvAppDate);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
