plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.classcrush"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.classcrush"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.database)
    implementation(libs.material3)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.firebase:firebase-database:22.0.0")
    implementation("com.google.firebase:firebase-auth:24.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.1")


    // Add FCM dependency
    implementation("com.google.firebase:firebase-messaging:25.0.0")

    implementation("com.google.firebase:firebase-firestore:26.0.0")

    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Swipe Cards

    // JSON for parsing
    implementation("org.json:json:20231013")
    implementation("com.alexstyl.swipeablecard:swipeablecard:0.1.0")

    // Date Picker
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Added for View-based activities
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}