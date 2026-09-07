package com.gabedev.mangako.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gabedev.mangako.backup.BackupScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Criação do DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val APP_THEME = stringPreferencesKey("app_theme")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val VIEW_MODE = stringPreferencesKey("view_mode")
    val COLLECTION_DENSITY = intPreferencesKey("collection_density")
    val CATALOG_INTEGRATION = stringPreferencesKey("catalog_integration")
    val CATALOG_INTEGRATION_MIGRATED_TO_MANGAKO = booleanPreferencesKey(
        "catalog_integration_migrated_to_mangako"
    )
    val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    val NAVIGATION_BAR_STYLE = stringPreferencesKey("navigation_bar_style")
    val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
    val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
    val LAST_BACKUP_AT = stringPreferencesKey("last_backup_at")
    val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
}

fun Context.getAppearancePreferences(): Flow<AppearancePreferences> = flow {
    var hasValue = false
    emitAll(dataStore.data
        .onEach { hasValue = true }
        .retryWhen { error, _ ->
            if (error !is IOException) return@retryWhen false
            // Allow startup on a read failure, then recover without resetting a valid theme.
            if (!hasValue) {
                emit(emptyPreferences())
                hasValue = true
            }
            delay(1_000)
            true
        }
        .map { AppearancePreferences.fromStored(it[SettingsKeys.APP_THEME], it[SettingsKeys.THEME_MODE]) })
}

suspend fun Context.saveAppTheme(theme: AppTheme) {
    dataStore.edit { it[SettingsKeys.APP_THEME] = theme.name }
}

suspend fun Context.saveThemeMode(mode: ThemeMode) {
    dataStore.edit { it[SettingsKeys.THEME_MODE] = mode.name }
}

enum class CatalogIntegration {
    MANGADEX,
    MANGAKO,
}

enum class NavigationBarStyle {
    CLASSIC,
    FLOATING,
}

enum class BackupFrequency {
    ON_CHANGE,
    DAILY,
    WEEKLY,
}

data class BackupPreferences(
    val treeUri: String?,
    val frequency: BackupFrequency,
    val onboardingCompleted: Boolean,
    val lastBackupAt: String?,
    val lastBackupError: String?,
)

// Função para salvar texto
suspend fun Context.saveConfigText(text: String) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.VIEW_MODE] = text
    }
    BackupScheduler.enqueueAfterChange(applicationContext)
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
    BackupScheduler.enqueueAfterChange(applicationContext)
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
    BackupScheduler.enqueueAfterChange(applicationContext)
}

suspend fun Context.saveNavigationBarStyle(style: NavigationBarStyle) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.NAVIGATION_BAR_STYLE] = style.name
    }
    BackupScheduler.enqueueAfterChange(applicationContext)
}

fun Context.getNavigationBarStyle(): Flow<NavigationBarStyle> {
    return dataStore.data.map { preferences ->
        preferences[SettingsKeys.NAVIGATION_BAR_STYLE]
            ?.let { value -> runCatching { NavigationBarStyle.valueOf(value) }.getOrNull() }
            ?: NavigationBarStyle.CLASSIC
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

fun Context.getBackupPreferences(): Flow<BackupPreferences> {
    return dataStore.data.map { preferences ->
        BackupPreferences(
            treeUri = preferences[SettingsKeys.BACKUP_TREE_URI],
            frequency = preferences[SettingsKeys.BACKUP_FREQUENCY]
                ?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() }
                ?: BackupFrequency.ON_CHANGE,
            onboardingCompleted = noBackupFilesDir.resolve(BACKUP_ONBOARDING_MARKER).exists(),
            lastBackupAt = preferences[SettingsKeys.LAST_BACKUP_AT],
            lastBackupError = preferences[SettingsKeys.LAST_BACKUP_ERROR],
        )
    }
}

suspend fun Context.saveBackupTreeUri(uri: String?) {
    val previousUri = dataStore.data.first()[SettingsKeys.BACKUP_TREE_URI]
    dataStore.edit { preferences ->
        if (uri == null) {
            preferences.remove(SettingsKeys.BACKUP_TREE_URI)
        } else {
            preferences[SettingsKeys.BACKUP_TREE_URI] = uri
        }
    }
    if (previousUri != null && previousUri != uri) {
        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(previousUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }
}

suspend fun Context.saveBackupFrequency(frequency: BackupFrequency) {
    dataStore.edit { preferences ->
        preferences[SettingsKeys.BACKUP_FREQUENCY] = frequency.name
    }
    BackupScheduler.configure(applicationContext, frequency)
}

suspend fun Context.completeBackupOnboarding() {
    noBackupFilesDir.resolve(BACKUP_ONBOARDING_MARKER).createNewFile()
}

private const val BACKUP_ONBOARDING_MARKER = "backup_onboarding_completed"
