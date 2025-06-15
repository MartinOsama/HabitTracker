plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id ("com.google.gms.google-services")
}

android {
    namespace = "com.martinosama.habittracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.martinosama.habittracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.kizitonwose.calendar:compose:2.4.0")
    implementation ("io.ktor:ktor-client-core:2.3.4")
    implementation ("io.ktor:ktor-client-cio:2.3.4")
    implementation ("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation ("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.compose.material3:material3:<version>")
    implementation ("com.google.firebase:firebase-firestore:24.8.1")
    implementation ("com.google.firebase:firebase-auth:22.2.0")
    implementation ("com.google.accompanist:accompanist-pager:0.30.1")
    implementation ("com.google.accompanist:accompanist-pager-indicators:0.30.1")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.3.1")
    implementation ("com.google.accompanist:accompanist-flowlayout:0.28.0")
    implementation ("com.stripe:stripe-android:20.20.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation (platform("com.google.firebase:firebase-bom:32.2.0"))
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.31.2-alpha")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.compose.foundation:foundation:1.2.0")
    implementation ("io.appwrite:sdk-for-android:5.1.0")
    implementation ("com.airbnb.android:lottie-compose:6.0.0")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("io.coil-kt:coil-gif:2.4.0")
    implementation ("com.google.code.gson:gson:2.9.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("androidx.compose.material3:material3:1.0.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.material3:material3:1.1.1")
    implementation ("androidx.compose.ui:ui:1.4.0")
    implementation ("androidx.compose.material3:material3:1.1.0")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.core:core-ktx:1.13.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation ("androidx.activity:activity-compose:1.9.0")
    implementation ("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation (platform("androidx.compose:compose-bom:2023.08.00"))
    implementation ("com.google.android.material:material:1.2.0")
    implementation ("androidx.compose.material:material:1.5.1")
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.material3:material3:1.0.0")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.4")
    implementation ("com.auth0.android:jwtdecode:2.0.1")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation ("androidx.navigation:navigation-compose:2.5.3")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation (platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")
}