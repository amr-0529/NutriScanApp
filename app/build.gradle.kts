plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.s23010749.nutriscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.s23010749.nutriscan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CameraX
    val camerax_version = "1.3.3"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Google Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ML Kit Barcode Scanning
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
}