package com.s23010749.nutriscan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // For showing confirmation dialog
import androidx.appcompat.app.AppCompatActivity;

// This activity shows the full details of a single scan history item.
public class HistoryDetailActivity extends AppCompatActivity {

    // UI elements
    TextView textViewDetailFoodName, textViewDetailHealthStatus, textViewDetailSugar,
            textViewDetailSalt, textViewDetailFat, textViewDetailTips, textViewDetailLocationDate;
    View detailHealthStatusIndicator;
    Button buttonViewOnMap, buttonDeleteScan, buttonBackFromDetail;

    DatabaseHelper databaseHelper; // For database operations (like delete)

    // Variables to store the data passed from HistoryActivity
    private long scanId;
    private String foodName, healthStatus, sugar, salt, fat, tips, date, location;
    private double latitude = 0.0, longitude = 0.0; // To store lat/lon if we can parse from location string


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail); // Link to our detail screen design

        // Link UI elements
        textViewDetailFoodName = findViewById(R.id.textViewDetailFoodName);
        textViewDetailHealthStatus = findViewById(R.id.textViewDetailHealthStatus);
        detailHealthStatusIndicator = findViewById(R.id.detailHealthStatusIndicator);
        textViewDetailSugar = findViewById(R.id.textViewDetailSugar);
        textViewDetailSalt = findViewById(R.id.textViewDetailSalt);
        textViewDetailFat = findViewById(R.id.textViewDetailFat);
        textViewDetailTips = findViewById(R.id.textViewDetailTips);
        textViewDetailLocationDate = findViewById(R.id.textViewDetailLocationDate);
        buttonViewOnMap = findViewById(R.id.buttonViewOnMap);
        buttonDeleteScan = findViewById(R.id.buttonDeleteScan);
        buttonBackFromDetail = findViewById(R.id.buttonBackFromDetail);

        databaseHelper = new DatabaseHelper(this); // Initialize database helper

        // Get data passed from HistoryActivity
        Intent intent = getIntent();
        scanId = intent.getLongExtra("SCAN_ID", -1); // Get the ID of the scan
        foodName = intent.getStringExtra("FOOD_NAME");
        healthStatus = intent.getStringExtra("HEALTH_STATUS");
        sugar = intent.getStringExtra("SUGAR");
        salt = intent.getStringExtra("SALT");
        fat = intent.getStringExtra("FAT");
        tips = intent.getStringExtra("TIPS");
        date = intent.getStringExtra("DATE");
        location = intent.getStringExtra("LOCATION"); // This is the string "Lat: X.X, Lon: Y.Y" or "City, Country"

        // Display data
        textViewDetailFoodName.setText("Food: " + foodName);
        textViewDetailHealthStatus.setText("Status: " + healthStatus);
        textViewDetailSugar.setText("Sugar: " + sugar);
        textViewDetailSalt.setText("Salt: " + salt);
        textViewDetailFat.setText("Fat: " + fat);
        textViewDetailTips.setText("Tips: " + tips);
        textViewDetailLocationDate.setText("Scanned: " + date + " at " + location);

        // Set health status indicator color
        if ("Healthy".equals(healthStatus)) {
            detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if ("Unhealthy".equals(healthStatus)) {
            detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#F44336"));
        } else if ("Moderate".equals(healthStatus)) {
            detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#FFC107"));
        } else {
            detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }

        // To parse latitude and longitude from the location string
        // We only enable the map button if we successfully parsed precise coordinates.
        if (location != null && location.startsWith("Lat: ")) { // Check for "Lat: " prefix
            try {
                // Split by ", " to get "Lat: X.X" and "Lon: Y.Y"
                String[] parts = location.split(", ");
                if (parts.length == 2) {
                    // Extract numbers after "Lat: " and "Lon: "
                    latitude = Double.parseDouble(parts[0].replace("Lat: ", ""));
                    longitude = Double.parseDouble(parts[1].replace("Lon: ", ""));
                    buttonViewOnMap.setEnabled(true); // Enable map button if parsing is successful
                } else {
                    buttonViewOnMap.setEnabled(false); // Disable if format is unexpected
                }
            } catch (NumberFormatException e) {
                // Log error if parsing fails, but proceed without map button
                e.printStackTrace();
                Toast.makeText(this, "Could not parse precise coordinates for map.", Toast.LENGTH_SHORT).show();
                buttonViewOnMap.setEnabled(false); // Disable map button if coordinates can't be parsed
            }
        } else {
            // If location is a city/country name or unknown, map button is disabled
            buttonViewOnMap.setEnabled(false);
            // Optional: You could add a Toast here if you want to notify user why map is disabled.
            // Toast.makeText(this, "Map view only available for precise Lat/Lon coordinates.", Toast.LENGTH_SHORT).show();
        }


        // What happens when "View on Map" button is clicked
        buttonViewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonViewOnMap.isEnabled()) { // Check if button is enabled (i.e., coordinates are valid)
                    Intent mapIntent = new Intent(HistoryDetailActivity.this, MapsActivity.class);
                    mapIntent.putExtra("LATITUDE", latitude);
                    mapIntent.putExtra("LONGITUDE", longitude);
                    mapIntent.putExtra("LOCATION_NAME", foodName + " Scan Location"); // Name for marker
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(HistoryDetailActivity.this, "Map view is not available for this location.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // What happens when "Delete Scan" button is clicked
        buttonDeleteScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a confirmation dialog before deleting
                new AlertDialog.Builder(HistoryDetailActivity.this)
                        .setTitle("Delete Scan")
                        .setMessage("Are you sure you want to delete this scan entry?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (scanId != -1) {
                                boolean deleted = databaseHelper.deleteScanResult(scanId);
                                if (deleted) {
                                    Toast.makeText(HistoryDetailActivity.this, "Scan deleted successfully.", Toast.LENGTH_SHORT).show();
                                    finish(); // Go back to history list after deletion
                                } else {
                                    Toast.makeText(HistoryDetailActivity.this, "Failed to delete scan.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HistoryDetailActivity.this, "Invalid scan ID.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", null) // Do nothing if "No" is clicked
                        .show();
            }
        });

        // What happens when "Back to History" button is clicked
        buttonBackFromDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and go back to HistoryActivity
            }
        });
    }
}