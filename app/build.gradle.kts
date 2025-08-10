import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()

// Tenta carregar as propriedades das variáveis de ambiente primeiro
val storeFileEnv = System.getenv("KEYSTORE_PATH")
val storePasswordEnv = System.getenv("STORE_PASSWORD")
val keyAliasEnv = System.getenv("KEY_ALIAS")
val keyPasswordEnv = System.getenv("KEY_PASSWORD")

val isEnvConfigured = !storeFileEnv.isNullOrBlank() &&
        !storePasswordEnv.isNullOrBlank() &&
        !keyAliasEnv.isNullOrBlank() &&
        !keyPasswordEnv.isNullOrBlank()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.0"
    id("kotlin-kapt")
}

// Se as variáveis de ambiente não estiverem completas, tenta o arquivo local
if (!isEnvConfigured) {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { keystoreProperties.load(it) }
    } else {
        println("⚠️ Arquivo keystore.properties não encontrado e variáveis de ambiente ausentes. A build de release pode falhar.")
    }
}

android {
    signingConfigs {
        create("release") {
            // Usa as variáveis de ambiente, se existirem. Caso contrário, usa as propriedades do arquivo.
            val storeFileProperty = storeFileEnv ?: keystoreProperties.getProperty("storeFile")
            storeFile = storeFileProperty?.let { file(it) }

            storePassword = storePasswordEnv ?: keystoreProperties.getProperty("storePassword")
            keyAlias = keyAliasEnv ?: keystoreProperties.getProperty("keyAlias")
            keyPassword = keyPasswordEnv ?: keystoreProperties.getProperty("keyPassword")
        }
    }

    namespace = "com.gabedev.mangako"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gabedev.mangako"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.ui.text.google.fonts)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Retrofit
    implementation(libs.retrofit)

    // Converter for JSON (e.g., Gson)
    implementation(libs.converter.gson)

    // Optional: OkHttp logging for debugging network requests
    implementation(libs.logging.interceptor)

    // Jetpack Compose integration
    implementation(libs.androidx.navigation.compose)

    // Views/Fragments integration
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Feature module support for Fragments
    implementation(libs.androidx.navigation.dynamic.features.fragment)

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation(libs.kotlinx.serialization.json)

    // Material core
    implementation(libs.androidx.material)

    // ícones estendidos
    implementation(libs.androidx.material.icons.extended)

    // Animation graphics
    implementation(libs.androidx.animation.graphics)

    // Material 3
    implementation(libs.material)

    // ROOM database
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)
}