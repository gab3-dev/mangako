package com.gabedev.mangako.e2e

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.gabedev.mangako.MainActivity
import com.gabedev.mangako.MangaKoApplication
import com.gabedev.mangako.core.appTextLocale
import com.gabedev.mangako.data.local.getAppearancePreferences
import com.gabedev.mangako.data.local.CoverLanguage
import com.gabedev.mangako.data.local.getCoverLanguage
import com.gabedev.mangako.data.local.saveCoverLanguage
import com.gabedev.mangako.ui.TestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LanguageSettingsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var originalLocales: LocaleListCompat
    private lateinit var originalCoverLanguage: CoverLanguage

    @Before
    fun setUp() {
        originalCoverLanguage = runBlocking { composeRule.activity.getCoverLanguage().first() }
        composeRule.runOnIdle {
            originalLocales = AppCompatDelegate.getApplicationLocales()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        waitForLanguage("en")
    }

    @After
    fun restoreLanguage() {
        if (::originalCoverLanguage.isInitialized) {
            runBlocking { composeRule.activity.saveCoverLanguage(originalCoverLanguage) }
        }
        if (::originalLocales.isInitialized) {
            composeRule.runOnIdle { AppCompatDelegate.setApplicationLocales(originalLocales) }
            waitForLanguage(originalLocales.toLanguageTags())
        }
    }

    private fun waitForLanguage(tag: String) {
        composeRule.waitUntil(5_000) {
            LocaleManagerCompat.getApplicationLocales(composeRule.activity.applicationContext)
                .toLanguageTags() == tag
        }
        composeRule.waitForIdle()
    }

    @Test
    fun coverLanguageIsIndependentAndSurvivesRecreation() {
        val context = composeRule.activity.applicationContext
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(TestTags.SettingsNavigation).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.SettingsNavigation).performClick()
        composeRule.onNodeWithTag("cover-language-picker").performScrollTo().performClick()
        composeRule.onNodeWithTag("cover-language-fr").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { context.getCoverLanguage().first() == CoverLanguage.FRENCH }
        }
        assertEquals("en", context.appTextLocale().language)
        composeRule.onNodeWithTag("app-language-ja").performScrollTo().performClick()
        waitForLanguage("ja")
        assertEquals(CoverLanguage.FRENCH, runBlocking { context.getCoverLanguage().first() })
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("cover-language-picker").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(CoverLanguage.FRENCH, runBlocking { context.getCoverLanguage().first() })
        assertEquals("ja", context.appTextLocale().language)
    }

    @Test
    fun languageUpdatesInterfaceAndCatalogWithoutChangingAppearanceOrVolumeLanguage() {
        val context = composeRule.activity.applicationContext
        val appearance = runBlocking { context.getAppearancePreferences().first() }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(TestTags.SettingsNavigation).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.SettingsNavigation).performClick()
        composeRule.onNodeWithTag("app-language-pt-BR").performScrollTo().performClick()
        waitForLanguage("pt-BR")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Idioma do app").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app-language-pt-BR").assertIsSelected()
        assertEquals("pt-BR", context.appTextLocale().toLanguageTag())
        val repository = (context as MangaKoApplication).appContainer.mangaRepository
        val portuguese = runBlocking { repository.getManga("manga-1") }
        assertEquals("One Piece PT", portuguese.title)
        assertEquals("Aventura pirata", portuguese.description)

        composeRule.onNodeWithTag("app-language-ja").performScrollTo().performClick()
        waitForLanguage("ja")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("アプリの言語").fetchSemanticsNodes().isNotEmpty()
        }
        val japanese = runBlocking { repository.getManga("manga-1") }
        assertEquals("ワンピース", japanese.title)
        assertEquals("海賊の冒険", japanese.description)
        assertEquals(listOf("ja", "ja"), runBlocking {
            repository.getCoverListByManga(japanese).map { it.locale }
        })

        composeRule.activityRule.scenario.recreate()
        waitForLanguage("ja")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("app-language-ja").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app-language-ja").assertIsSelected()
        assertEquals(appearance, runBlocking { context.getAppearancePreferences().first() })

        composeRule.onNodeWithTag("app-language-system").performScrollTo().performClick()
        waitForLanguage("")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("app-language-system").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("app-language-system").assertIsSelected()
        assertEquals(LocaleManagerCompat.getSystemLocales(context)[0], context.appTextLocale())
    }
}
