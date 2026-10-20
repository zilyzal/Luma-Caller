plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.luma.caller"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.luma.caller"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.0"
    }

    // The keystore is never committed. CI creates it (see .github/workflows/build.yml);
    // without one, the release build is simply left unsigned.
    val keystore = file("luma-release.jks")
    val keystorePassword = System.getenv("LUMA_KEYSTORE_PASSWORD") ?: "luma-local"

    signingConfigs {
        if (keystore.exists()) {
            create("release") {
                storeFile = keystore
                storePassword = keystorePassword
                keyAlias = "luma"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystore.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
