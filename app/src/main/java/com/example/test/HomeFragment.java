package com.example.test;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView hydrationText;
    private TextView goalText, hello, time;
    private Button btnSmall, btnLarge;
    private int hydration = 0; // initial hydration in ml

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize the views
        progressBar = view.findViewById(R.id.progressBar);
        hydrationText = view.findViewById(R.id.hydration_text);
        goalText = view.findViewById(R.id.goal_text);
        hello = view.findViewById(R.id.hello);
        time = view.findViewById(R.id.time);
        btnSmall = view.findViewById(R.id.btn_small);
        btnLarge = view.findViewById(R.id.btn_large);

        btnSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWater(500); // 500 ml
            }
        });

        btnLarge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWater(1000);
            }
        });


        // Update the date and time
        updateDateTime();

        return view;
    }

    private void updateDateTime() {
        // Get the current date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Set the date to the TextView
        time.setText(currentDate);
    }

    private void addWater(int amount) {
        hydration += amount;
        int progress = hydration / 20;
        progressBar.setProgress(progress);
        hydrationText.setText(hydration + " ml");
        goalText.setText("You have achieved\n" + progress + "% of your goal today");
    }

    private void removeWater(int amount) {
        hydration -= amount;
        int progress = hydration / 30; // Assuming goal is 3000 ml
        progressBar.setProgress(progress);
        hydrationText.setText(hydration + " ml");
        goalText.setText("You have achieved\n" + progress + "% of your goal today");
    }
}
