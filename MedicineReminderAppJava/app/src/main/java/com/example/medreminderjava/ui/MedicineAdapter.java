package com.example.medreminderjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;
import com.example.medreminderjava.databinding.ItemMedicineBinding;
import com.example.medreminderjava.notification.AlarmReceiver;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private final List<Medicine> medicines;
    private final OnMedicineActionListener actionListener;

    public interface OnMedicineActionListener {
        void onEditMedicine(Medicine medicine);
        void onDeleteMedicine(Medicine medicine);
        void onDoseDeducted();
    }

    public MedicineAdapter(List<Medicine> medicines, OnMedicineActionListener actionListener) {
        this.medicines = medicines;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicineBinding binding = ItemMedicineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MedicineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        holder.bind(medicines.get(position));
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    class MedicineViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicineBinding binding;

        public MedicineViewHolder(@NonNull ItemMedicineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Medicine medicine) {
            Context context = binding.getRoot().getContext();
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);

            binding.tvMedicineName.setText(medicine.getName());
            binding.tvFrequencyBadge.setText(medicine.getTimesPerDay() + "x Daily");

            // Format timings and meals details (e.g. Dosage: 1 pill - After Breakfast, Dinner)
            binding.tvDosageDetails.setText("Dosage: " + medicine.getDosagePerTime() 
                    + " pill(s) - " + medicine.getTimingRelation() + " " + medicine.getTimingMeals());

            // Display current stock levels
            binding.tvStockStatus.setText("Stock: " + medicine.getRemainingQuantity() 
                    + " / " + medicine.getTotalQuantity() + " pills remaining");

            int daysLeft = medicine.getRemainingDays();

            // Set alert badge styling based on remaining days
            if (medicine.getRemainingQuantity() <= 0 || daysLeft == 0) {
                binding.tvMedicineDaysLeft.setText("Finished!");
                binding.tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.alert_danger));
            } else {
                binding.tvMedicineDaysLeft.setText(daysLeft + " days left");

                if (daysLeft <= 2) {
                    binding.tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.alert_danger));
                } else if (daysLeft <= 10) {
                    binding.tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.alert_warning));
                } else {
                    binding.tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.alert_ok));
                }
            }

            // Bind CRUD Action Listeners
            binding.btnEditMedicine.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditMedicine(medicine);
                }
            });

            binding.btnDeleteMedicine.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteMedicine(medicine);
                }
            });

            // Bind Dose Taken button (deducts stock and updates views)
            binding.btnTakeDose.setEnabled(medicine.getRemainingQuantity() > 0);
            binding.btnTakeDose.setOnClickListener(v -> {
                int newQty = dbHelper.deductMedicineDose(medicine.getId());
                Toast.makeText(context, "Logged dose for " + medicine.getName() + ". Remaining: " + newQty, Toast.LENGTH_SHORT).show();

                // Trigger a run of the notification logic to post alarms immediately if threshold is hit
                AlarmReceiver.checkAllMedicinesAndNotify(context);

                if (actionListener != null) {
                    actionListener.onDoseDeducted();
                }
            });
        }
    }
}
