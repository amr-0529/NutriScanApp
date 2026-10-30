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
import org.json.JSONObject;
import java.util.Iterator;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView tvName = findViewById(R.id.textViewFoodName);
        TextView tvScore = findViewById(R.id.textViewScore);
        TextView tvStatus = findViewById(R.id.textViewHealthStatus);
        CircularProgressIndicator progress = findViewById(R.id.healthProgress);
        LinearLayout container = findViewById(R.id.nutritionContainer);
        TextView tvTips = findViewById(R.id.textViewTips);
        TextView tvDate = findViewById(R.id.textViewLocationDate);
        Button btnBack = findViewById(R.id.buttonBack);

        String name = getIntent().getStringExtra("food_name");
        String jsonStr = getIntent().getStringExtra("nutrients_json");
        String date = getIntent().getStringExtra("date");

        tvName.setText(name);
        tvDate.setText("Logged on " + date);

        double sugar = 0, fat = 0, salt = 0;

        try {
            JSONObject json = new JSONObject(jsonStr);
            sugar = extractVal(json.optString("Sugar", "0"));
            fat = extractVal(json.optString("Fat", "0"));
            salt = extractVal(json.optString("Salt", "0"));

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String val = json.getString(key);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 12, 0, 12);

                TextView tvKey = new TextView(this);
                tvKey.setText(key);
                tvKey.setTextColor(Color.parseColor("#334155"));
                tvKey.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvVal = new TextView(this);
                tvVal.setText(val);
                tvVal.setTextColor(Color.parseColor("#0F172A"));
                tvVal.setTypeface(null, Typeface.BOLD);
                tvVal.setGravity(Gravity.END);

                row.addView(tvKey);
                row.addView(tvVal);
                container.addView(row);
            }
        } catch (Exception e) {}

        int score = 100;
        if (sugar > 13.5) score -= 30; else if (sugar > 5) score -= 10;
        if (fat > 5.0) score -= 25; else if (fat > 3) score -= 10;
        if (salt > 1.5) score -= 25; else if (salt > 0.3) score -= 10;
        score = Math.max(0, score);

        String status;
        if (score >= 80) { status = "Healthy"; progress.setIndicatorColor(Color.parseColor("#10B981")); tvStatus.setTextColor(Color.parseColor("#10B981")); tvTips.setText("Great choice! Good nutrient balance."); }
        else if (score >= 50) { status = "Moderate"; progress.setIndicatorColor(Color.parseColor("#F59E0B")); tvStatus.setTextColor(Color.parseColor("#F59E0B")); tvTips.setText("Consume in moderation. Watch portion sizes."); }
        else { status = "Unhealthy"; progress.setIndicatorColor(Color.parseColor("#EF4444")); tvStatus.setTextColor(Color.parseColor("#EF4444")); tvTips.setText("High in unhealthy nutrients. Limit consumption."); }

        progress.setProgress(score);
        tvScore.setText(String.valueOf(score));
        tvStatus.setText(status);

        DatabaseHelper db = new DatabaseHelper(this);
        btnBack.setOnClickListener(v -> {
            // FIXED: Added "Unknown" as the 6th argument for location
            db.addScanResult(name, status, jsonStr, tvTips.getText().toString(), date, "Unknown");
            Toast.makeText(this, "Added to daily log!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private double extractVal(String text) {
        try {
            String num = text.replaceAll("[^0-9.]", "");
            return num.isEmpty() ? 0 : Double.parseDouble(num);
        } catch (Exception e) { return 0; }
    }
}