plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// D-02 (LLD-03 §2.1): one project-wide test command, `./gradlew testDebugUnitTest`.
// JVM modules only have `test`; the app has its own `testDebugUnitTest` (one edition since issue #251).
listOf(":core-hangul", ":stroke-core", ":recognizer").forEach { path ->
    project(path) {
        tasks.register("testDebugUnitTest") { dependsOn("test") }
    }
}
