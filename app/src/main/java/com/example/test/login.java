package com.example.test;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private CheckBox rememberMeCheckBox;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.input_username);
        passwordEditText = findViewById(R.id.input_pass);
        loginButton = findViewById(R.id.sign_in_button);
        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if the UID is already remembered
        String savedUID = sharedPreferences.getString("uid", null);
        if (savedUID != null) {
            // UID found, auto-login
            Intent intent = new Intent(login.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Toggle password visibility
        ImageButton togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        togglePasswordVisibility.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // Redirect to signup activity
        TextView signUpRedirect = findViewById(R.id.sign_up_redirect);
        signUpRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(login.this, signup.class);
            startActivity(intent);
        });

        // Login button event
        loginButton.setOnClickListener(v -> loginUser());
    }
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(login.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng nhập...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(login.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            String uid = user.getUid(); // Lấy UID từ Firebase

                            // Lưu UID sau khi đăng nhập thành công vào SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("uid", uid);  // Luôn lưu UID
                            editor.putBoolean("remember", rememberMeCheckBox.isChecked());  // Lưu trạng thái "Remember Me"
                            editor.apply();

                            // Chuyển UID qua MainActivity qua Intent (Optional)
                            Intent intent = new Intent(login.this, MainActivity.class);
                            intent.putExtra("uid", uid);  // Truyền UID qua Intent
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Đăng nhập thất bại
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                        Toast.makeText(login.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
