package com.example.test;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

// ... (other necessary imports)

public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView avatarImageView;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private Uri selectedImageUri; // To store the selected image URI

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        avatarImageView = view.findViewById(R.id.avatarImageView);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Set click listener on avatarImageView
        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        // Load avatar image from Firebase Storage
        loadAvatarImage();

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

            // Display the selected image in the ImageView (you can use Picasso here too)
            Picasso.get().load(selectedImageUri).into(avatarImageView);

            // Upload the image to Firebase Storage
            uploadAvatarImage(selectedImageUri);
        }
    }


        // ... (các biến và phương thức khác)

        private void loadAvatarImage() {
            StorageReference avatarRef = storageRef.child("avatar/" + getCurrentUserId() + ".jpg"); // Điều chỉnh tên tệp nếu cần

            avatarRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Tải ảnh vào ImageView sử dụng Picasso
                    Picasso.get().load(uri).into(avatarImageView);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Xử lý lỗi (ví dụ: không tìm thấy ảnh)
                    // Bạn có thể hiển thị ảnh đại diện mặc định hoặc thông báo lỗi
                    Toast.makeText(getContext(), "Không thể tải ảnh đại diện", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void uploadAvatarImage(Uri imageUri) {
            StorageReference avatarRef = storageRef.child("avatars/" + getCurrentUserId() + ".jpg");

            avatarRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Tải ảnh lên thành công
                            Toast.makeText(getContext(), "Đã tải lên ảnh đại diện", Toast.LENGTH_SHORT).show();

                            // Bạn có thể tải lại ảnh đại diện ở đây để cập nhật thay đổi ngay lập tức
                            loadAvatarImage();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Xử lý lỗi khi tải lên không thành công
                            Toast.makeText(getContext(), "Tải lên ảnh đại diện thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // ... (Triển khai phương thức getCurrentUserId())

        private String getCurrentUserId() {
            // Thay thế bằng logic thực tế của bạn để lấy ID của người dùng hiện tại
            // Điều này có thể liên quan đến việc sử dụng Firebase Authentication hoặc các hệ thống quản lý người dùng khác
            // Hiện tại, giả sử một placeholder đơn giản
            return "user123";
        }

}