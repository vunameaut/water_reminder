package com.example.test;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class signup extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton;
    private TextView signUpRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_singup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.singup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Realtime Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Linking UI components
        usernameEditText = findViewById(R.id.input_username);
        emailEditText = findViewById(R.id.input_email);
        passwordEditText = findViewById(R.id.input_pass);
        confirmPasswordEditText = findViewById(R.id.input_confirm_pass);
        signUpButton = findViewById(R.id.sign_up_button);
        signUpRedirect = findViewById(R.id.already_have_account);

        // Handle sign up button click
        signUpButton.setOnClickListener(v -> registerUser());

        // Redirect to login if user already has an account
        signUpRedirect.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(signup.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(signup.this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị hộp thoại chờ
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng ký...");
        progressDialog.show();

        // Đăng ký người dùng với email và mật khẩu
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Đăng ký thành công, lưu thông tin người dùng vào Realtime Database
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            User userProfile = new User(user.getUid(), username, email,"");
                            mDatabase.child("users").child(user.getUid()).setValue(userProfile)
                                    .addOnCompleteListener(databaseTask -> {
                                        if (databaseTask.isSuccessful()) {
                                            // Gửi email xác thực
                                            user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                                                if (verificationTask.isSuccessful()) {
                                                    Toast.makeText(signup.this, "Đăng ký thành công! Vui lòng xác thực email trước khi đăng nhập.", Toast.LENGTH_LONG).show();
                                                    Intent intent = new Intent(signup.this, login.class);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Toast.makeText(signup.this, "Không thể gửi email xác thực: " + verificationTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(signup.this, "Lưu thông tin người dùng thất bại: " + databaseTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        // Nếu đăng ký thất bại, hiển thị thông báo lỗi
                        Toast.makeText(signup.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    public static class User {
        public String uid;
        public String username;
        public String email;
        public String phone;  // Thay đổi kiểu dữ liệu thành String

        public User(String uid, String username, String email, String phone) {
            this.uid = uid;
            this.username = username;
            this.email = email;
            this.phone = phone;
        }
    }
}
