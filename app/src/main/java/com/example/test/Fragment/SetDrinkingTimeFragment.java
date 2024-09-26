package com.example.test.Fragment;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.model.AlarmReceiver;
import com.example.test.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.test.Reminder;
import com.example.test.adapter.AlarmAdapter;

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

        // Gọi hàm loadAlarmHistory() để tải dữ liệu lần đầu tiên
        loadAlarmHistory();

        // Đặt Runnable để tự động làm mới mỗi 1 giây
        refreshRunnable = new Runnable() {
            @Override
            public void run() {

                loadAlarmHistory();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(refreshRunnable);
        deleteOldAlarms();
        resetAlarms();

        return view;
    }



    private void resetAlarms() {
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
                            setAlarm(reminder.hour, reminder.minute, reminder.timestamp);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Lỗi khi khởi tạo lại báo thức: " + databaseError.getMessage());
                }
            });
        }
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
        String description = "Nhắc nhở lúc " + alarmTime;
        String timestamp = String.valueOf(System.currentTimeMillis());

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference newReminderRef = database.child("users").child(uid).child("alarmHistory").child(alarmTime);
            String newReminderId = newReminderRef.getKey();

            Reminder newReminder = new Reminder(newReminderId, title, description, timestamp, hour, minute);

            newReminderRef.setValue(newReminder)
                    .addOnSuccessListener(aVoid -> {
                        alarmHistory.add(newReminder);
                        adapter.notifyItemInserted(alarmHistory.size() - 1);
                        setAlarm(hour, minute, timestamp);
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

        } else {
            Snackbar.make(recyclerView, "Không thể khởi tạo AlarmManager.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void onDeleteClick(int position) {
        // Kiểm tra vị trí hợp lệ trước khi thao tác
        if (position < 0 || position >= alarmHistory.size()) {
            Toast.makeText(getContext(), "Chỉ số không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        Reminder reminderToDelete = alarmHistory.get(position);

        if (reminderToDelete.id == null) {
            Toast.makeText(getContext(), "ID báo thức không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = getUserUid();
        if (uid != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            DatabaseReference reminderRef = database.child("users").child(uid).child("alarmHistory").child(reminderToDelete.id);

            // Hủy báo thức và xóa khỏi Firebase
            cancelAlarm(reminderToDelete.hour, reminderToDelete.minute, reminderToDelete.timestamp);

            reminderRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Kiểm tra và xóa khỏi alarmHistory nếu còn tồn tại
                        if (position < alarmHistory.size()) {
                            alarmHistory.remove(position);
                            adapter.notifyItemRemoved(position);
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
            DatabaseReference remindersRef = database.child("users").child(uid).child("alarmHistory");

            remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    alarmHistory.clear();  // Xóa dữ liệu cũ trước khi tải mới

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Reminder reminder = snapshot.getValue(Reminder.class);
                        if (reminder != null) {
                            alarmHistory.add(reminder);
                        }
                    }

                    // Sắp xếp lại danh sách báo thức
                    Collections.sort(alarmHistory, new Comparator<Reminder>() {
                        @Override
                        public int compare(Reminder o1, Reminder o2) {
                            if (o1.hour != o2.hour) {
                                return Integer.compare(o1.hour, o2.hour);
                            } else {
                                return Integer.compare(o1.minute, o2.minute);
                            }
                        }
                    });

                    adapter.notifyDataSetChanged();  // Cập nhật lại RecyclerView
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseError", "Lỗi khi tải dữ liệu báo thức: " + databaseError.getMessage());
                }
            });
        }
    }



    // Hàm lưu báo thức vào reminder_history và xóa khỏi alarmHistory
    private void saveReminderToHistoryAndDelete(Reminder reminder, DatabaseReference reminderRef) {
        String uid = getUserUid();
        if (uid == null) {
            Log.e("UIDError", "UID người dùng không hợp lệ.");
            return;
        }

        // Lưu vào reminder_history trước khi xóa
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference()
                .child("reminder_history").child(uid).push();

        historyRef.setValue(reminder)
                .addOnSuccessListener(aVoid1 -> {
                    Log.d("FirebaseSuccess", "Báo thức đã được lưu vào reminder_history.");
                    // Xóa khỏi alarmHistory sau khi lưu thành công
                    reminderRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                cancelAlarm(reminder.hour, reminder.minute, reminder.timestamp);
                                Log.d("FirebaseSuccess", "Báo thức đã được xóa khỏi alarmHistory.");
                            })
                            .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi xóa báo thức khỏi alarmHistory: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi lưu báo thức vào reminder_history: " + e.getMessage()));
    }

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
                            Calendar reminderTime = Calendar.getInstance();
                            reminderTime.set(Calendar.HOUR_OF_DAY, reminder.hour);
                            reminderTime.set(Calendar.MINUTE, reminder.minute);

                            if (reminderTime.before(Calendar.getInstance())) {
                                DatabaseReference reminderRef = snapshot.getRef();
                                saveReminderToHistoryAndDelete(reminder, reminderRef);
                            }
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

    private void openEditAlarmDialog(Reminder reminder, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chỉnh sửa báo thức");

        // Sử dụng TimePicker để chọn lại giờ và phút
        TimePicker timePicker = new TimePicker(getContext());
        timePicker.setHour(reminder.hour);
        timePicker.setMinute(reminder.minute);
        builder.setView(timePicker);

        // Nút "Lưu" để xác nhận chỉnh sửa
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            int newHour = timePicker.getHour();
            int newMinute = timePicker.getMinute();

            if (newHour != reminder.hour || newMinute != reminder.minute) {
                String uid = getUserUid();
                if (uid != null) {
                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference reminderRef = database.child("users").child(uid).child("alarmHistory").child(reminder.id);

                    // Hủy báo thức cũ
                    cancelAlarm(reminder.hour, reminder.minute, reminder.timestamp);

                    // Tạo timestamp mới
                    String newTimestamp = String.valueOf(System.currentTimeMillis());
                    String newDescription = "Nhắc nhở lúc " + String.format("%02d:%02d", newHour, newMinute);

                    // Cập nhật toàn bộ thông tin của báo thức với giờ phút mới
                    reminder.hour = newHour;
                    reminder.minute = newMinute;
                    reminder.timestamp = newTimestamp;
                    reminder.description = newDescription;
                    reminder.title = "Nhắc nhở đã sửa";  // Cập nhật tiêu đề nếu cần

                    // Cập nhật Firebase
                    reminderRef.setValue(reminder)
                            .addOnSuccessListener(aVoid -> {
                                // Đặt lại báo thức với giờ và phút mới
                                setAlarm(newHour, newMinute, newTimestamp);

                                // Cập nhật danh sách báo thức và thông báo cho adapter
                                alarmHistory.set(position, reminder);
                                adapter.notifyItemChanged(position);
                            })
                            .addOnFailureListener(e -> Log.e("FirebaseError", "Lỗi khi cập nhật báo thức: " + e.getMessage()));
                } else {
                    Log.e("UIDError", "UID người dùng không hợp lệ.");
                }
            }
        });

        // Nút "Hủy" để thoát khỏi dialog
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        // Hiển thị dialog
        builder.create().show();
    }



    @SuppressLint("UnspecifiedImmutableFlag")
    private void cancelAlarm(int hour, int minute, String timestamp) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        int requestCode = (hour * 10000 + minute * 100 + timestamp.hashCode()) & 0xfffffff;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);  // Hủy báo thức cũ
            Log.d("Alarm", "Báo thức cũ đã được hủy.");
        } else {
            Log.e("AlarmError", "Không thể hủy báo thức cũ.");
        }
    }
}
