package com.s23010749.nutriscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image; // To get the actual image data from CameraX
import android.os.AsyncTask; // For running network tasks in the background
import android.os.Bundle;
import android.util.Log; // For logging messages to help debug
import android.view.View;
import android.widget.Button;
import android.widget.Toast; // For showing quick messages to the user

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector; // To choose front or back camera
import androidx.camera.core.ImageCapture; // To take a picture
import androidx.camera.core.ImageCaptureException; // To handle errors when taking picture
import androidx.camera.core.ImageProxy; // The image object captured by CameraX
import androidx.camera.core.Preview; // To show the live camera feed
import androidx.camera.lifecycle.ProcessCameraProvider; // To bind camera to activity lifecycle
import androidx.camera.view.PreviewView; // The view that displays camera feed
import androidx.core.app.ActivityCompat; // To ask for permissions
import androidx.core.content.ContextCompat; // To check permissions

import com.google.android.gms.location.FusedLocationProviderClient; // Google Play Services for Location
import com.google.android.gms.location.LocationServices; // To get location service client
import com.google.android.gms.tasks.OnFailureListener; // For handling failed tasks
import com.google.android.gms.tasks.OnSuccessListener; // For handling successful tasks
import com.google.common.util.concurrent.ListenableFuture; // For CameraX async operations

import com.google.mlkit.vision.common.InputImage; // ML Kit: How to feed image to ML models
import com.google.mlkit.vision.text.Text; // ML Kit: The recognized text result
import com.google.mlkit.vision.text.TextRecognition; // ML Kit: The main text recognition class
import com.google.mlkit.vision.text.TextRecognizer; // ML Kit: The recognizer object itself
import com.google.mlkit.vision.text.latin.TextRecognizerOptions; // ML Kit: Options for Latin script (English)

import org.json.JSONException; // To handle errors when working with JSON
import org.json.JSONObject; // To work with JSON data

import java.io.BufferedReader; // To read data from network response
import java.io.InputStream; // To get input stream from network
import java.io.InputStreamReader; // To read input stream
import java.net.HttpURLConnection; // To make network connections
import java.net.URL; // To create URLs
import java.text.SimpleDateFormat; // To format dates nicely
import java.util.Date; // To get current date and time
import java.util.List; // For lists of addresses
import java.util.Locale; // For language/country settings

import java.util.concurrent.ExecutionException; // For CameraX future errors
import java.util.concurrent.ExecutorService; // For background tasks
import java.util.concurrent.Executors;


// This screen uses the phone's camera to scan food labels, performs OCR, and fetches nutrition data!
public class ScanActivity extends AppCompatActivity {

    // Request codes for permissions, helps us know which permission result is coming back
    private static final int REQUEST_CODE_CAMERA_PERMISSIONS = 10;
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 11;

