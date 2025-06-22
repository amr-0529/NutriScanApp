package com.s23010749.nutriscan;

import android.content.Intent;
import android.graphics.Color; // To use colors like green, red, yellow
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast; // For showing quick messages

import androidx.appcompat.app.AppCompatActivity; // Base class for activities

// This screen shows the user the nutrition results after a scan.
public class ResultActivity extends AppCompatActivity {

    // UI elements from activity_result.xml
    TextView textViewFoodName, textViewHealthStatus, textViewSugar, textViewSalt, textViewFat, textViewTips, textViewLocationDate;
    View healthStatusIndicator; // The colored bar that indicates health status
    Button buttonBack; // Button to go back to home

    DatabaseHelper databaseHelper; // To save scan data to our local database

    // Variables to temporarily store the data received from ScanActivity
    String foodName, sugarLevel, saltLevel, fatLevel, healthStatus, tipsMessage, scanDate, scanLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result); // Connects to our screen design

        // Link the Java variables to the actual UI elements using their IDs
        textViewFoodName = findViewById(R.id.textViewFoodName);
        textViewHealthStatus = findViewById(R.id.textViewHealthStatus);
        healthStatusIndicator = findViewById(R.id.healthStatusIndicator);
        textViewSugar = findViewById(R.id.textViewSugar);
        textViewSalt = findViewById(R.id.textViewSalt);
        textViewFat = findViewById(R.id.textViewFat);
        textViewTips = findViewById(R.id.textViewTips);
        textViewLocationDate = findViewById(R.id.textViewLocationDate); // Linked the new TextView!
        buttonBack = findViewById(R.id.buttonBack);

        databaseHelper = new DatabaseHelper(this); // Initialize our database helper

        // Get the data that was sent from the ScanActivity using an Intent
        Intent intent = getIntent();
        foodName = intent.getStringExtra("food_name");
        sugarLevel = intent.getStringExtra("sugar_level");
        saltLevel = intent.getStringExtra("salt_level");
        fatLevel = intent.getStringExtra("fat_level");
        healthStatus = intent.getStringExtra("health_status");
        tipsMessage = intent.getStringExtra("tips_message");
        scanDate = intent.getStringExtra("scan_date");
        scanLocation = intent.getStringExtra("scan_location");

        // Display the received data on the screen
        textViewFoodName.setText("Food Name: " + foodName);
        textViewHealthStatus.setText("Status: " + healthStatus);
        textViewSugar.setText("Sugar: " + sugarLevel);
        textViewSalt.setText("Salt: " + saltLevel);
        textViewFat.setText("Fat: " + fatLevel);
        textViewTips.setText("Tips: " + tipsMessage);
        textViewLocationDate.setText("Scanned: " + scanDate + " at " + scanLocation); // Display location and date

        // Change the health status bar color based on the determined health status
        if ("Healthy".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#4CAF50")); // Green for healthy
        } else if ("Unhealthy".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#F44336")); // Red for unhealthy
        } else if ("Moderate".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#FFC107")); // Orange/Yellow for moderate
        } else {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#9E9E9E")); // Gray for unknown or N/A
        }

        // What happens when the "Back to Home" button is clicked
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the current scan result to the database!
                long result = databaseHelper.addScanResult(
                        foodName, healthStatus, sugarLevel, saltLevel, fatLevel, tipsMessage, scanDate, scanLocation
                );

                if (result != -1) { // If save was successful
                    Toast.makeText(ResultActivity.this, "Scan saved to history!", Toast.LENGTH_SHORT).show();
                } else { // If save failed
                    Toast.makeText(ResultActivity.this, "Failed to save scan history.", Toast.LENGTH_SHORT).show();
                }

                // After saving, go back to the Home screen
                Intent backToHome = new Intent(ResultActivity.this, HomeActivity.class);
                // These flags clear previous activities so user can't just press back to return here
                backToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backToHome);
                finish(); // Close this result screen
            }
        });
    }
}