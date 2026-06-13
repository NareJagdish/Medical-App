package com.example.medreminderjava.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medreminderjava.R;
import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.data.Medicine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdherenceAdapter extends RecyclerView.Adapter<AdherenceAdapter.ViewHolder> {

    private final List<Medicine> medicineList;
    private final DatabaseHelper dbHelper;

    public AdherenceAdapter(List<Medicine> medicineList, Context context) {
        this.medicineList = medicineList;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adherence, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);
        holder.tvMedName.setText(medicine.getName());
        holder.tvMedFrequency.setText(medicine.getTimesPerDay() + " time a day");

        setupWeeklyBubbles(holder.llDaysContainer, medicine.getId());
    }

    private void setupWeeklyBubbles(LinearLayout container, long medicineId) {
        Calendar cal = Calendar.getInstance();
        // Get to Sunday of current week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        for (int i = 0; i < 7; i++) {
            View dayView = container.getChildAt(i);
            if (dayView == null) continue;

            TextView tvDayName = dayView.findViewById(R.id.tvDayName);
            ImageView ivStatus = dayView.findViewById(R.id.ivStatus);

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            tvDayName.setText(dayFormat.format(cal.getTime()));

            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = dbFormat.format(cal.getTime());
            String status = dbHelper.getLogStatus(medicineId, dateStr);

            // Simple logic for icons matching the image (Check for Taken, X for Missed)
            if (status.equalsIgnoreCase("Taken")) {
                ivStatus.setImageResource(R.drawable.ic_status_taken);
                ivStatus.setBackgroundTintList(null);
                ivStatus.setColorFilter(null);
                ivStatus.setVisibility(View.VISIBLE);
            } else if (status.equalsIgnoreCase("Missed")) {
                ivStatus.setImageResource(R.drawable.ic_status_missed);
                ivStatus.setBackgroundTintList(null);
                ivStatus.setColorFilter(null);
                ivStatus.setVisibility(View.VISIBLE);
            } else {
                ivStatus.setImageResource(0);
                ivStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE"))); // Grey
                ivStatus.setColorFilter(null);
                ivStatus.setVisibility(View.VISIBLE);
            }

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvMedFrequency;
        LinearLayout llDaysContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvMedFrequency = itemView.findViewById(R.id.tvMedFrequency);
            llDaysContainer = itemView.findViewById(R.id.llDaysContainer);
        }
    }
}
