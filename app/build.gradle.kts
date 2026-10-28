import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("release-signing.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val testBackendProperties = Properties().apply {
    val propertiesFile = rootProject.file("test-backend.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingValue(property: String, environment: String): String? =
    providers.environmentVariable(environment).orNull ?: releaseSigningProperties.getProperty(property)

fun quotedBuildConfig(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\r", "\\r")
    .replace("\n", "\\n") + "\""

android {
    namespace = "ru.coffeepoint"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.coffeepoint"
        minSdk = 26
        targetSdk = 37
        versionCode = 21
        versionName = "1.0.21"
        manifestPlaceholders["appLabel"] = "@string/app_name"
        manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        manifestPlaceholders["appRoundIcon"] = "@mipmap/ic_launcher_round"

        buildConfigField("boolean", "IS_TEST_ENVIRONMENT", "false")
        buildConfigField("String", "TEST_SHEETS_WEB_APP_URL", "\"\"")
        buildConfigField("String", "TEST_SHEETS_SYNC_TOKEN", "\"\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val configuredStore = signingValue("storeFile", "COFFEEPOINT_SIGNING_STORE_FILE")
            if (!configuredStore.isNullOrBlank()) {
                storeFile = rootProject.file(configuredStore)
                storePassword = signingValue("storePassword", "COFFEEPOINT_SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "COFFEEPOINT_SIGNING_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "COFFEEPOINT_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            if (providers.gradleProperty("ciBuild").orNull == "true") {
                applicationIdSuffix = ".ci"
                versionNameSuffix = "-ci"
                manifestPlaceholders["appLabel"] = "CoffeePoint TEST"
                manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_test"
                manifestPlaceholders["appRoundIcon"] = "@mipmap/ic_launcher_test"
                buildConfigField("boolean", "IS_TEST_ENVIRONMENT", "true")
                buildConfigField("String", "TEST_SHEETS_WEB_APP_URL", quotedBuildConfig(
                    providers.environmentVariable("COFFEEPOINT_TEST_WEB_APP_URL").orNull
                        ?.trim() ?: testBackendProperties.getProperty("webAppUrl", "").trim()
                ))
                buildConfigField("String", "TEST_SHEETS_SYNC_TOKEN", quotedBuildConfig(
                    providers.environmentVariable("COFFEEPOINT_TEST_SYNC_TOKEN").orNull
                        ?.trim() ?: testBackendProperties.getProperty("syncToken", "").trim()
                ))
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    ksp("androidx.room:room-compiler:2.7.2")

    implementation("androidx.core:core-ktx:1.17.0")

    val cameraxVersion = "1.4.2"

    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")
    implementation("com.google.guava:guava:33.2.1-android")

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.work:work-runtime-ktx:2.10.3")

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("io.coil-kt:coil-compose:2.7.0")
}
