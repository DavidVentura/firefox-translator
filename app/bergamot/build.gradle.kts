plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "dev.davidv.bergamot"
  compileSdk = 34
  ndkVersion = "27.0.12077973"
  buildToolsVersion = "34.0.0"

  defaultConfig {

    minSdk = 28 // iconv requirements from pathie-cpp
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
      }
    }
  }

  flavorDimensions += listOf("architecture")
  productFlavors {
    create("x86") {
      ndk {
        abiFilters += listOf("x86_64")
      }
      dimension = "architecture"
    }
    create("aarch64") {
      ndk {
        abiFilters += listOf("arm64-v8a")
      }
      dimension = "architecture"
    }
    create("armeabi-v7a") {
      ndk {
        abiFilters += listOf("armeabi-v7a")
      }
      dimension = "architecture"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      externalNativeBuild {
        cmake {
          arguments += listOf("-DCMAKE_BUILD_TYPE=Release")
        }
      }
    }
  }
  externalNativeBuild {
    cmake {
      path("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}
