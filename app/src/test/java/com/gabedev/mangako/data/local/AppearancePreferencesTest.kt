package com.gabedev.mangako.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun `missing preferences preserve original theme and system mode`() {
        assertEquals(AppearancePreferences(), AppearancePreferences.fromStored(null, null))
    }

    @Test
    fun `unknown preferences fall back independently`() {
        assertEquals(
            AppearancePreferences(AppTheme.MANGAKO, ThemeMode.DARK),
            AppearancePreferences.fromStored("unknown", "DARK"),
        )
        assertEquals(
            AppearancePreferences(AppTheme.DYNAMIC, ThemeMode.SYSTEM),
            AppearancePreferences.fromStored("DYNAMIC", "unknown"),
        )
    }

    @Test
    fun `all persisted choices round trip`() {
        AppTheme.entries.forEach { theme ->
            ThemeMode.entries.forEach { mode ->
                assertEquals(
                    AppearancePreferences(theme, mode),
                    AppearancePreferences.fromStored(theme.name, mode.name),
                )
            }
        }
    }

    @Test
    fun `system mode follows system brightness`() {
        assertFalse(AppearancePreferences().isDark(false))
        assertTrue(AppearancePreferences().isDark(true))
    }

    @Test
    fun `explicit mode overrides system brightness`() {
        listOf(false, true).forEach { systemDark ->
            assertFalse(AppearancePreferences(mode = ThemeMode.LIGHT).isDark(systemDark))
            assertTrue(AppearancePreferences(mode = ThemeMode.DARK).isDark(systemDark))
        }
    }
}
