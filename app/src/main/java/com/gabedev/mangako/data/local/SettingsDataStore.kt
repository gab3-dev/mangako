package com.gabedev.mangako.data.local

import android.content.Context
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
