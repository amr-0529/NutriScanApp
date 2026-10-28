package com.s23010749.nutriscan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    TextView textViewFoodName, textViewHealthStatus, textViewSugar, textViewSalt, textViewFat, textViewTips, textViewLocationDate;
    View healthStatusIndicator;
    Button buttonBack;

    DatabaseHelper databaseHelper;

    String foodName, sugarLevel, saltLevel, fatLevel, healthStatus, tipsMessage, scanDate, scanLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        textViewFoodName = findViewById(R.id.textViewFoodName);
        textViewHealthStatus = findViewById(R.id.textViewHealthStatus);
        healthStatusIndicator = findViewById(R.id.healthStatusIndicator);
        textViewSugar = findViewById(R.id.textViewSugar);
        textViewSalt = findViewById(R.id.textViewSalt);
        textViewFat = findViewById(R.id.textViewFat);
        textViewTips = findViewById(R.id.textViewTips);
        textViewLocationDate = findViewById(R.id.textViewLocationDate);
        buttonBack = findViewById(R.id.buttonBack);

        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        foodName = intent.getStringExtra("food_name");
        sugarLevel = intent.getStringExtra("sugar_level");
        saltLevel = intent.getStringExtra("salt_level");
        fatLevel = intent.getStringExtra("fat_level");
        healthStatus = intent.getStringExtra("health_status");
        tipsMessage = intent.getStringExtra("tips_message");
        scanDate = intent.getStringExtra("scan_date");
        scanLocation = intent.getStringExtra("scan_location");

        textViewFoodName.setText("Food Name: " + foodName);
        textViewHealthStatus.setText("Status: " + healthStatus);
        textViewSugar.setText("Sugar: " + sugarLevel);
        textViewSalt.setText("Salt: " + saltLevel);
        textViewFat.setText("Fat: " + fatLevel);
        textViewTips.setText("Tips: " + tipsMessage);
        textViewLocationDate.setText("Scanned: " + scanDate + " at " + scanLocation);

        // Set health status indicator color
        if ("Healthy".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        } else if ("Unhealthy".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#F44336")); // Red
        } else if ("Moderate".equals(healthStatus)) {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#FFC107")); // Orange/Yellow
        } else {
            healthStatusIndicator.setBackgroundColor(Color.parseColor("#9E9E9E")); // Gray
        }

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long result = databaseHelper.addScanResult(
                        foodName, healthStatus, sugarLevel, saltLevel, fatLevel, tipsMessage, scanDate, scanLocation
                );

                if (result != -1) {
                    Toast.makeText(ResultActivity.this, "Scan saved to history!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ResultActivity.this, "Failed to save scan history.", Toast.LENGTH_SHORT).show();
                }

                Intent backToHome = new Intent(ResultActivity.this, HomeActivity.class);
                backToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backToHome);
                finish();
            }
        });
    }
}