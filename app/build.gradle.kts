import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val keystoreProperties = Properties()
val localProperties = Properties()

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
    alias(libs.plugins.kover)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.google.devtools.ksp")
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

val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun localPropertyOrEnv(name: String, defaultValue: String): String {
    return localProperties.getProperty(name)
        ?: System.getenv(name)
        ?: defaultValue
}

fun mangaKoApiToken(): String {
    return localProperties.getProperty("MANGAKO_API_TOKEN")
        ?: System.getenv("MANGAKO_API_TOKEN")
        ?: ""
}

fun String.toBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

gradle.taskGraph.whenReady {
    val releaseAppBuildRequested = allTasks.any { task ->
        task.project == project && task.name in setOf(
            "assemble",
            "assembleRelease",
            "build",
            "bundle",
            "bundleRelease",
            "installRelease",
            "packageRelease",
            "packageReleaseBundle",
            "packageReleaseUniversalApk",
        )
    }

    if (releaseAppBuildRequested && mangaKoApiToken().isBlank()) {
        throw GradleException("MANGAKO_API_TOKEN is required to build a release APK or bundle.")
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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gabedev.mangako"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.gabedev.mangako.MangaKoE2ETestRunner"
        buildConfigField(
            "String",
            "MANGAKO_API_BASE_URL",
            localPropertyOrEnv(
                "MANGAKO_API_BASE_URL",
                "https://mangako-api.kostudio.io/"
            ).toBuildConfigString()
        )
        buildConfigField(
            "String",
            "MANGAKO_API_TOKEN",
            mangaKoApiToken().toBuildConfigString()
        )
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "MangaKō Debug")
        }

        release {
            signingConfig = signingConfigs.getByName("release")
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.Manifest",
                    "*.Manifest.*",
                    "*.R",
                    "*.R.*",
                    "*.*_Impl",
                    "*.*_Impl*",
                    "*.ComposableSingletons*",
                    "com.gabedev.mangako.MainActivity*",
                    "com.gabedev.mangako.MangaKoApplication",
                    "com.gabedev.mangako.Screen*",
                    "com.gabedev.mangako.core.CrashHandler",
                    "com.gabedev.mangako.core.FileLogger",
                    "com.gabedev.mangako.ui.screens.collection.MangaCollectionKt*",
                    "com.gabedev.mangako.ui.screens.detail.MangaDetailKt*",
                    "com.gabedev.mangako.ui.screens.search_list.MangaSearchListKt*",
                    "com.gabedev.mangako.ui.screens.settings.*",
                )
                packages(
                    "com.gabedev.mangako.background",
                    "com.gabedev.mangako.data.dao",
                    "com.gabedev.mangako.data.local",
                    "com.gabedev.mangako.data.remote.api",
                    "com.gabedev.mangako.ui.components",
                    "com.gabedev.mangako.ui.screens.detail.covertheme",
                    "com.gabedev.mangako.ui.theme",
                )
            }
        }
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
    implementation(libs.androidx.compose.runtime.saveable)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockwebserver)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Extract colors from manga covers
    implementation(libs.palette)

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
    ksp(libs.androidx.room.compiler)

    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // Icons
    implementation(libs.androidx.material.icons.extended)
}
