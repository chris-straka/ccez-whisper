plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "uk.ccez.whisper"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "uk.ccez.whisper"
        minSdk = 31
        targetSdk = 36
        versionCode = (System.getenv("CCEZ_WHISPER_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("CCEZ_WHISPER_VERSION_NAME") ?: "0.1.0-dev"
        // The phone only: Whisper on an emulator's x86 is too slow to use.
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    signingConfigs {
        // ccez-tube's key (on y8): the apps that may listen are named by key.
        val keystore = rootProject.file("release.keystore")
        if (keystore.exists()) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("CCEZ_WHISPER_KEY_PASSWORD")
                keyAlias = "ccez-tube"
                keyPassword = System.getenv("CCEZ_WHISPER_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
