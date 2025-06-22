package com.s23010749.nutriscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View; // Needed for Button and its click listener
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// This activity displays a Google Map with a marker at a specific location.
// It implements OnMapReadyCallback to know when the map is ready to be used.
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap; // Our Google Map object
    private LatLng locationLatLng; // Latitude and longitude of the location to show
    private String locationName; // Name to display on the map marker

    Button buttonBackFromMap; // Button to go back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps); // Link to our map screen design

        buttonBackFromMap = findViewById(R.id.buttonBackFromMap); // Find the back button

        // Get location data from the Intent that started this activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double lat = extras.getDouble("LATITUDE", 0.0);
            double lon = extras.getDouble("LONGITUDE", 0.0);
            locationName = extras.getString("LOCATION_NAME", "Scanned Location");

            // Check if coordinates are 0.0, 0.0, which means no valid lat/lon was passed
            if (lat == 0.0 && lon == 0.0) {
                // If precise location is 0,0, default to Colombo.
                locationLatLng = new LatLng(6.9271, 79.8612); // Default to Colombo (Sri Lanka)
                locationName = "Default Location (Colombo - No precise scan location)";
                Toast.makeText(this, "Precise scan location not available. Showing default map location.", Toast.LENGTH_LONG).show();
            } else {
                locationLatLng = new LatLng(lat, lon);
            }
        } else {
            // Default to a known location if no data is passed (E.g, Colombo, Sri Lanka)
            locationLatLng = new LatLng(6.9271, 79.8612); // Default to Colombo
            locationName = "Default Location (Colombo - No intent data)";
            Toast.makeText(this, "No location data provided. Showing default map location.", Toast.LENGTH_LONG).show();
        }

        // Get the SupportMapFragment from the layout and get notified when the map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // This calls onMapReady when the map is loaded
        } else {
            Toast.makeText(this, "Error: Map fragment not found.", Toast.LENGTH_LONG).show();
        }

        // Set up what happens when the back button is clicked
        buttonBackFromMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and go back to the previous one (HistoryDetail)
            }
        });
    }

    // This method is called when the Google Map is ready to be used.
    // The @NonNull annotation ensures the map object is not null.
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map; // Assign the ready map to our variable

        // Add a marker at the specified location and move the camera there
        if (locationLatLng != null) {
            googleMap.addMarker(new MarkerOptions().position(locationLatLng).title(locationName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15)); // Zoom level 15 is good for city view
        } else {
            Toast.makeText(this, "Error: Map object or location data is null.", Toast.LENGTH_LONG).show();
        }
    }
}