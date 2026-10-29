package com.s23010749.nutriscan;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialSwitch switchNotifications = findViewById(R.id.switchNotifications);
        MaterialCardView cardClearHistory = findViewById(R.id.cardClearHistory);
        Button buttonBackSettings = findViewById(R.id.buttonBackSettings);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Daily reminders enabled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Daily reminders disabled.", Toast.LENGTH_SHORT).show();
            }
        });

        cardClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to delete all past scans?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper db = new DatabaseHelper(this);
                        db.getWritableDatabase().delete("scan_history", null, null);
                        Toast.makeText(this, "History cleared!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        buttonBackSettings.setOnClickListener(v -> finish());
    }
}