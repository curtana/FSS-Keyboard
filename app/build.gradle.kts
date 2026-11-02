// AGP 9 compiles Kotlin itself (built-in Kotlin); `org.jetbrains.kotlin.android` must not be applied.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    // D-01: `kbd.app` is a temporary application ID root.
    namespace = "kbd.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kbd.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"
    }

    // One edition (owner, issue #251): the swipe engine is in every build; no product flavors.

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // T36 (gate G1): Robolectric view tests need the merged resources.
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric sets `FileDescriptor.fd` through `jdk.internal.access.SharedSecrets`; newer JDKs need it exported.
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

dependencies {
    implementation(project(":core-hangul"))
    implementation(project(":stroke-core"))
    implementation(project(":recognizer"))
    implementation(project(":gesture-engine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.customview)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)   // test only, MIT (issue #12)
}
