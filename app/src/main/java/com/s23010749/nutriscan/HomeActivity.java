package com.s23010749.nutriscan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialCardView buttonScanNewItem = findViewById(R.id.buttonScanNewItem);
        MaterialCardView buttonViewHistory = findViewById(R.id.buttonViewHistory);
        MaterialCardView buttonAbout = findViewById(R.id.buttonAbout);
        MaterialCardView buttonSettings = findViewById(R.id.buttonSettings);
        MaterialCardView buttonLogout = findViewById(R.id.buttonLogout);

        buttonScanNewItem.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ScanActivity.class)));
        buttonViewHistory.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, HistoryActivity.class)));
        buttonAbout.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AboutActivity.class)));
        buttonSettings.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        buttonLogout.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}