package com.s23010749.nutriscan;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

// This activity is a placeholder for app settings.
public class SettingsActivity extends AppCompatActivity {

    Button buttonBackSettings; // Button to go back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings); // Link to our Settings screen design

        buttonBackSettings = findViewById(R.id.buttonBackSettings); // Find the back button

        // Set up what happens when the back button is clicked
        buttonBackSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and go back to the previous one (Home)
            }
        });
    }
}