package com.s23010749.nutriscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Standard Button for reference if needed, but we use MaterialButton
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// This is the main screen after logging in/signing up, with navigation to other features.
public class HomeActivity extends AppCompatActivity {

    TextView textViewWelcome;
    // MaterialButton for proper icon display and modern look
    com.google.android.material.button.MaterialButton buttonScanNewItem, buttonViewHistory, buttonLogout;
    com.google.android.material.button.MaterialButton buttonAbout, buttonSettings; // New MaterialButtons

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Connects to our home screen design.

        // Linking up the stuff from our layout XML.
        textViewWelcome = findViewById(R.id.textViewWelcome);
        buttonScanNewItem = findViewById(R.id.buttonScanNewItem);
        buttonViewHistory = findViewById(R.id.buttonViewHistory);
        buttonLogout = findViewById(R.id.buttonLogout);
        // Link new About and Settings buttons
        buttonAbout = findViewById(R.id.buttonAbout);
        buttonSettings = findViewById(R.id.buttonSettings);


        // What happens when I tap the "Scan New Food Item" button.
        buttonScanNewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send the user to the camera scanning screen now!
                Intent intent = new Intent(HomeActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });

        // What happens when I tap the "View Scan History" button.
        buttonViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Now this button opens the new HistoryActivity!
                Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        // What happens when I tap the "Logout" button.
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send the user back to the Login screen.
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                // These flags clear the activity stack, so they can't go back to Home with the back button.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close this Home screen.
            }
        });

        // What happens when I tap the "About" button
        buttonAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

        // What happens when I tap the "Settings" button
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }
}