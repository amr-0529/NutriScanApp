package com.s23010749.nutriscan;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseHelper databaseHelper;
    private List<ScanHistoryItem> historyList;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        Button buttonBack = findViewById(R.id.buttonBackFromHistory);
        databaseHelper = new DatabaseHelper(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();

        loadHistory();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadHistory() {
        historyList.clear();
        Cursor cursor = databaseHelper.getAllScanHistory();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                historyList.add(new ScanHistoryItem(
                        cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCAN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HEALTH_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NUTRIENTS_JSON)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIPS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Toast.makeText(this, "No scan history yet.", Toast.LENGTH_SHORT).show();
        }

        adapter = new HistoryAdapter(this, historyList, item -> {
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
            intent.putExtra("SCAN_ID", item.id);
            intent.putExtra("FOOD_NAME", item.foodName);
            intent.putExtra("HEALTH_STATUS", item.healthStatus);
            intent.putExtra("NUTRIENTS_JSON", item.nutrientsJson);
            intent.putExtra("TIPS", item.tips);
            intent.putExtra("DATE", item.date);
            intent.putExtra("LOCATION", item.location);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }
}