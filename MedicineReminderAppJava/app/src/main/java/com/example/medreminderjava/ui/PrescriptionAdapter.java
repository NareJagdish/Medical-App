package com.example.medreminderjava.ui;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/* 
 * Feature Removed: Prescription Upload 
 */
public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.DummyViewHolder> {
    @NonNull @Override public DummyViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) { 
        return new DummyViewHolder(new View(p.getContext())); 
    }
    @Override public void onBindViewHolder(@NonNull DummyViewHolder h, int p) {}
    @Override public int getItemCount() { return 0; }
    static class DummyViewHolder extends RecyclerView.ViewHolder { 
        public DummyViewHolder(View v) { super(v); } 
    }
}
