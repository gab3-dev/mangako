package com.gabedev.mangako.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Criação do DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val VIEW_MODE = stringPreferencesKey("view_mode")
    val COLLECTION_DENSITY = intPreferencesKey("collection_density")
    val CATALOG_INTEGRATION = stringPreferencesKey("catalog_integration")
    val CATALOG_INTEGRATION_MIGRATED_TO_MANGAKO = booleanPreferencesKey(
        "catalog_integration_migrated_to_mangako"
    )
    val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    val NAVIGATION_BAR_STYLE = stringPreferencesKey("navigation_bar_style")
    val COVER_LANGUAGE_PREFERENCE = stringPreferencesKey("cover_language_preference")
}

enum class CatalogIntegration {
    MANGADEX,
    MANGAKO,
}

enum class NavigationBarStyle {
    CLASSIC,
    FLOATING,
}

enum class CoverLanguagePreference {
    JAPANESE,
    ORIGINAL,
    PORTUGUESE,
    ENGLISH,
    KOREAN,
    CHINESE,
    ALL,
}

// Função para salvar texto
suspend fun Context.saveConfigText(text: String) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.VIEW_MODE] = text
    }
}

// Função para ler texto como Flow
fun Context.getConfigText(): Flow<String> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.VIEW_MODE] ?: ""
    }
}

suspend fun Context.saveCollectionDensity(density: Int) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.COLLECTION_DENSITY] = density.coerceIn(1, 5)
    }
}

fun Context.getCollectionDensity(): Flow<Int> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.COLLECTION_DENSITY]?.coerceIn(1, 5) ?: 2
    }
}

suspend fun Context.saveCatalogIntegration(integration: CatalogIntegration) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.CATALOG_INTEGRATION] = integration.name
    }
}

suspend fun Context.saveNavigationBarStyle(style: NavigationBarStyle) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.NAVIGATION_BAR_STYLE] = style.name
    }
}

fun Context.getNavigationBarStyle(): Flow<NavigationBarStyle> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.NAVIGATION_BAR_STYLE]
            ?.let { value -> runCatching { NavigationBarStyle.valueOf(value) }.getOrNull() }
            ?: NavigationBarStyle.CLASSIC
    }
}

suspend fun Context.saveCoverLanguagePreference(preference: CoverLanguagePreference) {
    dataStore.edit { settings ->
        settings[SettingsKeys.COVER_LANGUAGE_PREFERENCE] = preference.name
    }
}

fun Context.getCoverLanguagePreference(): Flow<CoverLanguagePreference> {
    return dataStore.data.map { settings ->
        settings[SettingsKeys.COVER_LANGUAGE_PREFERENCE]
            ?.let { value -> runCatching { CoverLanguagePreference.valueOf(value) }.getOrNull() }
            ?: CoverLanguagePreference.JAPANESE
    }
}

fun Context.getCatalogIntegration(): Flow<CatalogIntegration> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.CATALOG_INTEGRATION]
            ?.let { value -> runCatching { CatalogIntegration.valueOf(value) }.getOrNull() }
            ?: CatalogIntegration.MANGAKO
    }
}

suspend fun Context.migrateCatalogIntegrationDefaultToMangaKo() {
    dataStore.edit { preferences ->
        if (preferences[SettingsKeys.CATALOG_INTEGRATION_MIGRATED_TO_MANGAKO] != true) {
            preferences[SettingsKeys.CATALOG_INTEGRATION] = CatalogIntegration.MANGAKO.name
            preferences[SettingsKeys.CATALOG_INTEGRATION_MIGRATED_TO_MANGAKO] = true
        }
    }
}

fun Context.getNotificationPermissionRequested(): Flow<Boolean> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.NOTIFICATION_PERMISSION_REQUESTED] ?: false
    }
}

suspend fun Context.saveNotificationPermissionRequested(requested: Boolean) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.NOTIFICATION_PERMISSION_REQUESTED] = requested
    }
}
