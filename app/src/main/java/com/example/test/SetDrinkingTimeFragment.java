package com.example.test;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.example.test.adapter.AlarmAdapter;
import com.example.test.Reminder;
import com.google.android.material.snackbar.Snackbar;
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

public class SetDrinkingTimeFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlarmAdapter adapter;
    private List<Reminder> alarmHistory;
    private TimePicker timePicker;
    private Button setAlarmButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_PERMISSION_GRANTED = "isPermissionGranted";
    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_drinking_time, container, false);

        recyclerView = view.findViewById(R.id.alarmHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        alarmHistory = new ArrayList<>();
        adapter = new AlarmAdapter(alarmHistory, position -> {
            Reminder clickedAlarm = alarmHistory.get(position);
            openEditAlarmDialog(clickedAlarm, position);
        }, this::onDeleteClick);
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
        deleteOldAlarms();  // Gọi hàm xóa báo thức cũ ngay khi khởi chạy

        // Đặt Handler để refresh mỗi 3 giây
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Gọi lại loadAlarmHistory để tải lại dữ liệu từ Firebase
                loadAlarmHistory();
                // Lặp lại sau 3 giây
                handler.postDelayed(this, 3000);
            }
        };
        // Bắt đầu refresh mỗi 3 giây
        handler.post(refreshRunnable);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Dừng handler khi Fragment bị hủy
        handler.removeCallbacks(refreshRunnable);
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

    // Quá trình đặt báo thức
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
        String description = "Nhắc nhở lúc " + alarmTime;
        String timestamp = String.valueOf(System.currentTimeMillis());

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            // Lưu lịch sử báo thức rõ ràng hơn
            DatabaseReference newReminderRef = database.child("users").child(uid).child("alarmHistory").child(alarmTime);
            String newReminderId = newReminderRef.getKey();

            Reminder newReminder = new Reminder(newReminderId, title, description, timestamp, hour, minute);

            newReminderRef.setValue(newReminder)
                    .addOnSuccessListener(aVoid -> {
                        alarmHistory.add(newReminder);
                        adapter.notifyItemInserted(alarmHistory.size() - 1); // Thêm mới báo thức vào RecyclerView
                        setAlarm(hour, minute, timestamp); // Đặt báo thức
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
            Snackbar.make(recyclerView, "Báo thức đã được đặt cho " + hour + ":" + minute, Snackbar.LENGTH_LONG).show();

        } else {
            Snackbar.make(recyclerView, "Không thể khởi tạo AlarmManager.", Snackbar.LENGTH_LONG).show();

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
            DatabaseReference reminderRef = database.child("users").child(uid).child("reminder_history").child(reminderToDelete.id);

            // Hủy báo thức cũ
            cancelAlarm(reminderToDelete.hour, reminderToDelete.minute, reminderToDelete.timestamp);

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

    private void openEditAlarmDialog(Reminder reminder, int position) {
        // Tạo dialog chỉnh sửa báo thức
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chỉnh sửa báo thức");

        // Sử dụng layout TimePicker để chọn lại giờ và phút
        TimePicker timePicker = new TimePicker(getContext());
        timePicker.setHour(reminder.hour);
        timePicker.setMinute(reminder.minute);
        builder.setView(timePicker);

        // Nút "Lưu" để xác nhận chỉnh sửa
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            int newHour = timePicker.getHour();
            int newMinute = timePicker.getMinute();

            // Kiểm tra nếu giờ và phút mới khác với giờ phút hiện tại
            if (newHour != reminder.hour || newMinute != reminder.minute) {
                // Xóa báo thức cũ
                deleteOldAlarm(reminder, position);

                // Tạo báo thức mới với giờ và phút mới
                createNewAlarm(newHour, newMinute, position);
            }
        });

        // Nút "Hủy" để thoát khỏi dialog
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        // Hiển thị dialog
        builder.create().show();
    }

    private void deleteOldAlarm(Reminder reminder, int position) {
        String uid = getUserUid();
        if (uid != null && reminder.id != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference reminderRef = database.child("users").child(uid).child("alarmHistory").child(reminder.id);

            // Hủy báo thức cũ
            cancelAlarm(reminder.hour, reminder.minute, reminder.timestamp);

            // Xóa báo thức cũ khỏi Firebase
            reminderRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Xóa báo thức khỏi danh sách hiển thị
                        alarmHistory.remove(position);
                        adapter.notifyItemRemoved(position);
                        Log.d("Alarm", "Báo thức cũ đã được xóa.");
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi xóa báo thức: " + e.getMessage()));
        }
    }

    private void createNewAlarm(int newHour, int newMinute, int position) {
        String newId = String.format("%02d:%02d", newHour, newMinute); // Tạo id mới từ giờ và phút mới
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference newReminderRef = database.child("users").child(uid).child("alarmHistory").child(newId);

            String title = "Nhắc nhở mới";
            String description = "Nhắc nhở lúc " + newId;
            String timestamp = String.valueOf(System.currentTimeMillis());

            Reminder newReminder = new Reminder(newId, title, description, timestamp, newHour, newMinute);

            newReminderRef.setValue(newReminder)
                    .addOnSuccessListener(aVoid -> {
                        // Thêm báo thức mới vào danh sách hiển thị
                        alarmHistory.add(position, newReminder);
                        adapter.notifyItemInserted(position);
                        Log.d("Alarm", "Báo thức mới đã được tạo.");

                        // Đặt lại báo thức mới
                        setAlarm(newHour, newMinute, timestamp);  // Đặt báo thức mới
                    })
                    .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi tạo báo thức: " + e.getMessage()));
        }
    }

    private void cancelAlarm(int hour, int minute, String timestamp) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        int requestCode = (hour * 10000 + minute * 100 + timestamp.hashCode()) & 0xfffffff;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);  // Hủy báo thức cũ
            Log.d("Alarm", "Báo thức cũ đã được hủy.");
        }
    }

    private void loadAlarmHistory() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    alarmHistory.clear();

                    Calendar now = Calendar.getInstance();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);

                        if (reminder != null) {
                            Calendar reminderTime = Calendar.getInstance();
                            reminderTime.set(Calendar.HOUR_OF_DAY, reminder.hour);
                            reminderTime.set(Calendar.MINUTE, reminder.minute);

                            if (reminderTime.before(now)) {
                                // Xóa báo thức đã qua thời gian
                                snapshot.getRef().removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Alarm", "Báo thức đã được xóa do hết hạn.");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirebaseError", "Lỗi khi xóa báo thức: " + e.getMessage());
                                        });
                            } else {
                                alarmHistory.add(reminder);
                            }
                        }
                    }

                    Collections.sort(alarmHistory, new Comparator<Reminder>() {
                        @Override
                        public int compare(Reminder r1, Reminder r2) {
                            int time1 = r1.hour * 60 + r1.minute;
                            int time2 = r2.hour * 60 + r2.minute;
                            return Integer.compare(time1, time2);
                        }
                    });

                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Lỗi khi tải lịch sử báo thức: " + databaseError.getMessage());
                }
            });
        }
    }

    // Hàm xóa các báo thức đã qua và lưu vào reminder_history
    private void deleteOldAlarms() {
        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);

                        if (reminder != null) {
                            // Xóa tất cả báo thức cũ
                            snapshot.getRef().removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Lưu báo thức đã xóa vào bảng reminder_history
                                        DatabaseReference historyRef = database.child("reminder_history").child(uid).push();
                                        historyRef.setValue(reminder)
                                                .addOnSuccessListener(aVoid1 -> Log.d("Alarm", "Báo thức đã được lưu vào reminder_history."))
                                                .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi lưu báo thức vào reminder_history: " + e.getMessage()));
                                    })
                                    .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi xóa báo thức: " + e.getMessage()));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Lỗi khi truy xuất dữ liệu báo thức: " + databaseError.getMessage());
                }
            });
        }
    }


}
