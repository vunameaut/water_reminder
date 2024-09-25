package com.example.test;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {
    private final List<SettingFragment.Reminder> alarmHistory;
    private final OnAlarmClickListener onAlarmClickListener;
    private final OnDeleteClickListener onDeleteClickListener;

    public AlarmAdapter(List<SettingFragment.Reminder> alarmHistory, OnAlarmClickListener onAlarmClickListener, OnDeleteClickListener onDeleteClickListener) {
        this.alarmHistory = alarmHistory;
        this.onAlarmClickListener = onAlarmClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView alarmTitle;
        TextView alarmTime;
        Button deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            alarmTitle = itemView.findViewById(R.id.alarmTitle);
            alarmTime = itemView.findViewById(R.id.alarmTime);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onAlarmClickListener != null) {
                    onAlarmClickListener.onAlarmClick(position);
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClick(position);
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingFragment.Reminder reminder = alarmHistory.get(position);
        holder.alarmTitle.setText(reminder.title);
        holder.alarmTime.setText(reminder.description);
    }

    @Override
    public int getItemCount() {
        return alarmHistory.size();
    }

    public interface OnAlarmClickListener {
        void onAlarmClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
}
