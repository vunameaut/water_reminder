package com.example.test;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private FrameLayout frameLayout;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo widget
        frameLayout = findViewById(R.id.frameLayout);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Đặt fragment mặc định nếu chưa có
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HomeFragment()).commit();
        }

        // Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // Lấy id của MenuItem
                int id = menuItem.getItemId();

                // Điều hướng fragment dựa trên id
                if (id == R.id.bottom_nav_home) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HomeFragment()).commit();
                }else if (id == R.id.bottom_nav_history) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new HistoryFragment()).commit();
                } else if (id == R.id.bottom_nav_notification) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new NotificationFragment()).commit();
                } else if (id == R.id.bottom_nav_account) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, new AccountFragment()).commit();
            }
                return true;
            }
        });
        }
}
