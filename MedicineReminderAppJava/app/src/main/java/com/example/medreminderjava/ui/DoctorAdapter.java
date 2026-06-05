package com.example.medreminderjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Doctor;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ItemDoctorBinding;

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
        ItemDoctorBinding binding = ItemDoctorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DoctorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.bind(doctors.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    class DoctorViewHolder extends RecyclerView.ViewHolder {
        private final ItemDoctorBinding binding;

        public DoctorViewHolder(@NonNull ItemDoctorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Doctor doctor, int serialNumber) {
            Context context = binding.getRoot().getContext();
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);

            // Bind name
            binding.tvDoctorName.setText(doctor.getName());

            // Query medicines list to count total items and determine remaining days
            List<Medicine> medicines = dbHelper.getMedicinesForDoctor(doctor.getId());
            binding.tvMedicineStatusSummary.setText(medicines.size() + " medicines prescribed");

            // Calculate minimum remaining days across all medicines for this doctor
            int minDays = dbHelper.getDoctorMinRemainingDays(doctor.getId());

            if (minDays == -1) {
                // No medicines added yet
                binding.tvRemainingDaysBadge.setText("No Medicines");
                binding.tvRemainingDaysBadge.setBackgroundResource(R.drawable.badge_bg_normal);
                binding.tvRemainingDaysBadge.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_dark)));
            } else if (minDays == 0) {
                // Out of stock
                binding.tvRemainingDaysBadge.setText("Finished!");
                binding.tvRemainingDaysBadge.setBackgroundResource(R.drawable.badge_bg_danger);
                binding.tvRemainingDaysBadge.setBackgroundTintList(null); // use shape solid color directly
            } else {
                binding.tvRemainingDaysBadge.setText(minDays + " days remaining");
                
                // Color code the badge based on severity of supply levels
                if (minDays <= 2) {
                    binding.tvRemainingDaysBadge.setBackgroundResource(R.drawable.badge_bg_danger);
                    binding.tvRemainingDaysBadge.setBackgroundTintList(null);
                } else if (minDays <= 10) {
                    binding.tvRemainingDaysBadge.setBackgroundResource(R.drawable.badge_bg_warning);
                    binding.tvRemainingDaysBadge.setBackgroundTintList(null);
                } else {
                    binding.tvRemainingDaysBadge.setBackgroundResource(R.drawable.badge_bg_normal);
                    binding.tvRemainingDaysBadge.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.alert_ok)));
                }
            }

            // Click action
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDoctorClick(doctor);
                }
            });

            binding.btnEditDoctor.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditDoctor(doctor);
                }
            });

            binding.btnDeleteDoctor.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteDoctor(doctor);
                }
            });
        }
    }
}
