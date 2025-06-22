package com.s23010749.nutriscan;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

// This activity displays information about the NutriScan app.
public class AboutActivity extends AppCompatActivity {

    Button buttonBackAbout; // Button to go back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about); // Link to our About screen design

        buttonBackAbout = findViewById(R.id.buttonBackAbout); // Find the back button

        // Set up what happens when the back button is clicked
        buttonBackAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and go back to the previous one (Home)
            }
        });
    }
}