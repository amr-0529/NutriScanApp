package com.s23010749.nutriscan;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    TextView tvCalories, tvProtein, tvCarbs, tvSugar, tvFat;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);

        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvSugar = findViewById(R.id.tvSugar);
        tvFat = findViewById(R.id.tvFat);

        findViewById(R.id.btnScanBarcode).setOnClickListener(v -> startActivity(new Intent(this, ScanActivity.class)));
        findViewById(R.id.btnManualEntry).setOnClickListener(v -> startActivity(new Intent(this, ManualEntryActivity.class)));
        findViewById(R.id.btnViewHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard();
    }

    private void updateDashboard() {
        double cals = 0, pro = 0, carb = 0, sug = 0, fat = 0;
        Cursor cursor = db.getTodayScans();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String jsonStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NUTRIENTS_JSON));
                try {
                    JSONObject json = new JSONObject(jsonStr);
                    cals += extractNum(json.optString("Energy", "0"));
                    pro += extractNum(json.optString("Protein", "0"));
                    carb += extractNum(json.optString("Carbohydrates", "0"));
                    sug += extractNum(json.optString("Sugar", "0"));
                    fat += extractNum(json.optString("Fat", "0"));
                } catch (JSONException e) { e.printStackTrace(); }
            } while (cursor.moveToNext());
            cursor.close();
        }

        tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", cals));
        tvProtein.setText(String.format(Locale.getDefault(), "%.1f g", pro));
        tvCarbs.setText(String.format(Locale.getDefault(), "%.1f g", carb));
        tvSugar.setText(String.format(Locale.getDefault(), "%.1f g", sug));
        tvFat.setText(String.format(Locale.getDefault(), "%.1f g", fat));
    }

    private double extractNum(String text) {
        try {
            String num = text.replaceAll("[^0-9.]", "");
            return num.isEmpty() ? 0 : Double.parseDouble(num);
        } catch (Exception e) { return 0; }
    }
}