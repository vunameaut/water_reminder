package com.example.test;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText oldPassEditText, newPassEditText, confirmPassEditText;
    private Button changePasswordButton;
    private TextView resultTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        oldPassEditText = findViewById(R.id.input_old_pass);
        newPassEditText = findViewById(R.id.input_new_pass);
        confirmPassEditText = findViewById(R.id.input_confirm_new_pass);
        changePasswordButton = findViewById(R.id.change_password_button);
        resultTextView = findViewById(R.id.change_password_result);
        ImageButton backButton = findViewById(R.id.button_back);

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Change password button functionality
        changePasswordButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = oldPassEditText.getText().toString().trim();
        String newPassword = newPassEditText.getText().toString().trim();
        String confirmPassword = confirmPassEditText.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(ChangePasswordActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            resultTextView.setText("Mật khẩu mới và mật khẩu xác nhận không khớp.");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Re-authenticate the user with the old password before changing it
            mAuth.signInWithEmailAndPassword(user.getEmail(), oldPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Change the user's password
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            resultTextView.setText("Đổi mật khẩu thành công!");
                            resultTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else {
                            resultTextView.setText("Đổi mật khẩu thất bại: " + updateTask.getException().getMessage());
                        }
                    });
                } else {
                    resultTextView.setText("Mật khẩu cũ không chính xác.");
                }
            });
        } else {
            Toast.makeText(this, "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
        }
    }
}
