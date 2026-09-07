package com.gabedev.mangako.e2e

import android.os.Build
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.gabedev.mangako.MainActivity
import com.gabedev.mangako.R
import com.gabedev.mangako.data.local.AppTheme
import com.gabedev.mangako.data.local.AppearancePreferences
import com.gabedev.mangako.data.local.ThemeMode
import com.gabedev.mangako.data.local.getAppearancePreferences
import com.gabedev.mangako.data.local.saveAppTheme
import com.gabedev.mangako.data.local.saveThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppearanceJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppearancePreferences

    @Before
    fun savePreferences() = runBlocking {
        original = composeRule.activity.getAppearancePreferences().first()
        composeRule.activity.saveAppTheme(AppTheme.MANGAKO)
        composeRule.activity.saveThemeMode(ThemeMode.SYSTEM)
    }

    @After
    fun restorePreferences() = runBlocking {
        composeRule.activity.saveAppTheme(original.theme)
        composeRule.activity.saveThemeMode(original.mode)
    }

    @Test
    fun selectionPersistsAcrossActivityRecreation() {
        val settings = composeRule.activity.getString(R.string.nav_settings)
        val dark = composeRule.activity.getString(R.string.appearance_mode_dark)
        val dynamic = composeRule.activity.getString(R.string.appearance_theme_dynamic)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(settings).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(settings).performClick()
        composeRule.onNodeWithText(dark).performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { composeRule.activity.getAppearancePreferences().first().mode == ThemeMode.DARK }
        }
        composeRule.onNodeWithText(dark).assertIsSelected()

        val expectedTheme = if (Build.VERSION.SDK_INT >= 31) {
            composeRule.onNodeWithText(dynamic).performScrollTo().performClick()
            AppTheme.DYNAMIC
        } else {
            composeRule.onNodeWithText(dynamic).performScrollTo().assertIsNotEnabled()
            AppTheme.MANGAKO
        }
        composeRule.waitUntil(5_000) {
            runBlocking { composeRule.activity.getAppearancePreferences().first().theme == expectedTheme }
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(dark).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(
            AppearancePreferences(expectedTheme, ThemeMode.DARK),
            runBlocking { composeRule.activity.getAppearancePreferences().first() },
        )
        // The settings destination must survive both theme changes and recreation.
        composeRule.onNodeWithText(dark).performScrollTo().assertIsSelected()
    }
}
