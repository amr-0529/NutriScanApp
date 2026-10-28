package com.s23010749.nutriscan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private static final String[] REQUIRED_LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private PreviewView viewFinder;
    private View buttonCaptureImage;
    private FloatingActionButton fabFlashlight;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentKnownLocation = "Unknown Location";
    private Camera camera;
    private boolean isFlashlightOn = false;

    private TextRecognizer textRecognizer;
    private BarcodeScanner barcodeScanner;
    private OpenFoodFactsApi api;

    private boolean isScanned = false;
    private boolean isProcessingFallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage);
        fabFlashlight = findViewById(R.id.fabFlashlight);

        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        barcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build());

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://world.openfoodfacts.org/").addConverterFactory(GsonConverterFactory.create()).build();
        api = retrofit.create(OpenFoodFactsApi.class);

        if (allCameraPermissionsGranted()) startCamera();
        else ActivityCompat.requestPermissions(this, REQUIRED_CAMERA_PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSIONS);

        if (allLocationPermissionsGranted()) getLastLocation();
        else ActivityCompat.requestPermissions(this, REQUIRED_LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION_PERMISSIONS);

        fabFlashlight.setOnClickListener(v -> {
            if (camera != null) {
                isFlashlightOn = !isFlashlightOn;
                camera.getCameraControl().enableTorch(isFlashlightOn);
                fabFlashlight.setImageResource(isFlashlightOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
            }
        });

        viewFinder.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP && camera != null) {
                MeteringPointFactory factory = viewFinder.getMeteringPointFactory();
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(2, TimeUnit.SECONDS).build();
                camera.getCameraControl().startFocusAndMetering(action);
            }
            return true;
        });

        buttonCaptureImage.setOnClickListener(v -> {
            if (!isProcessingFallback) {
                isProcessingFallback = true;
                Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show();
                captureAndRecognizeText();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new android.util.Size(1920, 1080))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageForBarcode);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera Error", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageForBarcode(@NonNull ImageProxy imageProxy) {
        if (isScanned || isProcessingFallback) { imageProxy.close(); return; }
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            barcodeScanner.process(image).addOnSuccessListener(barcodes -> {
                if (!barcodes.isEmpty() && !isScanned) {
                    isScanned = true;
                    fetchFromOpenFoodFacts(barcodes.get(0).getRawValue());
                }
            }).addOnCompleteListener(task -> imageProxy.close());
        } else { imageProxy.close(); }
    }

    private void fetchFromOpenFoodFacts(String barcode) {
        String userAgent = "NutriScanApp/1.0 (contact@nutriscan.com)";
        api.getProductByBarcode(barcode, userAgent).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                    ProductResponse.Product product = response.body().getProduct();
                    if (product != null && product.getNutriments() != null) {
                        String name = product.getProductName() != null ? product.getProductName() : "Unknown Product";
                        JSONObject nutrientsJson = new JSONObject();
                        try {
                            if (product.getNutriments().getEnergyKcal100g() > 0) nutrientsJson.put("Energy", String.format(Locale.getDefault(), "%.0f kcal", product.getNutriments().getEnergyKcal100g()));
                            if (product.getNutriments().getProteins100g() > 0) nutrientsJson.put("Protein", String.format(Locale.getDefault(), "%.1fg", product.getNutriments().getProteins100g()));
                            if (product.getNutriments().getCarbohydrates100g() > 0) nutrientsJson.put("Carbohydrates", String.format(Locale.getDefault(), "%.1fg", product.getNutriments().getCarbohydrates100g()));
                            if (product.getNutriments().getSugars100g() > 0) nutrientsJson.put("Sugar", String.format(Locale.getDefault(), "%.1fg", product.getNutriments().getSugars100g()));
                            if (product.getNutriments().getFat100g() > 0) nutrientsJson.put("Fat", String.format(Locale.getDefault(), "%.1fg", product.getNutriments().getFat100g()));
                            if (product.getNutriments().getSalt100g() > 0) nutrientsJson.put("Salt", String.format(Locale.getDefault(), "%.2fg", product.getNutriments().getSalt100g()));
                        } catch (JSONException e) { e.printStackTrace(); }

                        int[] scoreData = calculateHealthScore(product.getNutriments().getSugars100g(), product.getNutriments().getFat100g(), product.getNutriments().getSalt100g());
                        navigateToResult(name, nutrientsJson.toString(), "", scoreData[0], getTags(product.getNutriments().getSugars100g(), product.getNutriments().getFat100g(), product.getNutriments().getSalt100g(), product.getNutriments().getProteins100g()));
                    } else {
                        triggerOcrFallback("No nutrition data. Scanning label...");
                    }
                } else {
                    triggerOcrFallback("Product not found. Scanning label...");
                }
            }
            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                triggerOcrFallback("Network error. Scanning label...");
            }
        });
    }

    private void triggerOcrFallback(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
        isProcessingFallback = true;
        captureAndRecognizeText();
    }

    private void captureAndRecognizeText() {
        if (imageCapture == null) return;
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                processImageForTextRecognition(imageProxy);
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                navigateToResult("Unknown Food", "{}", "", -1, "Could not capture.");
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageForTextRecognition(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            textRecognizer.process(image).addOnSuccessListener(result -> parseAllText(result.getText())).addOnCompleteListener(task -> imageProxy.close());
        } else { imageProxy.close(); }
    }

    // --- THE NEW "SCAN EVERYTHING" ENGINE ---
    private void parseAllText(String rawText) {
        Map<String, String> nutrientMap = new LinkedHashMap<>();

        // Flatten the text into a single line so multi-column labels don't break the parser
        String flatText = rawText.replace("\n", " ").replace("\r", " ");

        // Regex looks for ANY nutrient keyword, followed by a number, followed by a unit.
        // It extracts the FIRST number it finds for each nutrient (which is usually the Per 100g value).
        Pattern pattern = Pattern.compile("(?i)(energy|calories|protein|carbohydrates?|sugars?|fat|saturates|saturated|trans fat|salt|sodium|fiber|cholesterol)[^0-9]{0,10}([0-9]+(?:[.,][0-9]+)?)\\s*(g|mg|kj|kcal|%)?");
        Matcher matcher = pattern.matcher(flatText);

        double parsedSugar = 0, parsedFat = 0, parsedSalt = 0, parsedProtein = 0;
        boolean foundAnything = false;

        while (matcher.find()) {
            String rawName = matcher.group(1).toLowerCase().trim();
            String valueStr = matcher.group(2).replace(",", ".");
            String unit = matcher.group(3) != null ? matcher.group(3).toLowerCase() : "g";

            // Clean up the name for the UI
            String displayName = capitalize(rawName);
            if (rawName.contains("carbohydrate")) displayName = "Carbohydrates";
            if (rawName.contains("energy") || rawName.contains("calorie")) displayName = "Energy";
            if (rawName.contains("saturates")) displayName = "Saturated Fat";
            if (rawName.contains("sodium")) displayName = "Sodium";

            // Prevent duplicates (e.g., if "Total Fat" and "Fat" are both found, keep the first one)
            if (!nutrientMap.containsKey(displayName)) {
                String formattedValue = valueStr + " " + (unit.equals("kj") ? "kJ" : unit);
                nutrientMap.put(displayName, formattedValue);
                foundAnything = true;
            }

            // Parse values for Health Score Calculation
            double val = Double.parseDouble(valueStr);
            if (unit.equals("mg")) val = val / 1000.0; // Convert mg to g

            if (rawName.contains("sugar")) parsedSugar = Math.max(parsedSugar, val);
            if (rawName.equals("fat") || rawName.contains("total fat")) parsedFat = Math.max(parsedFat, val);
            if (rawName.contains("salt")) parsedSalt = Math.max(parsedSalt, val);
            if (rawName.contains("sodium")) parsedSalt = Math.max(parsedSalt, val * 2.5);
            if (rawName.contains("protein")) parsedProtein = Math.max(parsedProtein, val);
        }

        // Extract Ingredients
        StringBuilder ingredients = new StringBuilder();
        String lowerText = rawText.toLowerCase();
        if (lowerText.contains("ingredients")) {
            int startIndex = lowerText.indexOf("ingredients") + 11;
            if (startIndex < rawText.length()) {
                String ingText = rawText.substring(startIndex);
                if (ingText.toLowerCase().contains("manufactured")) ingText = ingText.substring(0, ingText.toLowerCase().indexOf("manufactured"));
                ingredients.append(ingText.trim());
            }
        }

        // Product Name
        String productName = "Scanned Food Label";
        String[] lines = rawText.split("\n");
        for(int i=0; i<Math.min(4, lines.length); i++) {
            if(lines[i].trim().length() > 5 && !lines[i].toLowerCase().contains("nutrition") && !lines[i].toLowerCase().contains("serving")) {
                productName = lines[i].trim();
                break;
            }
        }

        String jsonString = new JSONObject(nutrientMap).toString();
        String ingredientsStr = ingredients.toString().replace("\n", " ").trim();

        if (!foundAnything) {
            navigateToResult(productName, "{}", ingredientsStr, -1, "No nutrients detected. Please ensure the label is well-lit and readable.");
        } else {
            int[] scoreData = calculateHealthScore(parsedSugar, parsedFat, parsedSalt);
            String tags = getTags(parsedSugar, parsedFat, parsedSalt, parsedProtein);
            navigateToResult(productName, jsonString, ingredientsStr, scoreData[0], tags);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private int[] calculateHealthScore(double sugar, double fat, double salt) {
        int score = 100;
        if (sugar > 13.5) score -= 30; else if (sugar > 5) score -= 10;
        if (fat > 5.0) score -= 25; else if (fat > 3) score -= 10;
        if (salt > 1.5) score -= 25; else if (salt > 0.3) score -= 10;
        score = Math.max(0, Math.min(100, score));
        return new int[]{score, score >= 80 ? 1 : (score >= 50 ? 2 : 3)};
    }

    private String getTags(double sugar, double fat, double salt, double protein) {
        StringBuilder tags = new StringBuilder();
        if (sugar > 13.5) tags.append("High Sugar|");
        if (fat > 5.0) tags.append("High Fat|");
        if (salt > 1.5) tags.append("High Salt|");
        if (protein > 5.0) tags.append("High Protein|");
        if (sugar <= 5.0 && fat <= 3.0 && salt <= 0.3) tags.append("Low Fat/Sugar|");
        return tags.toString();
    }

    private void navigateToResult(String name, String nutrientsJson, String ingredients, int score, String tags) {
        String healthStatus = score == -1 ? "Not Detected" : (score >= 80 ? "Healthy" : (score >= 50 ? "Moderate" : "Unhealthy"));
        Intent i = new Intent(this, ResultActivity.class);
        i.putExtra("food_name", name);
        i.putExtra("nutrients_json", nutrientsJson);
        i.putExtra("ingredients", ingredients);
        i.putExtra("health", healthStatus);
        i.putExtra("score", score);
        i.putExtra("tags", tags);
        i.putExtra("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
        i.putExtra("location", currentKnownLocation);
        startActivity(i);
        finish();
    }

    private boolean allLocationPermissionsGranted() {
        for (String p : REQUIRED_LOCATION_PERMISSIONS) if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private boolean allCameraPermissionsGranted() {
        for (String p : REQUIRED_CAMERA_PERMISSIONS) if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (allLocationPermissionsGranted()) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) currentKnownLocation = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", location.getLatitude(), location.getLongitude());
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS && allCameraPermissionsGranted()) startCamera();
        else if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS && allLocationPermissionsGranted()) getLastLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
        barcodeScanner.close();
    }
}