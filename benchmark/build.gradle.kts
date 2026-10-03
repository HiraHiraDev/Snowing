import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Macrobenchmarks (docs/PERFORMANCE.md). Test-only: nothing here ships in the app.
// Snowing itself is installed separately (:app:installBenchmark) and driven over adb
// through PerfControlActivity, the same way docs/perf/measure.sh does.
plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.hirahira.snowing.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Matches the profileable "benchmark" build type of :scrollapp.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":scrollapp"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
