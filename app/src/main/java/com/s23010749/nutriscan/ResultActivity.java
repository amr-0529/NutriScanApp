package com.s23010749.nutriscan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

public class ResultActivity extends AppCompatActivity {

    TextView textViewFoodName, textViewHealthStatus, textViewTips, textViewLocationDate, textViewScore;
    CircularProgressIndicator healthProgress;
    LinearLayout nutritionContainer;
    Button buttonBack;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        textViewFoodName = findViewById(R.id.textViewFoodName);
        textViewHealthStatus = findViewById(R.id.textViewHealthStatus);
        textViewScore = findViewById(R.id.textViewScore);
        healthProgress = findViewById(R.id.healthProgress);
        nutritionContainer = findViewById(R.id.nutritionContainer);
        textViewTips = findViewById(R.id.textViewTips);
        textViewLocationDate = findViewById(R.id.textViewLocationDate);
        buttonBack = findViewById(R.id.buttonBack);

        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        String foodName = intent.getStringExtra("food_name");
        String nutrientsJson = intent.getStringExtra("nutrients_json");
        String healthStatus = intent.getStringExtra("health");
        int score = intent.getIntExtra("score", -1);
        String tips = intent.getStringExtra("tips");
        String scanDate = intent.getStringExtra("date");
        String scanLocation = intent.getStringExtra("location");

        textViewFoodName.setText(foodName);
        textViewHealthStatus.setText(healthStatus);
        textViewLocationDate.setText("Scanned on " + scanDate + "\n" + scanLocation);
        textViewTips.setText(tips != null ? tips : "No tips available.");

        if (score == -1) {
            healthProgress.setProgress(0);
            healthProgress.setIndicatorColor(Color.parseColor("#94A3B8"));
            textViewScore.setText("?");
            textViewHealthStatus.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            healthProgress.setProgress(score);
            textViewScore.setText(String.valueOf(score));

            if ("Healthy".equals(healthStatus)) {
                healthProgress.setIndicatorColor(Color.parseColor("#10B981"));
                textViewHealthStatus.setTextColor(Color.parseColor("#10B981"));
            } else if ("Unhealthy".equals(healthStatus)) {
                healthProgress.setIndicatorColor(Color.parseColor("#EF4444"));
                textViewHealthStatus.setTextColor(Color.parseColor("#EF4444"));
            } else {
                healthProgress.setIndicatorColor(Color.parseColor("#F59E0B"));
                textViewHealthStatus.setTextColor(Color.parseColor("#F59E0B"));
            }
        }

        try {
            JSONObject jsonObject = new JSONObject(nutrientsJson);
            Iterator<String> keys = jsonObject.keys();

            if (!keys.hasNext()) {
                TextView noData = new TextView(this);
                noData.setText("No readable nutrients found.");
                noData.setTextColor(Color.GRAY);
                nutritionContainer.addView(noData);
            }

            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 12, 0, 12);

                TextView tvKey = new TextView(this);
                tvKey.setText(key);
                tvKey.setTextColor(Color.parseColor("#334155"));
                tvKey.setTextSize(16);
                tvKey.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvValue = new TextView(this);
                tvValue.setText(value);
                tvValue.setTextColor(Color.parseColor("#0F172A"));
                tvValue.setTextSize(16);
                tvValue.setTypeface(null, Typeface.BOLD);
                tvValue.setGravity(Gravity.END);

                row.addView(tvKey);
                row.addView(tvValue);
                nutritionContainer.addView(row);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.parseColor("#E2E8F0"));
                nutritionContainer.addView(divider);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        buttonBack.setOnClickListener(v -> {
            // Passing empty string for ingredients parameter to maintain DB compatibility
            databaseHelper.addScanResult(foodName, healthStatus, nutrientsJson, "", textViewTips.getText().toString(), scanDate, scanLocation);
            Toast.makeText(ResultActivity.this, "Saved to history!", Toast.LENGTH_SHORT).show();

            Intent backToHome = new Intent(ResultActivity.this, HomeActivity.class);
            backToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(backToHome);
            finish();
        });
    }
}