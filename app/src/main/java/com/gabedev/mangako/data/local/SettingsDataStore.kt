package com.gabedev.mangako.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Criação do DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val VIEW_MODE = stringPreferencesKey("view_mode")
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
