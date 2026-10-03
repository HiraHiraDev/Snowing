import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A stand-in for "some other app" that :benchmark scrolls under the snow (S2 in
// docs/PERFORMANCE.md). Never published; it only gives Macrobenchmark a list
// whose frames it can measure with the overlay on and off.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hirahira.snowing.scrollapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hirahira.snowing.scrollapp"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
