import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Word suggestions (LLD-12 §2): pure Kotlin/JVM, no other module, no library.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(libs.junit)
}
