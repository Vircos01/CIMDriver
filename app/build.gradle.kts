import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cimdriver.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cimdriver.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 14
        versionName = "1.1.3-${getGitHash(providers)}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
        
        buildConfigField("String", "GIT_HASH", "\"${getGitHash(providers)}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }

            val keystoreFile = keystoreProperties.getProperty("RELEASE_STORE_FILE") ?: project.findProperty("RELEASE_STORE_FILE") as String? ?: System.getenv("RELEASE_STORE_FILE")
            val keystorePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD") ?: project.findProperty("RELEASE_STORE_PASSWORD") as String? ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keystoreKeyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS") ?: project.findProperty("RELEASE_KEY_ALIAS") as String? ?: System.getenv("RELEASE_KEY_ALIAS")
            val keystoreKeyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD") ?: project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (keystoreFile != null && rootProject.file(keystoreFile).exists()) {
                storeFile = rootProject.file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keystoreKeyAlias
                keyPassword = keystoreKeyPassword
            } else {
                // Fallback to debug keystore so release builds can succeed without secrets
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.maplibre.sdk)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Car App Library
    implementation(libs.androidx.car.app)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Glance for Widget
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
    
    // Play Services Location (for Geofencing)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun getGitHash(providers: org.gradle.api.provider.ProviderFactory): String {
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        "unknown"
    }
}
