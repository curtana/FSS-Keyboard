import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":stroke-core"))
    implementation(project(":core-hangul"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(testFixtures(project(":stroke-core")))
    testImplementation(libs.junit)
}
