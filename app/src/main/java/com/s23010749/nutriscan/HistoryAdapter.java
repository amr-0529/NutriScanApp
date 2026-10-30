package com.s23010749.nutriscan;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<ScanHistoryItem> historyList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ScanHistoryItem item);
    }

    public HistoryAdapter(Context context, List<ScanHistoryItem> historyList, OnItemClickListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanHistoryItem item = historyList.get(position);
        holder.tvFoodName.setText(item.foodName);
        holder.tvDateLocation.setText(item.date);
        holder.tvStatus.setText(item.healthStatus);

        if ("Healthy".equals(item.healthStatus)) {
            holder.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
            holder.tvStatus.setTextColor(Color.parseColor("#10B981"));
        } else if ("Unhealthy".equals(item.healthStatus)) {
            holder.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B"));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvStatus, tvDateLocation;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateLocation = itemView.findViewById(R.id.tvDateLocation);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }
    }
}