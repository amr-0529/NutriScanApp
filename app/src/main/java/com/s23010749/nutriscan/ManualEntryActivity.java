package com.s23010749.nutriscan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ManualEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);

        EditText etName = findViewById(R.id.etName);
        EditText etCal = findViewById(R.id.etCalories);
        EditText etPro = findViewById(R.id.etProtein);
        EditText etCarb = findViewById(R.id.etCarbs);
        EditText etSug = findViewById(R.id.etSugar);
        EditText etFat = findViewById(R.id.etFat);

        findViewById(R.id.btnSaveManual).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject json = new JSONObject();
            try {
                String cal = etCal.getText().toString().isEmpty() ? "0" : etCal.getText().toString();
                String pro = etPro.getText().toString().isEmpty() ? "0" : etPro.getText().toString();
                String carb = etCarb.getText().toString().isEmpty() ? "0" : etCarb.getText().toString();
                String sug = etSug.getText().toString().isEmpty() ? "0" : etSug.getText().toString();
                String fat = etFat.getText().toString().isEmpty() ? "0" : etFat.getText().toString();

                json.put("Energy", cal + " kcal");
                json.put("Protein", pro + "g");
                json.put("Carbohydrates", carb + "g");
                json.put("Sugar", sug + "g");
                json.put("Fat", fat + "g");
            } catch (Exception e) {}

            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("food_name", name);
            intent.putExtra("nutrients_json", json.toString());
            intent.putExtra("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            startActivity(intent);
            finish();
        });
    }
}