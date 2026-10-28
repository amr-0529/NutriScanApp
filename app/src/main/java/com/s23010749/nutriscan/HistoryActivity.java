package com.s23010749.nutriscan;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private DatabaseHelper databaseHelper;
    private ArrayList<ScanHistoryItem> scanHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listViewHistory = findViewById(R.id.listViewHistory);
        Button buttonBackFromHistory = findViewById(R.id.buttonBackFromHistory);
        databaseHelper = new DatabaseHelper(this);

        loadScanHistory();

        listViewHistory.setOnItemClickListener((parent, view, position, id) -> {
            ScanHistoryItem clickedItem = scanHistoryList.get(position);
            Intent detailIntent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
            detailIntent.putExtra("SCAN_ID", clickedItem.id);
            detailIntent.putExtra("FOOD_NAME", clickedItem.foodName);
            detailIntent.putExtra("HEALTH_STATUS", clickedItem.healthStatus);
            detailIntent.putExtra("NUTRIENTS_JSON", clickedItem.nutrientsJson);
            detailIntent.putExtra("INGREDIENTS", clickedItem.ingredients); // NEW
            detailIntent.putExtra("TIPS", clickedItem.tips);
            detailIntent.putExtra("DATE", clickedItem.date);
            detailIntent.putExtra("LOCATION", clickedItem.location);
            startActivity(detailIntent);
        });

        buttonBackFromHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScanHistory();
    }

    private void loadScanHistory() {
        scanHistoryList = new ArrayList<>();
        ArrayList<String> displayList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = databaseHelper.getAllScanHistory();
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCAN_ID));
                    String food = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_NAME));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HEALTH_STATUS));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION));
                    String nutrientsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NUTRIENTS_JSON));
                    String ingredients = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_INGREDIENTS)); // NEW
                    String tips = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIPS));

                    scanHistoryList.add(new ScanHistoryItem(id, food, status, nutrientsJson, ingredients, tips, date, location));
                    displayList.add(food + " (" + status + ") - " + date.substring(0, 10));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
                listViewHistory.setAdapter(adapter);
            } else {
                Toast.makeText(this, "No scan history yet.", Toast.LENGTH_LONG).show();
                listViewHistory.setAdapter(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static class ScanHistoryItem {
        long id; String foodName, healthStatus, nutrientsJson, ingredients, tips, date, location;
        ScanHistoryItem(long id, String foodName, String healthStatus, String nutrientsJson, String ingredients, String tips, String date, String location) {
            this.id = id; this.foodName = foodName; this.healthStatus = healthStatus; this.nutrientsJson = nutrientsJson; this.ingredients = ingredients; this.tips = tips; this.date = date; this.location = location;
        }
    }
}