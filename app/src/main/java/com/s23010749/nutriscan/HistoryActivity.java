package com.s23010749.nutriscan;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView; // For handling list item clicks
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

// This screen displays a list of all past food scan results.
public class HistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private DatabaseHelper databaseHelper;
    // Remove the emptyHistoryMessage TextView because it's not directly added to layout by code now.
    // ArrayAdapter is used with simple_list_item_1, so a separate empty view in XML is usually better
    // If you want to use setEmptyView(). For simplicity, we'll stick to a Toast if empty.

    private ArrayList<ScanHistoryItem> scanHistoryList; // To store full objects for detail view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // Connect to our history screen design.

        listViewHistory = findViewById(R.id.listViewHistory);
        Button buttonBackFromHistory = findViewById(R.id.buttonBackFromHistory);
        databaseHelper = new DatabaseHelper(this); // Initialize our database helper

        // Load and display history when the screen is created
        loadScanHistory();

        // What happens when a history item is clicked
        listViewHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the clicked item from our list
                ScanHistoryItem clickedItem = scanHistoryList.get(position);

                // Create an Intent to go to HistoryDetailActivity
                Intent detailIntent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);

                // Put all the details of the clicked item into the Intent
                detailIntent.putExtra("SCAN_ID", clickedItem.getId());
                detailIntent.putExtra("FOOD_NAME", clickedItem.getFoodName());
                detailIntent.putExtra("HEALTH_STATUS", clickedItem.getHealthStatus());
                detailIntent.putExtra("SUGAR", clickedItem.getSugar());
                detailIntent.putExtra("SALT", clickedItem.getSalt());
                detailIntent.putExtra("FAT", clickedItem.getFat());
                detailIntent.putExtra("TIPS", clickedItem.getTips());
                detailIntent.putExtra("DATE", clickedItem.getDate());
                detailIntent.putExtra("LOCATION", clickedItem.getLocation());

                startActivity(detailIntent); // Start the detail screen
            }
        });


        // What happens when the "Back to Home" button is clicked.
        buttonBackFromHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
                // Clear activity stack to prevent going back to scan/login
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Close this history screen.
            }
        });
    }

    // This method makes sure history is refreshed if we come back from deleting an item
    @Override
    protected void onResume() {
        super.onResume();
        loadScanHistory(); // Reload history whenever this activity is resumed
    }

    // Function to load data from the database and show it in the list.
    private void loadScanHistory() {
        scanHistoryList = new ArrayList<>(); // Clear previous list
        ArrayList<String> displayList = new ArrayList<>(); // For the ArrayAdapter

        Cursor cursor = null;
        try {
            cursor = databaseHelper.getAllScanHistory(); // Get all history from DB

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCAN_ID));
                    String food = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOOD_NAME));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HEALTH_STATUS));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                    String location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION));
                    String sugar = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUGAR));
                    String salt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SALT));
                    String fat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAT));
                    String tips = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIPS));

                    // Store the full object
                    scanHistoryList.add(new ScanHistoryItem(id, food, status, sugar, salt, fat, tips, date, location));

                    // Create a display string for the ListView
                    String itemString = food + " (" + status + ") - " + date.substring(0,10); // Show name, status, and just date part
                    displayList.add(itemString);
                }

                // Set up the adapter to display the items in the ListView
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1, // A simple built-in layout for each list item
                        displayList
                );
                listViewHistory.setAdapter(adapter);

            } else {
                // If no history, show a message
                Toast.makeText(this, "No scan history yet. Go scan some food!", Toast.LENGTH_LONG).show();
                listViewHistory.setAdapter(null); // Clear the list view if empty
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No need to close database helper here, it's managed by the system.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The DatabaseHelper will close the database automatically.
    }

    // A simple class to hold our scan history data for easier passing
    private static class ScanHistoryItem {
        private long id;
        private String foodName;
        private String healthStatus;
        private String sugar;
        private String salt;
        private String fat;
        private String tips;
        private String date;
        private String location;

        public ScanHistoryItem(long id, String foodName, String healthStatus, String sugar, String salt, String fat, String tips, String date, String location) {
            this.id = id;
            this.foodName = foodName;
            this.healthStatus = healthStatus;
            this.sugar = sugar;
            this.salt = salt;
            this.fat = fat;
            this.tips = tips;
            this.date = date;
            this.location = location;
        }

        // Getters for all properties
        public long getId() { return id; }
        public String getFoodName() { return foodName; }
        public String getHealthStatus() { return healthStatus; }
        public String getSugar() { return sugar; }
        public String getSalt() { return salt; }
        public String getFat() { return fat; }
        public String getTips() { return tips; }
        public String getDate() { return date; }
        public String getLocation() { return location; }
    }
}