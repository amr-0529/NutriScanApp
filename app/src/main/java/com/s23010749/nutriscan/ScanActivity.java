package com.s23010749.nutriscan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.FocusMeteringAction;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

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

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA_PERMISSIONS = 10;
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 11;
    private static final String[] REQUIRED_CAMERA_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final String[] REQUIRED_LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private PreviewView viewFinder;
    private View buttonCaptureImage;
    private ProgressBar progressBar;
    private FloatingActionButton fabFlashlight;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private String currentKnownLocation = "Unknown Location";
    private Camera camera;
    private boolean isFlashlightOn = false;

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage);
        progressBar = findViewById(R.id.progressBar);
        fabFlashlight = findViewById(R.id.fabFlashlight);

        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

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
            buttonCaptureImage.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            captureAndProcessImage();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera Error", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndProcessImage() {
        if (imageCapture == null) return;

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                @OptIn(markerClass = ExperimentalGetImage.class)
                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                    textRecognizer.process(image)
                            .addOnSuccessListener(result -> parseFullLabel(result.getText()))
                            .addOnFailureListener(e -> runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                buttonCaptureImage.setEnabled(true);
                                Toast.makeText(ScanActivity.this, "OCR Failed. Try again.", Toast.LENGTH_SHORT).show();
                            }))
                            .addOnCompleteListener(task -> imageProxy.close());
                } else {
                    imageProxy.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonCaptureImage.setEnabled(true);
                    Toast.makeText(ScanActivity.this, "Capture failed.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void parseFullLabel(String text) {
        Map<String, String> nutrientMap = new LinkedHashMap<>();
        String[] lines = text.split("\n");

        double sugar = 0, fat = 0, salt = 0, protein = 0, carbs = 0;

        String productName = "Scanned Product";
        for (int i = 0; i < Math.min(6, lines.length); i++) {
            String line = lines[i].trim();
            String lower = line.toLowerCase();
            if (line.length() > 4 && !lower.contains("nutrition") && !lower.contains("serving") && !lower.contains("amount") && !lower.contains("per 100")) {
                productName = line;
                break;
            }
        }

        Pattern pattern = Pattern.compile("(?i)^\\s*([a-zA-Z\\s\\(\\)%]+?)\\s*[:\\-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*(g|mg|kcal|kj)?");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String rawName = matcher.group(1).trim().toLowerCase();
                String valStr = matcher.group(2).replace(",", ".");
                String unit = matcher.group(3) != null ? matcher.group(3).toLowerCase() : "";

                rawName = rawName.replaceAll("(?i)per\\s+100(g|ml)|typical\\s+values|amount|approx", "").trim();
                if (rawName.length() < 3 || rawName.contains("ingredient")) continue;

                double val = Double.parseDouble(valStr);
                String displayName = capitalize(rawName);
                String displayValue = valStr + (unit.isEmpty() ? "" : " " + unit);

                if (rawName.contains("energy") || rawName.contains("calorie")) displayName = "Energy";
                if (rawName.contains("carbohydrate")) displayName = "Carbohydrates";

                if (!nutrientMap.containsKey(displayName)) {
                    nutrientMap.put(displayName, displayValue);
                }

                if (unit.equals("mg")) val = val / 1000.0;

                if (rawName.contains("sugar")) sugar = Math.max(sugar, val);
                if (rawName.equals("fat") || rawName.contains("total fat")) fat = Math.max(fat, val);
                if (rawName.contains("salt")) salt = Math.max(salt, val);
                if (rawName.contains("sodium")) salt = Math.max(salt, val * 2.5);
                if (rawName.contains("protein")) protein = Math.max(protein, val);
                if (rawName.contains("carbohydrate")) carbs = Math.max(carbs, val);
            }
        }

        final String finalJsonString = new JSONObject(nutrientMap).toString();
        final int finalScore = nutrientMap.isEmpty() ? -1 : calculateHealthScore(sugar, fat, salt);
        final String finalHealthStatus = finalScore == -1 ? "Not Detected" :
                (finalScore >= 80 ? "Healthy" : (finalScore >= 50 ? "Moderate" : "Unhealthy"));
        final String finalTips = generateProfessionalTips(sugar, fat, salt, protein, carbs);
        final String finalProductName = productName;

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            buttonCaptureImage.setEnabled(true);

            Intent i = new Intent(ScanActivity.this, ResultActivity.class);
            i.putExtra("food_name", finalProductName);
            i.putExtra("nutrients_json", finalJsonString);
            i.putExtra("health", finalHealthStatus);
            i.putExtra("score", finalScore);
            i.putExtra("tips", finalTips);
            i.putExtra("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            i.putExtra("location", currentKnownLocation);
            startActivity(i);
            finish();
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String generateProfessionalTips(double sugar, double fat, double salt, double protein, double carbs) {
        StringBuilder tips = new StringBuilder();
        if (sugar > 13.5) tips.append("• High sugar content. Try to limit consumption.\n");
        else if (sugar <= 5.0 && sugar > 0) tips.append("• Low in sugar, great for a balanced diet.\n");

        if (fat > 5.0) tips.append("• High in fat. Watch your portion sizes.\n");
        else if (fat <= 3.0 && fat > 0) tips.append("• Low fat content, heart-healthy choice.\n");

        if (salt > 1.5) tips.append("• High sodium/salt. May increase blood pressure.\n");
        else if (salt <= 0.3 && salt > 0) tips.append("• Low sodium, good for blood pressure.\n");

        if (protein > 5.0) tips.append("• Good source of protein for muscle recovery.\n");
        if (carbs > 20.0) tips.append("• High in carbohydrates, provides quick energy.\n");

        if (tips.length() == 0 && (sugar > 0 || fat > 0 || salt > 0)) {
            return "This product has moderate nutrient levels. Consume as part of a balanced diet.";
        } else if (tips.length() == 0) {
            return "Could not analyze nutritional quality. Please ensure the label is clear.";
        }
        return tips.toString().trim();
    }

    private int calculateHealthScore(double sugar, double fat, double salt) {
        int score = 100;
        if (sugar > 13.5) score -= 30; else if (sugar > 5) score -= 10;
        if (fat > 5.0) score -= 25; else if (fat > 3) score -= 10;
        if (salt > 1.5) score -= 25; else if (salt > 0.3) score -= 10;
        return Math.max(0, Math.min(100, score));
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
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}