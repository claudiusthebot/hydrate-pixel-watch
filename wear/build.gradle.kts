plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "rocks.claudiusthebot.watertracker.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "rocks.claudiusthebot.watertracker"
        minSdk = 34  // Wear OS 5+ only (Pixel Watch 3 / Wear OS 6 target)
        targetSdk = 36
        versionCode = 9
        versionName = "0.9.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/INDEX.LIST") }
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose for Wear — use BOM for ui/foundation alignment
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    // No material-icons dep on wear — drink icons live in WaterIcons.kt so
    // we don't pull the 40+ MB material-icons-extended catalogue.

    // Wear Compose Material 3 (Material 3 Expressive). 1.5.6 brings
    // MotionScheme.standard(), TransformingLazyColumn, AppCard, etc.
    implementation("androidx.wear.compose:compose-material3:1.5.6")
    implementation("androidx.wear.compose:compose-foundation:1.5.6")
    implementation("androidx.wear.compose:compose-navigation:1.5.6")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-tooling-preview:1.0.0")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    // Bridges kotlinx coroutines → ListenableFuture for the TileService API.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Wearable Data Layer (watch is a thin client — HC lives on the phone)
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    // Ongoing activity on watch face
    implementation("androidx.wear:wear-ongoing:1.0.0")

    // Wear OS Tile (swipe-able panel) — ProtoLayout 1.3.0 unlocks the
    // Material 3 Expressive `materialScope` / `primaryLayout` idiom that
    // mirrors the phone app's M3-Expressive design.
    implementation("androidx.wear.tiles:tiles:1.4.1")
    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-material3:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.3.0")
    implementation("com.google.guava:guava:32.1.3-android")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
