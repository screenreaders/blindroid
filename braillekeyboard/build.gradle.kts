plugins {
    id("com.android.library")
}

android {
    namespace = "com.screenreaders.blindroid.braillekeyboard"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
}
