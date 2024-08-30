package com.example.test;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView avatarImageView;
    private TextView nameTextView, emailTextView, phoneTextView;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference databaseRef;
    private FirebaseAuth mAuth;

    private Button edit_user, btn_log_out;

    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        avatarImageView = view.findViewById(R.id.avatarImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);

        btn_log_out = view.findViewById(R.id.btnSignOut);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Đặt OnClickListener cho avatarImageView để mở trình chọn ảnh
        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        // Đặt OnClickListener cho nút đăng xuất
        btn_log_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });

        // Tải ảnh đại diện từ Firebase Storage
        loadAvatarImage();
        // Tải thông tin người dùng từ Firebase Realtime Database
        loadUserInfo();

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Hiển thị ảnh đã chọn lên ImageView sử dụng Picasso
            Picasso.get().load(selectedImageUri).into(avatarImageView);

            // Tải ảnh lên Firebase Storage
            uploadAvatarImage(selectedImageUri);
        }
    }

    private void loadAvatarImage() {
        String userId = getUserId();
        if (userId != null) {
            StorageReference avatarRef = storageRef.child("avatar/" + userId + ".jpg");

            avatarRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Tải ảnh đại diện vào ImageView sử dụng Picasso với CircleTransform
                    Picasso.get()
                            .load(uri)
                            .transform(new CircleTransform()) // Áp dụng transform hình tròn
                            .into(avatarImageView);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Xử lý lỗi (ví dụ: ảnh không tồn tại)
                    Toast.makeText(getContext(), "Không thể tải ảnh đại diện: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void uploadAvatarImage(Uri imageUri) {
        String userId = getUserId();
        if (userId != null) {
            StorageReference avatarRef = storageRef.child("avatar/" + userId + ".jpg");

            avatarRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Ảnh đã được tải lên thành công
                            Toast.makeText(getContext(), "Đã tải lên ảnh đại diện", Toast.LENGTH_SHORT).show();

                            // Tải lại ảnh đại diện để hiển thị ngay lập tức
                            loadAvatarImage();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xử lý lỗi khi tải lên thất bại
                            Toast.makeText(getContext(), "Tải lên ảnh đại diện thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadUserInfo() {
        String userId = getUserId();
        if (userId != null) {
            DatabaseReference userRef = databaseRef.child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        String phone = dataSnapshot.child("phone").getValue(String.class);

                        // Hiển thị thông tin lên các TextView
                        nameTextView.setText("Name: " + username);
                        emailTextView.setText("Email: " + email);
                        phoneTextView.setText("Phone: " + (phone != null ? phone : ""));
                    } else {
                        // Xử lý trường hợp không tìm thấy thông tin người dùng
                        Toast.makeText(getContext(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Xử lý lỗi khi đọc dữ liệu từ Realtime Database
                    Toast.makeText(getContext(), "Lỗi khi đọc dữ liệu: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else {
            return null;
        }
    }

    private void signOut() {
        mAuth.signOut(); // Đăng xuất người dùng

        // Hiển thị thông báo
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        // Chuyển về màn hình đăng nhập hoặc khởi động lại Activity
        Intent intent = new Intent(getActivity(), login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
