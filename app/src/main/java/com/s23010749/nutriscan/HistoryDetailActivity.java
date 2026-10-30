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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

public class HistoryDetailActivity extends AppCompatActivity {

    TextView textViewDetailFoodName, textViewDetailHealthStatus, textViewDetailTips, textViewDetailLocationDate;
    View detailHealthStatusIndicator;
    LinearLayout detailNutritionContainer;
    Button buttonDeleteScan, buttonBackFromDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        textViewDetailFoodName = findViewById(R.id.textViewDetailFoodName);
        textViewDetailHealthStatus = findViewById(R.id.textViewDetailHealthStatus);
        detailHealthStatusIndicator = findViewById(R.id.detailHealthStatusIndicator);
        detailNutritionContainer = findViewById(R.id.detailNutritionContainer);
        textViewDetailTips = findViewById(R.id.textViewDetailTips);
        textViewDetailLocationDate = findViewById(R.id.textViewDetailLocationDate);
        buttonDeleteScan = findViewById(R.id.buttonDeleteScan);
        buttonBackFromDetail = findViewById(R.id.buttonBackFromDetail);

        DatabaseHelper databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        long scanId = intent.getLongExtra("SCAN_ID", -1);
        String foodName = intent.getStringExtra("FOOD_NAME");
        String healthStatus = intent.getStringExtra("HEALTH_STATUS");
        String nutrientsJson = intent.getStringExtra("NUTRIENTS_JSON");
        String tips = intent.getStringExtra("TIPS");
        String date = intent.getStringExtra("DATE");

        textViewDetailFoodName.setText(foodName);
        textViewDetailHealthStatus.setText(healthStatus);
        textViewDetailTips.setText(tips);
        textViewDetailLocationDate.setText("Logged on: " + date);

        if ("Healthy".equals(healthStatus)) detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#10B981"));
        else if ("Unhealthy".equals(healthStatus)) detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#EF4444"));
        else if ("Moderate".equals(healthStatus)) detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#F59E0B"));
        else detailHealthStatusIndicator.setBackgroundColor(Color.parseColor("#94A3B8"));

        try {
            JSONObject jsonObject = new JSONObject(nutrientsJson);
            Iterator<String> keys = jsonObject.keys();

            if (!keys.hasNext()) {
                TextView noData = new TextView(this);
                noData.setText("No nutrient data saved.");
                noData.setTextColor(Color.GRAY);
                detailNutritionContainer.addView(noData);
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
                detailNutritionContainer.addView(row);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.parseColor("#E2E8F0"));
                detailNutritionContainer.addView(divider);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        long finalScanId = scanId;
        buttonDeleteScan.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Log").setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (finalScanId != -1) {
                        databaseHelper.deleteScanResult(finalScanId);
                        Toast.makeText(this, "Log deleted.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).setNegativeButton("No", null).show());

        buttonBackFromDetail.setOnClickListener(v -> finish());
    }
}