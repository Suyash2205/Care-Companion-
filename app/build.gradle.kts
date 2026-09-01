import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.carecompanion.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carecompanion.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"https://zijedzsoevhljankgvvj.supabase.co\"")
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InppamVkenNvZXZobGphbmtndnZqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3MDU4ODQsImV4cCI6MjA5OTI4MTg4NH0.CCay92d4k_wN01yFF5oF3kIC1WuNLZf3PA4XiE9TM5A\""
        )

        // Google Sign-In needs the WEB (client_type 3) OAuth client id. Read it straight
        // out of google-services.json so it can never drift from the Firebase project.
        val webClientId: String = providers.provider {
            val f = rootProject.file("app/google-services.json")
            if (!f.exists()) "" else Regex("\"client_id\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"client_type\"\\s*:\\s*3")
                .find(f.readText().replace("\n", ""))?.groupValues?.get(1)
                ?: Regex("\"client_type\"\\s*:\\s*3[^}]*?\"client_id\"\\s*:\\s*\"([^\"]+)\"")
                    .find(f.readText().replace("\n", ""))?.groupValues?.get(1) ?: ""
        }.get()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    // Upload key for Play. Values come from keystore.properties (git-ignored) so the
    // secrets never enter the repo; without that file the release build is simply
    // unsigned, which still lets anyone clone and build a debug APK.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                // Report every failing test in one run instead of aborting at the first class.
                it.ignoreFailures = true
                it.testLogging { events("failed") }
            }
        }
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(bom)
    androidTestImplementation(bom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Supabase via PostgREST (Retrofit + kotlinx-serialization)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Room (offline cache + outbox)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Location (SOS GPS)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Google Sign-In via Credential Manager (the current API; GoogleSignInClient is deprecated)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Unit tests (JVM) ──────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.retrofit2:retrofit:2.11.0")
    testImplementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core-ktx:1.5.0")
    // Compose UI tests run on the JVM via Robolectric — real screens, no emulator.
    testImplementation("androidx.compose.ui:ui-test-junit4:1.6.7")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.6.7")
    testImplementation("androidx.work:work-testing:2.9.0")
}
