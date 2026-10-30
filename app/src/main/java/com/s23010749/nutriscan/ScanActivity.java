package com.s23010749.nutriscan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ProgressBar progressBar;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private OpenFoodFactsApi api;
    private boolean isScanned = false;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        viewFinder = findViewById(R.id.viewFinder);
        progressBar = findViewById(R.id.progressBar);
        cameraExecutor = Executors.newSingleThreadExecutor();

        barcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(OpenFoodFactsApi.class);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 10);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // UPDATED: Using the modern ResolutionSelector to remove the warning
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(new Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector) // Apply new selector
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::processImage);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);

                viewFinder.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        MeteringPointFactory factory = viewFinder.getMeteringPointFactory();
                        MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                        FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
                        camera.getCameraControl().startFocusAndMetering(action);
                    }
                    return true;
                });

            } catch (ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImage(@NonNull ImageProxy proxy) {
        if (isScanned) { proxy.close(); return; }
        Image mediaImage = proxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, proxy.getImageInfo().getRotationDegrees());
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && !isScanned) {
                            isScanned = true;
                            String barcode = barcodes.get(0).getRawValue();
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.VISIBLE);
                                fetchProduct(barcode);
                            });
                        }
                    })
                    .addOnCompleteListener(task -> proxy.close());
        } else { proxy.close(); }
    }

    private void fetchProduct(String barcode) {
        api.getProductByBarcode(barcode, "NutriScanApp/1.0").enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                    ProductResponse.Product p = response.body().getProduct();
                    if (p != null && p.getNutriments() != null) {
                        navigateToResult(p.getProductName(), p.getNutriments());
                        return;
                    }
                }
                showManualFallback();
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                showManualFallback();
            }
        });
    }

    private void showManualFallback() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Product not found. Add manually.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ManualEntryActivity.class));
            finish();
        });
    }

    private void navigateToResult(String name, ProductResponse.Nutriments n) {
        JSONObject json = new JSONObject();
        try {
            json.put("Energy", n.getEnergyKcal100g() + " kcal");
            json.put("Protein", n.getProteins100g() + "g");
            json.put("Carbohydrates", n.getCarbohydrates100g() + "g");
            json.put("Sugar", n.getSugars100g() + "g");
            json.put("Fat", n.getFat100g() + "g");
            json.put("Salt", n.getSalt100g() + "g");
        } catch (Exception e) { e.printStackTrace(); }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("food_name", name != null ? name : "Scanned Product");
        intent.putExtra("nutrients_json", json.toString());
        intent.putExtra("date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}