package com.example.test.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.R;
import com.example.test.Reminder;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private List<Reminder> alarmList;
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    // Constructor
    public AlarmAdapter(List<Reminder> alarmList, OnItemClickListener onItemClickListener, OnDeleteClickListener onDeleteClickListener) {
        this.alarmList = alarmList;
        this.onItemClickListener = onItemClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Reminder reminder = alarmList.get(position);
        holder.titleTextView.setText(reminder.title);
        holder.descriptionTextView.setText(reminder.description);
        String time = String.format("%02d:%02d", reminder.hour, reminder.minute);
        holder.timeTextView.setText(time);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(position));
        holder.deleteButton.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(position));
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;
        ImageButton deleteButton;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.alarmTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.alarmDescriptionTextView);
            timeTextView = itemView.findViewById(R.id.alarmTimeTextView);
            deleteButton = itemView.findViewById(R.id.deleteAlarmButton);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
}
