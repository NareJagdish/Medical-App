package com.example.medreminderjava.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.Medicine;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
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
        private final TextView tvMedicineName, tvFrequencyBadge, tvDosageDetails, tvStockStatus, tvMedicineDaysLeft;
        private final View btnEditMedicine, btnDeleteMedicine;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvFrequencyBadge = itemView.findViewById(R.id.tvFrequencyBadge);
            tvDosageDetails = itemView.findViewById(R.id.tvDosageDetails);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
            tvMedicineDaysLeft = itemView.findViewById(R.id.tvMedicineDaysLeft);
            btnEditMedicine = itemView.findViewById(R.id.btnEditMedicine);
            btnDeleteMedicine = itemView.findViewById(R.id.btnDeleteMedicine);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Medicine medicine) {
            Context context = itemView.getContext();

            tvMedicineName.setText(medicine.getName());
            tvFrequencyBadge.setText(medicine.getTimesPerDay() + "x Daily");
            tvDosageDetails.setText("Dosage: " + medicine.getDosagePerTime() + " pill(s) - " + medicine.getTimingRelation() + " " + medicine.getTimingMeals());
            tvStockStatus.setText("Stock: " + medicine.getRemainingQuantity() + " / " + medicine.getTotalQuantity() + " pills remaining");

            int daysLeft = medicine.getRemainingDays();
            if (medicine.getRemainingQuantity() <= 0 || daysLeft == 0) {
                tvMedicineDaysLeft.setText("Finished!");
                tvMedicineDaysLeft.setVisibility(View.VISIBLE);
                tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.error));
            } else if (daysLeft <= 10) {
                tvMedicineDaysLeft.setText(daysLeft + " days left");
                tvMedicineDaysLeft.setVisibility(View.VISIBLE);
                if (daysLeft <= 2) tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.error));
                else tvMedicineDaysLeft.setTextColor(ContextCompat.getColor(context, R.color.warning));
            } else {
                tvMedicineDaysLeft.setVisibility(View.GONE);
            }

            btnEditMedicine.setOnClickListener(v -> { if (actionListener != null) actionListener.onEditMedicine(medicine); });
            btnDeleteMedicine.setOnClickListener(v -> { if (actionListener != null) actionListener.onDeleteMedicine(medicine); });
        }
    }
}
