package com.s23010749.nutriscan; // Make sure this matches your package name!

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // For delaying the transition
import android.os.Looper; // For ensuring handler runs on main thread
import androidx.appcompat.app.AppCompatActivity;

// This is the first screen that appears when the app starts.
public class SplashScreenActivity extends AppCompatActivity {

    // How long the splash screen will show (in milliseconds)
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Link to our splash screen design

        // Use a Handler to delay going to the next activity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
            @Override
            public void run() {
                // Create an Intent to start the LoginActivity
                Intent mainIntent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                SplashScreenActivity.this.startActivity(mainIntent); // Start the Login screen
                SplashScreenActivity.this.finish(); // Close the splash screen so user can't go back
            }
        }, SPLASH_DISPLAY_LENGTH); // Wait for the specified time
    }
}