    // List of permissions we need from the user
    private static final String[] REQUIRED_CAMERA_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String[] REQUIRED_LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, // More precise location
            Manifest.permission.ACCESS_COARSE_LOCATION // Less precise location
    };

    // UI elements on the screen
    private PreviewView viewFinder; // Shows the live camera feed
    private Button buttonCaptureImage; // Button to take the picture

    // CameraX related variables
    private ImageCapture imageCapture; // Helps take actual photos
    private ExecutorService cameraExecutor; // Runs camera tasks in the background

    // Location related variables
    private FusedLocationProviderClient fusedLocationClient; // Client to get device location
    private String currentKnownLocation = "Unknown Location"; // Stores the text for current location

    // ML Kit Text Recognition related variable
    private TextRecognizer textRecognizer; // The ML Kit object that recognizes text

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan); // Link to our camera screen design (activity_scan.xml)

        // Find UI elements by their IDs
        viewFinder = findViewById(R.id.viewFinder);
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage);

        // Set up background workers
        cameraExecutor = Executors.newSingleThreadExecutor(); // For camera tasks
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); // For location tasks
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); // For text recognition (ML Kit)

        // Step 1: Check Camera Permissions and start camera preview
        if (allCameraPermissionsGranted()) {
            startCamera();
        } else {
            // If no camera permission, ask the user
            ActivityCompat.requestPermissions(this, REQUIRED_CAMERA_PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSIONS);
        }

        // Step 2: Request Location Permissions and get location if allowed
        if (allLocationPermissionsGranted()) {
            getLastLocation(); // Get location if permissions are already granted
        } else {
            // If no location permission, ask the user
            ActivityCompat.requestPermissions(this, REQUIRED_LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION_PERMISSIONS);
        }

        // What happens when the capture button is pressed
        buttonCaptureImage.setOnClickListener(v -> captureAndRecognizeText()); // Now, this triggers OCR.
    }

    // --- Camera Setup and Control Functions ---

    // Sets up the camera to show live preview and be ready to take pictures
    private void startCamera() {
        // This helps us wait until the camera provider is ready
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // When the camera provider is ready, do this:
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); // Get the camera provider

                // Create a preview use case for showing live camera feed
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider()); // Link preview to our viewFinder UI element

                // Create an image capture use case for taking pictures
                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(viewFinder.getDisplay().getRotation()) // Match screen rotation
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Make picture taking fast
                        .build();

                // Choose which camera to use (back camera for scanning)
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind any previous camera connections, then bind our new setup to the activity lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                // Show an error if camera fails to start
                Toast.makeText(this, "Oops! Couldn't start camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this)); // Run this on the main app thread
    }

    // This function captures the image and then sends it to ML Kit for text recognition
    private void captureAndRecognizeText() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Capturing image for text recognition...", Toast.LENGTH_SHORT).show();

        // Tell CameraX to take a picture
        imageCapture.takePicture(
                cameraExecutor, // Use our background executor for image processing
                new ImageCapture.OnImageCapturedCallback() { // What to do after picture is taken
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        // Image captured successfully, now process it with ML Kit
                        Log.d("ScanActivity", "Image captured successfully.");
                        processImageForTextRecognition(imageProxy); // Go to the OCR step
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Show an error if image capture fails.
                        Log.e("ScanActivity", "Image capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(ScanActivity.this, "Image capture failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        // Fallback: Proceed with a default food name if OCR fails due to capture error
                        fetchNutritionData("Unknown Scanned Food (Capture Error)");
                    }
                }
        );
    }

    // Processes the captured image using ML Kit's Text Recognition
    private void processImageForTextRecognition(@NonNull ImageProxy imageProxy) {
        // @OptIn is for experimental API, Android Studio might suggest adding it automatically.
        @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
        Image mediaImage = imageProxy.getImage(); // Get the raw image data from CameraX

        if (mediaImage != null) {
            // Create an InputImage object for ML Kit
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // Process the image to recognize text
            textRecognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text result) {
                            // Text recognition was successful!
                            String recognizedText = result.getText(); // Get all recognized text
                            Log.d("ScanActivity", "Recognized Text: " + recognizedText);
                            // Show a small preview of the recognized text
                            // Use substring to avoid very long toasts for large texts
                            String toastText = "Text Recognized: " + recognizedText;
                            if (recognizedText.length() > 50) {
                                toastText = "Text Recognized: " + recognizedText.substring(0, 50) + "...";
                            }
                            Toast.makeText(ScanActivity.this, toastText, Toast.LENGTH_SHORT).show();

                            // Use the recognized text to fetch nutrition data (simulated for now)
                            fetchNutritionData(recognizedText);
                        }
                    })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Text recognition failed
                                    Log.e("ScanActivity", "Text recognition failed: " + e.getMessage(), e);
                                    Toast.makeText(ScanActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Fallback: Proceed with a default food name if OCR fails
                                    fetchNutritionData("Unknown Scanned Food (OCR Failed)");
                                }
                            })
                    .addOnCompleteListener(task -> {
                        imageProxy.close(); // Always close the ImageProxy when you're done with it (success or failure)
                    });
        } else {
            Toast.makeText(this, "Failed to get image from camera for OCR. Image was null.", Toast.LENGTH_SHORT).show();
            imageProxy.close(); // Close if mediaImage is null
            fetchNutritionData("Unknown Scanned Food (Image Null)"); // Fallback if image data is missing
        }
    }


    // --- Location Sensing Functions ---

    // Checks if all required location permissions are granted
    private boolean allLocationPermissionsGranted() {
        for (String permission : REQUIRED_LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false; // Found a permission we don't have
            }
        }
        return true; // All location permissions are granted!
    }

    // Checks if all required camera permissions are granted
    private boolean allCameraPermissionsGranted() {
        for (String permission : REQUIRED_CAMERA_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false; // Found a permission we don't have
            }
        }
        return true; // All camera permissions are granted!
    }

    // Gets the last known location of the device using FusedLocationProviderClient
    private void getLastLocation() {
        if (allLocationPermissionsGranted()) {
            // Request the last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Store location as Lat:X.XXXX, Lon:Y.YYYY format for MapsActivity
                                currentKnownLocation = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", location.getLatitude(), location.getLongitude());
                                Log.d("ScanActivity", "Precise Location detected: " + currentKnownLocation);
                            } else {
                                // Fallback to a broader or unknown location if precise coords are not available
                                Geocoder geocoder = new Geocoder(ScanActivity.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address address = addresses.get(0);
                                        String locality = address.getLocality(); // City name
                                        String country = address.getCountryName(); // Country name
                                        if (locality != null && country != null) {
                                            currentKnownLocation = locality + ", " + country; // E.g., "Colombo, Sri Lanka"
                                        } else if (country != null) {
                                            currentKnownLocation = country; // E.g., "Sri Lanka"
                                        } else {
                                            currentKnownLocation = "Unknown Location"; // Fallback to unknown
                                        }
                                    } else {
                                        currentKnownLocation = "Unknown Location"; // Fallback to unknown
                                    }
                                } catch (Exception e) { // Catch block for Geocoder exception
                                    Log.e("ScanActivity", "Geocoder failed: " + e.getMessage(), e);
                                    currentKnownLocation = "Unknown Location (Geocoder Error)";
                                }
                                Log.d("ScanActivity", "Broad Location detected (or none): " + currentKnownLocation);
                            }
                        }
                    });
        } else {
            // If permissions are denied, set to "Location Permission Denied"
            currentKnownLocation = "Location Permission Denied";
            Toast.makeText(this, "Location permission not granted. Cannot get location.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Permission Request Results ---

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS) { // Result for camera permissions
            if (allCameraPermissionsGranted()) {
                startCamera(); // If granted, start camera
            } else {
                Toast.makeText(this, "Camera permission needed to use this feature!", Toast.LENGTH_LONG).show();
                finish(); // Close activity if camera permission is critical and denied
            }
        } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) { // Result for location permissions
            if (allLocationPermissionsGranted()) {
                getLastLocation(); // If granted, get location
            } else {
                Toast.makeText(this, "Location permission denied. Cannot save location.", Toast.LENGTH_LONG).show();
                currentKnownLocation = "Location Denied"; // Update location status
            }
        }
    }

    // --- Nutrition API Fetching (Simulated) ---

    // This method kicks off the background task to fetch nutrition data
    private void fetchNutritionData(String recognizedFoodText) {
        // Get current date and time
        String scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Start the background task to fetch nutrition data
        // Pass the recognized text, date, and location
        new FetchNutritionTask(recognizedFoodText, scanDate, currentKnownLocation).execute();
    }


    // AsyncTask to run network operations in the background so the app doesn't freeze.
    // NOTE: AsyncTask is deprecated, but simple for this example. For newer Android,
    // consider Kotlin Coroutines or `java.util.concurrent.ExecutorService` with Callbacks.
    private class FetchNutritionTask extends AsyncTask<Void, Void, String> {
        private String recognizedFoodText; // The text recognized by OCR
        private String scanDate;
        private String scanLocation;

        public FetchNutritionTask(String recognizedFoodText, String scanDate, String scanLocation) {
            this.recognizedFoodText = recognizedFoodText;
            this.scanDate = scanDate;
            this.scanLocation = scanLocation;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(ScanActivity.this, "Fetching nutrition data...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            // This is where you would normally make a real API call.
            // For now, we'll use a mock response based on the recognized text.
            return getMockNutritionData(recognizedFoodText);


        }

        // Provides a mock JSON response for demonstration.
        // This method tries to find keywords in the recognized text to return specific mock data.
        private String getMockNutritionData(String recognizedText) {
            String textLowerCase = recognizedText.toLowerCase(Locale.ROOT);

            if (textLowerCase.contains("coca") || textLowerCase.contains("cola") || textLowerCase.contains("coke")) {
                return "{\"product\":{\"product_name\":\"Coca Cola\",\"nutriments\":{\"energy_100g\":180,\"fat_100g\":0.0,\"sugars_100g\":10.6,\"salt_100g\":0.01},\"nutriscore_grade\":\"e\"}}";
            } else if (textLowerCase.contains("apple") || textLowerCase.contains("fruit")) {
                return "{\"product\":{\"product_name\":\"Fresh Apple\",\"nutriments\":{\"energy_100g\":218,\"fat_100g\":0.2,\"sugars_100g\":10.3,\"salt_100g\":0.00},\"nutriscore_grade\":\"a\"}}";
            } else if (textLowerCase.contains("chip") || textLowerCase.contains("crisps") || textLowerCase.contains("potato")) {
                return "{\"product\":{\"product_name\":\"Potato Chips (Salty)\",\"nutriments\":{\"energy_100g\":2240,\"fat_100g\":34.0,\"sugars_100g\":1.0,\"salt_100g\":1.2},\"nutriscore_grade\":\"d\"}}";
            } else if (textLowerCase.contains("milk") || textLowerCase.contains("dairy") || textLowerCase.contains("life") || textLowerCase.contains("freshmilk")) { // Added for "Rich Life Freshmilk"
                return "{\"product\":{\"product_name\":\"Fresh Milk (Rich Life)\",\"nutriments\":{\"energy_100g\":264,\"fat_100g\":3.5,\"sugars_100g\":4.7,\"salt_100g\":0.12},\"nutriscore_grade\":\"b\"}}";
            }
            // Default mock response for unknown items
            return "{\"product\":{\"product_name\":\"Unrecognized Food\",\"nutriments\":{\"energy_100g\":0,\"fat_100g\":0.0,\"sugars_100g\":0.0,\"salt_100g\":0.0},\"nutriscore_grade\":\"unknown\"}}";
        }


        @Override
        protected void onPostExecute(String jsonResponse) {
            super.onPostExecute(jsonResponse);

            String foodName = "Unrecognized Food"; // Default name if OCR fails or no match
            String sugarLevel = "N/A";
            String saltLevel = "N/A";
            String fatLevel = "N/A";
            String healthStatus = "Unknown";
            String tipsMessage = "Could not get detailed nutrition data. Ensure good lighting and clear text.";

            if (jsonResponse != null) {
                try {
                    JSONObject mainObject = new JSONObject(jsonResponse);
                    if (mainObject.has("product") && !mainObject.isNull("product")) {
                        JSONObject productObject = mainObject.getJSONObject("product");

                        if (productObject.has("product_name") && !productObject.isNull("product_name")) {
                            foodName = productObject.getString("product_name");
                        } else {
                            foodName = "Unrecognized Food (No name in API)"; // If no product name in API response
                        }

                        if (productObject.has("nutriments") && !productObject.isNull("nutriments")) {
                            JSONObject nutriments = productObject.getJSONObject("nutriments");

                            // Extract values, provide defaults if not found
                            double sugars = nutriments.optDouble("sugars_100g", 0.0);
                            double salt = nutriments.optDouble("salt_100g", 0.0);
                            double fat = nutriments.optDouble("fat_100g", 0.0);
                            String nutriscore = productObject.optString("nutriscore_grade", "unknown").toUpperCase(Locale.ROOT);

                            sugarLevel = String.format(Locale.getDefault(), "%.1fg", sugars);
                            saltLevel = String.format(Locale.getDefault(), "%.2fg", salt);
                            fatLevel = String.format(Locale.getDefault(), "%.1fg", fat);

                            // Determine health status based on simplified thresholds and Nutri-Score
                            if (nutriscore.equals("A") || nutriscore.equals("B")) {
                                healthStatus = "Healthy";
                                tipsMessage = "Excellent choice! This food is healthy.";
                            } else if (nutriscore.equals("C")) {
                                healthStatus = "Moderate";
                                tipsMessage = "Okay for occasional consumption. Check portion sizes.";
                            } else if (nutriscore.equals("D") || nutriscore.equals("E")) {
                                healthStatus = "Unhealthy";
                                tipsMessage = "High in sugar, salt, or fat. Consume rarely.";
                            } else {
                                // Fallback if Nutri-Score is not available, use basic thresholds
                                if (sugars > 15.0 || salt > 1.0 || fat > 20.0) { // Example high thresholds
                                    healthStatus = "Unhealthy";
                                    tipsMessage = "Warning: High in certain nutrients. Consume in moderation.";
                                } else if (sugars < 5.0 && salt < 0.3 && fat < 5.0) { // Example low thresholds
                                    healthStatus = "Healthy";
                                    tipsMessage = "Good nutrient balance!";
                                } else {
                                    healthStatus = "Moderate";
                                    tipsMessage = "Check the full label for details.";
                                }
                            }
                        } else {
                            tipsMessage = "Nutrition data not found for " + foodName + ".";
                        }
                    } else {
                        tipsMessage = "No product data found in API response for " + foodName + ".";
                    }
                } catch (JSONException e) {
                    Log.e("FetchNutritionTask", "JSON parsing error: " + e.getMessage(), e);
                    tipsMessage = "Error parsing nutrition data.";
                }
            } else {
                tipsMessage = "No response from nutrition service or network error.";
            }

            // After getting data (real or mock), go to ResultActivity
            Intent intent = new Intent(ScanActivity.this, ResultActivity.class);
            intent.putExtra("food_name", foodName);
            intent.putExtra("sugar_level", sugarLevel);
            intent.putExtra("salt_level", saltLevel);
            intent.putExtra("fat_level", fatLevel);
            intent.putExtra("health_status", healthStatus);
            intent.putExtra("tips_message", tipsMessage);
            intent.putExtra("scan_date", scanDate);
            intent.putExtra("scan_location", scanLocation);
            startActivity(intent);
            finish(); // Close ScanActivity
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown(); // Stop the camera background tasks
        textRecognizer.close(); // Close the ML Kit recognizer to free up resources
    }
}