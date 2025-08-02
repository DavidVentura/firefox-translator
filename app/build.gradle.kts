plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.davidv.translator"
    compileSdk = 34
    ndkVersion = "27.0.12077973"
    buildToolsVersion = "34.0.0"

    sourceSets {
        getByName("androidTest") {
            assets {
                srcDirs("src/androidTest/assets")
            }
        }
    }
    defaultConfig {
        applicationId = "dev.davidv.translator"
        minSdk = 28 // iconv functions need 28?
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

        flavorDimensions += listOf("architecture")
        productFlavors {
            create("x86") {
                ndk {
                    abiFilters += listOf("x86_64") // armeabi-v7a arm64-v8a
                }
                dimension = "architecture"
            }
            create("aarch64") {
                ndk {
                    abiFilters += listOf("arm64-v8a")
                }
                dimension = "architecture"
            }
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":app:bergamot"))
    // OpenMP - Multi-threaded. Provides better performance on multi-core processors when using only single instance of Tesseract.
    implementation(("cz.adaptech.tesseract4android:tesseract4android-openmp:4.9.0"))
}
