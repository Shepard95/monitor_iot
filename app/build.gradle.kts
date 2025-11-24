plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.gms.google-services") version "4.4.4"
}

android {
    namespace = "com.example.monitoriot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.monitoriot"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Versiones de Firebase especificadas manualmente para resolver el conflicto
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")

    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // Credential Manager dependencies
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // MPAndroidChart para gráficos generados por código
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Librerías estándar de Android UI y materiales
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
