package com.gabedev.mangako.data.local

enum class AppTheme { MANGAKO, DYNAMIC }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppearancePreferences(
    val theme: AppTheme = AppTheme.MANGAKO,
    val mode: ThemeMode = ThemeMode.SYSTEM,
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    companion object {
        fun fromStored(theme: String?, mode: String?) = AppearancePreferences(
            theme = AppTheme.entries.firstOrNull { it.name == theme } ?: AppTheme.MANGAKO,
            mode = ThemeMode.entries.firstOrNull { it.name == mode } ?: ThemeMode.SYSTEM,
        )
    }
}
