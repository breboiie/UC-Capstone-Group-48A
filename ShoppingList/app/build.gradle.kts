plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Sharing between phones needs Firebase. Drop google-services.json into this folder
// to switch it on (see README). Without it the app builds and runs with the list
// kept on the phone, and "Connect a phone" explains that sharing is not set up.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "au.edu.canberra.team48.shoppinglist"
    compileSdk = 35

    defaultConfig {
        applicationId = "au.edu.canberra.team48.shoppinglist"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            // Demo mode: two months of pretend shopping history so Shopping suggestions
            // and "usual items first" can be shown straight away, and a test reminder button.
            buildConfigField("boolean", "DEMO_MODE", "true")
        }
        release {
            buildConfigField("boolean", "DEMO_MODE", "false")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.android)

    // QR code: ZXing draws "My code". The scanner is inside the app: CameraX shows the camera
    // and ML Kit's bundled barcode model reads the code (nothing to download first).
    implementation(libs.zxing.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    // CameraX hands back Guava's ListenableFuture. Another library brings an empty placeholder
    // for it ("listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"), so Guava itself is needed.
    implementation(libs.guava)
    implementation(libs.mlkit.barcode.scanning)
    // "My number" on sign up: offers the phone's own number (Phone Number Hint). No permission needed.
    implementation(libs.play.services.auth)
    // Reads the words on a packet, on the phone. The model is bundled, so it works offline.
    implementation(libs.mlkit.text.recognition)

    // Real-time sync. Anonymous sign-in: nobody types a password.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    // Photo: Gemini says what the photo shows (Firebase AI Logic). App Check proves the request
    // comes from this app: the debug provider in debug builds, Play Integrity in release builds.
    implementation(libs.firebase.ai)
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)

    testImplementation(libs.junit)
}
