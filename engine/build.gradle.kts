import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin snow simulation. No Android dependencies on purpose:
// it stays unit-testable on the JVM and cannot couple to UI code.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}
