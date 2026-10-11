plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.softwareparatodos.ladria"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.softwareparatodos.ladria"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("LADRIA_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("LADRIA_VERSION_NAME") ?: "0.1.0"
    }
    // La firma release se habilita únicamente cuando están presentes los secretos.
    val releaseKeystore = System.getenv("LADRIA_KEYSTORE_PATH")
    val releaseStorePassword = System.getenv("LADRIA_STORE_PASSWORD")
    val releaseAlias = System.getenv("LADRIA_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("LADRIA_KEY_PASSWORD")
    signingConfigs {
        if (listOf(releaseKeystore, releaseStorePassword, releaseAlias, releaseKeyPassword)
                .all { !it.isNullOrBlank() }) {
            create("ladriaRelease") {
                storeFile = file(releaseKeystore!!)
                storePassword = releaseStorePassword
                keyAlias = releaseAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("ladriaRelease")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    androidResources { noCompress += "tflite" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
