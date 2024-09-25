package com.example.test;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Edit_Acc extends AppCompatActivity {

    private EditText editName, editEmail, editPhone;
    private Button btnSave;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_acc);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        btnSave = findViewById(R.id.btn_save);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        // Load current user info
        loadUserInfo();

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update user information in Firebase Realtime Database
            databaseRef.child("username").setValue(name);
            databaseRef.child("email").setValue(email);
            databaseRef.child("phone").setValue(phone);

            Toast.makeText(this, "Thông tin tài khoản đã được cập nhật", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity sau khi lưu
        });
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);

                        editName.setText(username);
                        editEmail.setText(email);
                        editPhone.setText(phone != null ? phone : "");
                    } else {
                        Toast.makeText(Edit_Acc.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(Edit_Acc.this, "Lỗi khi đọc dữ liệu: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
