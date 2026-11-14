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
        versionCode = 3
        versionName = "0.1.0-alpha.3"
    }

    // One edition (owner, issue #251): no product flavors.

    // Release signing (FSS#28): the key and passwords come only from the Gradle properties below, normally in the
    // user's ~/.gradle/gradle.properties — never from this repository. Without all four the release build is left
    // unsigned as before; debug builds and tests never read them.
    val keystore = providers.gradleProperty("FSS_KEYSTORE").orNull
    val keystorePassword = providers.gradleProperty("FSS_KEYSTORE_PASSWORD").orNull
    val keyAlias = providers.gradleProperty("FSS_KEY_ALIAS").orNull
    val keyPassword = providers.gradleProperty("FSS_KEY_PASSWORD").orNull
    val releaseSigning = listOf(keystore, keystorePassword, keyAlias, keyPassword).all { !it.isNullOrBlank() }

    signingConfigs {
        if (releaseSigning) {
            create("release") {
                storeFile = file(keystore!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigning) signingConfig = signingConfigs.getByName("release")
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
    // Compose UI tests under Robolectric, test only, Apache-2.0 (FSS#44); the host activity is registered in the test (no manifest library in the APK)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
