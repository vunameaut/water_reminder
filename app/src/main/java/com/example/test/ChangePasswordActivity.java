package com.example.test;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
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
    private CheckBox showPasswordCheckbox;

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
        showPasswordCheckbox = findViewById(R.id.show_password_checkbox);

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Change password button functionality
        changePasswordButton.setOnClickListener(v -> changePassword());

        // Show/hide password functionality
        showPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show passwords
                oldPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                newPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                confirmPassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                // Hide passwords
                oldPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                newPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                confirmPassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });
    }

    private void changePassword() {
        String oldPassword = oldPassEditText.getText().toString().trim();
        String newPassword = newPassEditText.getText().toString().trim();
        String confirmPassword = confirmPassEditText.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(ChangePasswordActivity.this, "Please enter complete information", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            resultTextView.setText("New password and confirmation password do not match.");
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
                            resultTextView.setText("Password changed successfully!");
                            resultTextView.setTextColor(getResources().getColor(android.R.color.black));
                            finish();
                        } else {
                            resultTextView.setText("Change password failed: " + updateTask.getException().getMessage());
                        }
                    });
                } else {
                    resultTextView.setText("Old password is incorrect.");
                }
            });
        } else {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
        }
    }
}