package com.example.medreminderjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.data.Medicine;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    private final List<Doctor> doctors;
    private final OnDoctorClickListener listener;

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
        void onEditDoctor(Doctor doctor);
        void onDeleteDoctor(Doctor doctor);
    }

    public DoctorAdapter(List<Doctor> doctors, OnDoctorClickListener listener) {
        this.doctors = doctors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.bind(doctors.get(position));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    class DoctorViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDoctorName, tvMedicineStatusSummary, tvRemainingDaysBadge;
        private final View btnEditDoctor, btnDeleteDoctor;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvMedicineStatusSummary = itemView.findViewById(R.id.tvMedicineStatusSummary);
            tvRemainingDaysBadge = itemView.findViewById(R.id.tvRemainingDaysBadge);
            btnEditDoctor = itemView.findViewById(R.id.btnEditDoctor);
            btnDeleteDoctor = itemView.findViewById(R.id.btnDeleteDoctor);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Doctor doctor) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(itemView.getContext());
            tvDoctorName.setText(doctor.getName());
            List<Medicine> medicines = dbHelper.getMedicinesForDoctor(doctor.getId());
            tvMedicineStatusSummary.setText(medicines.size() + " medicines prescribed");

            int minDays = dbHelper.getDoctorMinRemainingDays(doctor.getId());
            if (minDays == -1) tvRemainingDaysBadge.setText("No Medicines");
            else if (minDays == 0) tvRemainingDaysBadge.setText("Finished!");
            else tvRemainingDaysBadge.setText(minDays + " days remaining");

            itemView.setOnClickListener(v -> { if (listener != null) listener.onDoctorClick(doctor); });
            btnEditDoctor.setOnClickListener(v -> { if (listener != null) listener.onEditDoctor(doctor); });
            btnDeleteDoctor.setOnClickListener(v -> { if (listener != null) listener.onDeleteDoctor(doctor); });
        }
    }
}
