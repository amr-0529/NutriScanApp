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
import androidx.cardview.widget.CardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

public class ResultActivity extends AppCompatActivity {

    TextView textViewFoodName, textViewHealthStatus, textViewTips, textViewLocationDate, textViewScore, textViewIngredients;
    CircularProgressIndicator healthProgress;
    LinearLayout nutritionContainer;
    ChipGroup chipGroupTags;
    CardView cardIngredients;
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
        chipGroupTags = findViewById(R.id.chipGroupTags);
        textViewTips = findViewById(R.id.textViewTips);
        textViewLocationDate = findViewById(R.id.textViewLocationDate);
        cardIngredients = findViewById(R.id.cardIngredients);
        textViewIngredients = findViewById(R.id.textViewIngredients);
        buttonBack = findViewById(R.id.buttonBack);

        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        String foodName = intent.getStringExtra("food_name");
        String nutrientsJson = intent.getStringExtra("nutrients_json");
        String ingredients = intent.getStringExtra("ingredients");
        String healthStatus = intent.getStringExtra("health");
        int score = intent.getIntExtra("score", -1);
        String tags = intent.getStringExtra("tags");
        String scanDate = intent.getStringExtra("date");
        String scanLocation = intent.getStringExtra("location");

        textViewFoodName.setText(foodName);
        textViewHealthStatus.setText(healthStatus);
        textViewLocationDate.setText("Scanned on " + scanDate + "\n" + scanLocation);

        if (ingredients != null && !ingredients.isEmpty()) {
            cardIngredients.setVisibility(View.VISIBLE);
            textViewIngredients.setText(ingredients);
        }

        if (score == -1) {
            healthProgress.setProgress(0);
            healthProgress.setIndicatorColor(Color.parseColor("#9E9E9E"));
            textViewScore.setText("?");
            textViewHealthStatus.setTextColor(Color.parseColor("#9E9E9E"));
            textViewTips.setText("Could not detect nutrition values. Please hold the camera steady, ensure good lighting, and try again.");
        } else {
            healthProgress.setProgress(score);
            textViewScore.setText(String.valueOf(score));

            if ("Healthy".equals(healthStatus)) {
                healthProgress.setIndicatorColor(Color.parseColor("#4CAF50"));
                textViewHealthStatus.setTextColor(Color.parseColor("#4CAF50"));
                textViewTips.setText("Great choice! This product has a good nutritional balance.");
            } else if ("Unhealthy".equals(healthStatus)) {
                healthProgress.setIndicatorColor(Color.parseColor("#F44336"));
                textViewHealthStatus.setTextColor(Color.parseColor("#F44336"));
                textViewTips.setText("Warning: This product is high in unhealthy nutrients. Consume in moderation.");
            } else {
                healthProgress.setIndicatorColor(Color.parseColor("#FFC107"));
                textViewHealthStatus.setTextColor(Color.parseColor("#FFC107"));
                textViewTips.setText("Okay for occasional consumption. Watch your portion sizes.");
            }
        }

        if (tags != null && !tags.isEmpty() && score != -1) {
            String[] tagArray = tags.split("\\|");
            for (String tag : tagArray) {
                if (tag.isEmpty()) continue;
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setChipStrokeWidth(2f);

                if (tag.contains("High")) {
                    chip.setChipStrokeColorResource(android.R.color.holo_red_dark);
                    chip.setTextColor(Color.parseColor("#F44336"));
                } else if (tag.contains("Low") || tag.contains("Good")) {
                    chip.setChipStrokeColorResource(android.R.color.holo_green_dark);
                    chip.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    chip.setChipStrokeColorResource(android.R.color.darker_gray);
                    chip.setTextColor(Color.DKGRAY);
                }
                chipGroupTags.addView(chip);
            }
        }

        try {
            JSONObject jsonObject = new JSONObject(nutrientsJson);
            Iterator<String> keys = jsonObject.keys();

            if (!keys.hasNext()) {
                TextView noData = new TextView(this);
                noData.setText("No readable nutrients found on this label.");
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
                tvKey.setTextColor(Color.parseColor("#333333"));
                tvKey.setTextSize(16);
                tvKey.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvValue = new TextView(this);
                tvValue.setText(value);
                tvValue.setTextColor(Color.parseColor("#1A1A1A"));
                tvValue.setTextSize(16);
                tvValue.setTypeface(null, Typeface.BOLD);
                tvValue.setGravity(Gravity.END);

                row.addView(tvKey);
                row.addView(tvValue);
                nutritionContainer.addView(row);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                nutritionContainer.addView(divider);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        buttonBack.setOnClickListener(v -> {
            databaseHelper.addScanResult(foodName, healthStatus, nutrientsJson, ingredients, textViewTips.getText().toString(), scanDate, scanLocation);
            Toast.makeText(ResultActivity.this, "Saved to history!", Toast.LENGTH_SHORT).show();

            Intent backToHome = new Intent(ResultActivity.this, HomeActivity.class);
            backToHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(backToHome);
            finish();
        });
    }
}