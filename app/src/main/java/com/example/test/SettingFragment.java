package com.example.test;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SettingFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlarmAdapter adapter;
    private List<Reminder> alarmHistory;
    private TimePicker timePicker;
    private Button setAlarmButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_PERMISSION_GRANTED = "isPermissionGranted";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        recyclerView = view.findViewById(R.id.alarmHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        alarmHistory = new ArrayList<>();
        adapter = new AlarmAdapter(alarmHistory, position -> {
            // Xử lý sự kiện click vào item
            Reminder clickedAlarm = alarmHistory.get(position);
            // Thực hiện hành động cần thiết với clickedAlarm
        }, this::onDeleteClick);  // Thêm listener cho nút xóa
        recyclerView.setAdapter(adapter);

        timePicker = view.findViewById(R.id.timePicker);
        setAlarmButton = view.findViewById(R.id.setAlarmButton);

        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean isPermissionGranted = sharedPreferences.getBoolean(KEY_PERMISSION_GRANTED, false);

        setAlarmButton.setOnClickListener(v -> {
            if (isPermissionGranted || checkExactAlarmPermission()) {
                setAlarmProcess();
            } else {
                requestExactAlarmPermission();
            }
        });

        loadAlarmHistory();

        return view;
    }

    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private String getUserUid() {
        return sharedPreferences.getString(KEY_UID, null);
    }

    private void setAlarmProcess() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String alarmTime = String.format("%02d:%02d", hour, minute);

        Calendar now = Calendar.getInstance();
        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(Calendar.HOUR_OF_DAY, hour);
        selectedTime.set(Calendar.MINUTE, minute);
        selectedTime.set(Calendar.SECOND, 0);

        if (selectedTime.before(now)) {
            Log.d("Alarm", "Thời gian chọn đã qua, không thể đặt báo thức.");
            return;
        }

        for (Reminder reminder : alarmHistory) {
            if (reminder.hour == hour && reminder.minute == minute) {
                Log.d("Alarm", "Báo thức đã tồn tại cho " + alarmTime);
                return;
            }
        }

        String title = "Nhắc nhở mới";
        String description = "Nhắc nhở " + alarmTime;
        String timestamp = String.valueOf(System.currentTimeMillis());

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference newReminderRef = database.child("reminder_history").child(uid).push(); // Tạo ID tự động
            String newReminderId = newReminderRef.getKey(); // Lấy ID

            Reminder newReminder = new Reminder(newReminderId, title, description, timestamp, hour, minute);

            newReminderRef.setValue(newReminder)
                    .addOnSuccessListener(aVoid -> {
                        alarmHistory.add(newReminder);
                        adapter.notifyItemInserted(alarmHistory.size() - 1);
                        setAlarm(hour, minute, timestamp);
                        loadAlarmHistory();  // Gọi lại để làm mới danh sách từ Firebase
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi lưu báo thức: " + e.getMessage()));


        } else {
            Log.e("UIDError", "UID người dùng không hợp lệ.");
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setAlarm(int hour, int minute, String timestamp) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        int requestCode = (hour * 10000 + minute * 100 + timestamp.hashCode()) & 0xfffffff;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d("Alarm", "Báo thức đã được đặt cho " + hour + ":" + minute);
        } else {
            Log.e("AlarmManagerError", "AlarmManager không khởi tạo được.");
        }
    }

    private void onDeleteClick(int position) {
        if (position < 0 || position >= alarmHistory.size()) {
            Log.e("DeleteError", "Chỉ số không hợp lệ: " + position);
            return;
        }

        Reminder reminderToDelete = alarmHistory.get(position);

        if (reminderToDelete.id == null) {
            Log.e("DeleteError", "ID báo thức không hợp lệ.");
            return;
        }

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference reminderRef = database.child("reminder_history").child(uid).child(reminderToDelete.id);

            reminderRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (position >= 0 && position < alarmHistory.size()) {
                            alarmHistory.remove(position);
                            adapter.notifyItemRemoved(position);
                            loadAlarmHistory();
                            Log.d("Alarm", "Báo thức đã được xóa.");
                        } else {
                            Log.e("DeleteError", "Chỉ số không hợp lệ sau khi xóa: " + position);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi xóa báo thức: " + e.getMessage()));
        } else {
            Log.e("UIDError", "UID người dùng không hợp lệ.");
        }
    }


    private void loadAlarmHistory() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("reminder_history").child(uid);

            remindersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    alarmHistory.clear();

                    Calendar now = Calendar.getInstance();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);

                        if (reminder != null && reminder.id != null) { // Kiểm tra ID không phải là null
                            Calendar alarmCalendar = Calendar.getInstance();
                            alarmCalendar.set(Calendar.HOUR_OF_DAY, reminder.hour);
                            alarmCalendar.set(Calendar.MINUTE, reminder.minute);
                            alarmCalendar.set(Calendar.SECOND, 0);

                            if (alarmCalendar.after(now)) {
                                alarmHistory.add(reminder);
                            }
                        }
                    }

                    sortAndNotify();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Lỗi khi tải báo thức: " + databaseError.getMessage());
                }
            });
        }
    }

    private void sortAndNotify() {
        Collections.sort(alarmHistory, new Comparator<Reminder>() {
            @Override
            public int compare(Reminder r1, Reminder r2) {
                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();

                cal1.set(Calendar.HOUR_OF_DAY, r1.hour);
                cal1.set(Calendar.MINUTE, r1.minute);
                cal2.set(Calendar.HOUR_OF_DAY, r2.hour);
                cal2.set(Calendar.MINUTE, r2.minute);

                return cal1.compareTo(cal2);
            }
        });
        adapter.notifyDataSetChanged();
    }

    public static class Reminder {
        public String id; // Thêm thuộc tính ID
        public String title;
        public String description;
        public String timestamp;
        public int hour;
        public int minute;

        public Reminder() {
            // Constructor không có tham số
        }

        public Reminder(String id, String title, String description, String timestamp, int hour, int minute) {
            this.id = id; // Khởi tạo ID
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.hour = hour;
            this.minute = minute;
        }
    }
}
