package com.s23010749.nutriscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA_PERMISSIONS = 10;
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 11;
    private static final String[] REQUIRED_CAMERA_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String[] REQUIRED_LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private PreviewView viewFinder;
    private Button buttonCaptureImage; // Button to trigger OCR fallback
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentKnownLocation = "Unknown Location";

    // ML Kit
    private TextRecognizer textRecognizer;
    private BarcodeScanner barcodeScanner;

    // Networking
    private OpenFoodFactsApi api;

    // State flag to prevent multiple scans
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage);

        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize ML Kit Barcode Scanner
        BarcodeScannerOptions barcodeOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(barcodeOptions);

        // Initialize Retrofit for Open Food Facts
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(OpenFoodFactsApi.class);

        if (allCameraPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_CAMERA_PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSIONS);
        }

        if (allLocationPermissionsGranted()) {
            getLastLocation();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION_PERMISSIONS);
        }

        // The capture button now serves as the OCR fallback trigger
        buttonCaptureImage.setOnClickListener(v -> captureAndRecognizeText());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(viewFinder.getDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // ImageAnalysis for barcode scanning
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageForBarcode);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera start failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Analyzer for Barcode Detection
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageForBarcode(@NonNull ImageProxy imageProxy) {
        if (isScanned) {
            imageProxy.close();
            return;
        }
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && !isScanned) {
                            isScanned = true; // Stop further processing
                            String rawValue = barcodes.get(0).getRawValue();
                            Log.d("ScanActivity", "Barcode detected: " + rawValue);

                            // Fetch data from Open Food Facts
                            fetchFromOpenFoodFacts(rawValue);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ScanActivity", "Barcode scan failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    // Fetch product data from Open Food Facts API
    private void fetchFromOpenFoodFacts(String barcode) {
        Toast.makeText(this, "Product found! Fetching nutrition data...", Toast.LENGTH_SHORT).show();

        String userAgent = "NutriScanApp/1.0 (contact@nutriscan.com)"; // Required format
        String scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        api.getProductByBarcode(barcode, userAgent).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                    ProductResponse.Product product = response.body().getProduct();
                    if (product != null && product.getNutriments() != null) {
                        // Successfully got product data
                        String foodName = product.getProductName() != null ? product.getProductName() : "Unknown Product";
                        double sugar = product.getNutriments().getSugars100g();
                        double fat = product.getNutriments().getFat100g();
                        double salt = product.getNutriments().getSalt100g();
                        String nutriscore = product.getNutriscoreGrade();

                        // Determine health status
                        String[] healthResult = determineHealthStatus(sugar, fat, salt, nutriscore);

                        navigateToResult(foodName, sugar, fat, salt, healthResult[0], healthResult[1], scanDate);
                    } else {
                        // Product found but no nutrition data
                        showErrorAndRetry("Product found, but no nutrition data available. Try OCR fallback.");
                    }
                } else {
                    // Product not found in database
                    showErrorAndRetry("Product not found in Open Food Facts. Try OCR fallback.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                Log.e("ScanActivity", "API call failed", t);
                // Network error -> Automatically trigger OCR fallback
                Toast.makeText(ScanActivity.this, "Network error. Falling back to OCR.", Toast.LENGTH_SHORT).show();
                isScanned = false; // Reset flag
                captureAndRecognizeText(); // Trigger OCR
            }
        });
    }

    // --- OCR Fallback Logic ---
    private void captureAndRecognizeText() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Capturing image for text recognition...", Toast.LENGTH_SHORT).show();

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                processImageForTextRecognition(imageProxy);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("ScanActivity", "Image capture failed", exception);
                // Ultimate fallback: Navigate with default values
                navigateToResultWithFallback("Unknown Food (Capture Error)", "0g", "0g", "0g", "Unknown", "Could not capture image.");
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageForTextRecognition(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String recognizedText = result.getText();
                        Log.d("ScanActivity", "OCR Text: " + recognizedText);
                        // Parse nutrition values using Regex
                        parseNutritionFromText(recognizedText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ScanActivity", "OCR failed", e);
                        navigateToResultWithFallback("Unknown Food (OCR Failed)", "0g", "0g", "0g", "Unknown", "Could not read label.");
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
            navigateToResultWithFallback("Unknown Food", "0g", "0g", "0g", "Unknown", "Image was null.");
        }
    }

    // Regex to extract Sugar, Fat, and Salt from OCR text
    private void parseNutritionFromText(String text) {
        double sugar = 0, fat = 0, salt = 0;
        boolean found = false;

        // Patterns look for "Sugar", "Fat", "Salt" followed by a number and "g"
        // Example: "Sugar 10.5g", "Fat: 5g", "Salt - 0.3g"
        Pattern sugarPattern = Pattern.compile("(?i)sugars?\\s*[:\\-]?\\s*(\\d+(?:\\.\\d+)?)\\s*g");
        Pattern fatPattern = Pattern.compile("(?i)fats?\\s*[:\\-]?\\s*(\\d+(?:\\.\\d+)?)\\s*g");
        Pattern saltPattern = Pattern.compile("(?i)salt\\s*[:\\-]?\\s*(\\d+(?:\\.\\d+)?)\\s*g");

        Matcher sugarMatcher = sugarPattern.matcher(text);
        Matcher fatMatcher = fatPattern.matcher(text);
        Matcher saltMatcher = saltPattern.matcher(text);

        if (sugarMatcher.find()) { sugar = Double.parseDouble(sugarMatcher.group(1)); found = true; }
        if (fatMatcher.find()) { fat = Double.parseDouble(fatMatcher.group(1)); found = true; }
        if (saltMatcher.find()) { salt = Double.parseDouble(saltMatcher.group(1)); found = true; }

        String scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        if (found) {
            // Determine health status based on parsed values
            String[] healthResult = determineHealthStatus(sugar, fat, salt, null);
            navigateToResult("Scanned Food (OCR)", sugar, fat, salt, healthResult[0], healthResult[1], scanDate);
        } else {
            // If no values found, use fallback
            navigateToResultWithFallback("Scanned Food (OCR)", "0g", "0g", "0g", "Unknown", "Could not parse nutrition values from label. Please try again.");
        }
    }

    // --- Shared Logic for Health Classification ---
    // Uses Nutri-Score if available, otherwise applies nutrient thresholds.
    private String[] determineHealthStatus(double sugar, double fat, double salt, String nutriscore) {
        // Priority 1: Use Nutri-Score if available from API
        if (nutriscore != null) {
            switch (nutriscore.toLowerCase()) {
                case "a":
                case "b":
                    return new String[]{"Healthy", "Excellent choice! This food is healthy."};
                case "c":
                    return new String[]{"Moderate", "Okay for occasional consumption. Check portion sizes."};
                case "d":
                case "e":
                    return new String[]{"Unhealthy", "High in sugar, salt, or fat. Consume rarely."};
            }
        }

        // Priority 2: Use nutrient thresholds (for OCR fallback or if Nutri-Score is missing)
        if (sugar > 13.5 || fat > 5.0 || salt > 1.5) {
            return new String[]{"Unhealthy", "Warning: High in certain nutrients. Consume in moderation."};
        } else if (sugar <= 5.0 && fat <= 3.0 && salt <= 0.3) {
            return new String[]{"Healthy", "Good nutrient balance!"};
        } else {
            return new String[]{"Moderate", "Check the full label for details."};
        }
    }

    // Helper to navigate to ResultActivity
    private void navigateToResult(String foodName, double sugar, double fat, double salt, String healthStatus, String tips, String date) {
        Intent intent = new Intent(ScanActivity.this, ResultActivity.class);
        intent.putExtra("food_name", foodName);
        intent.putExtra("sugar_level", String.format(Locale.getDefault(), "%.1fg", sugar));
        intent.putExtra("salt_level", String.format(Locale.getDefault(), "%.2fg", salt));
        intent.putExtra("fat_level", String.format(Locale.getDefault(), "%.1fg", fat));
        intent.putExtra("health_status", healthStatus);
        intent.putExtra("tips_message", tips);
        intent.putExtra("scan_date", date);
        intent.putExtra("scan_location", currentKnownLocation);
        startActivity(intent);
        finish();
    }

    private void navigateToResultWithFallback(String foodName, String sugar, String fat, String salt, String healthStatus, String tips) {
        String scanDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        Intent intent = new Intent(ScanActivity.this, ResultActivity.class);
        intent.putExtra("food_name", foodName);
        intent.putExtra("sugar_level", sugar);
        intent.putExtra("salt_level", salt);
        intent.putExtra("fat_level", fat);
        intent.putExtra("health_status", healthStatus);
        intent.putExtra("tips_message", tips);
        intent.putExtra("scan_date", scanDate);
        intent.putExtra("scan_location", currentKnownLocation);
        startActivity(intent);
        finish();
    }

    private void showErrorAndRetry(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        isScanned = false; // Reset the scan flag so the user can try again
    }

    // --- Location & Permission Methods (Unchanged from your original) ---
    private boolean allLocationPermissionsGranted() {
        for (String permission : REQUIRED_LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean allCameraPermissionsGranted() {
        for (String permission : REQUIRED_CAMERA_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getLastLocation() {
        if (allLocationPermissionsGranted()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentKnownLocation = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", location.getLatitude(), location.getLongitude());
                        } else {
                            // Fallback to Geocoder if possible, or keep default
                            currentKnownLocation = "Unknown Location";
                        }
                    });
        } else {
            currentKnownLocation = "Location Permission Denied";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS) {
            if (allCameraPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission needed!", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            if (allLocationPermissionsGranted()) {
                getLastLocation();
            } else {
                currentKnownLocation = "Location Denied";
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
        barcodeScanner.close();
    }
}