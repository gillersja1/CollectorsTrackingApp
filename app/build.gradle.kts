plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    jacoco
}

android {
    namespace = "com.example.collectorstrackingapp"
    compileSdk = 36

    defaultConfig {
        // What Google Play and Firebase know the app as. The Kotlin code keeps its original
        // com.example package (the namespace above): Play only looks at this.
        applicationId = "io.github.gillersja1.collectorstracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        // Points Firebase at the local emulators before any UI test runs (see FirebaseEmulatorRunner).
        testInstrumentationRunner = "com.example.collectorstrackingapp.FirebaseEmulatorRunner"
    }

    signingConfigs {
        // A shared debug key, committed on purpose: CI and Android Studio sign debug
        // builds with the same key, so a CI-built APK installs over a local build
        // (and vice versa) without uninstalling, which deletes the app's local data.
        // Debug keys aren't secret; never use this one for a Play Store release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Generates a JaCoCo report via createDebugUnitTestCoverageReport.
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    sourceSets {
        // Room's exported schemas, so migration tests can build each old version of the database.
        // Robolectric (the JVM unit tests) reads the debug build's assets, not test-only ones, so
        // they go in debug; release builds never include them.
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    lint {
        // Existing findings are recorded in lint-baseline.xml; anything new fails the build.
        baseline = file("lint-baseline.xml")
        abortOnError = true
        htmlReport = true
        xmlReport = true
    }
    testOptions {
        // Lets Robolectric-based tests (e.g. Room DAO tests) run on the JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    // Every database version's schema is saved here and committed, so migrations can be tested.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Image Loading
    implementation(libs.coil)

    // Code Scanner
    implementation(libs.play.services.code.scanner)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}

// Robolectric loads app classes through its own class loader; without these
// settings JaCoCo skips them and Room/repository tests report 0% coverage.
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
