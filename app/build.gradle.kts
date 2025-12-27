plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Цей плагін замінює стару налаштування composeOptions
    alias(libs.plugins.kotlin.compose)
}

android {

    namespace = "com.needtools.oldtube"
    compileSdk = 35 // Можна використовувати останній для збірки

    defaultConfig {
        applicationId = "com.needtools.oldtube"
        minSdk = 21 // Lollipop
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Дозволяє використовувати можливості нових версій Java на старих Android
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    // УВАГА: Блок composeOptions ВИДАЛЕНО. У Kotlin 2.0 він не потрібен!
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Бібліотека плеєра
    implementation(libs.core)
    implementation(libs.ads.mobile.sdk)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}