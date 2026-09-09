package com.gabedev.mangako.e2e

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.gabedev.mangako.core.unavailableMangaTitle
import com.gabedev.mangako.ui.screens.detail.MangaHeader
import com.gabedev.mangako.ui.theme.MangaKōTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class MangaHeaderLocalizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun japaneseTitleKeepsRomanizedSubtitle() {
        composeRule.setContent {
            MangaKōTheme {
                MangaHeader(
                    title = "ワンピース",
                    alternativeTitle = "Wan Pisu",
                    description = "",
                )
            }
        }

        composeRule.onNodeWithText("ワンピース").assertIsDisplayed()
        composeRule.onNodeWithText("Wan Pisu").assertIsDisplayed()
    }

    @Test
    fun unavailableJapaneseTitleKeepsRomanizedSubtitle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unavailable = context.unavailableMangaTitle(Locale.JAPAN)
        composeRule.setContent {
            MangaKōTheme {
                MangaHeader(
                    title = unavailable,
                    alternativeTitle = "Wan Pisu",
                    description = "",
                )
            }
        }

        assertEquals("タイトル情報なし", unavailable)
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
        composeRule.onNodeWithText("Wan Pisu").assertIsDisplayed()
    }

    @Test
    fun duplicateBlankAndNullSubtitlesAreHidden() {
        val alternative = mutableStateOf<String?>("  WAN PISU  ")
        composeRule.setContent {
            MangaKōTheme {
                MangaHeader(
                    title = "Wan Pisu",
                    alternativeTitle = alternative.value,
                    description = "",
                )
            }
        }

        composeRule.onNodeWithText("Wan Pisu").assertIsDisplayed()
        composeRule.onNodeWithText("  WAN PISU  ").assertDoesNotExist()
        composeRule.runOnIdle { alternative.value = "   " }
        composeRule.onNodeWithText("   ").assertDoesNotExist()
        composeRule.runOnIdle { alternative.value = null }
        composeRule.onNodeWithText("Wan Pisu").assertIsDisplayed()
    }

    @Test
    fun unavailableTitleUsesRequestedLocaleRatherThanContextLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Title unavailable", context.unavailableMangaTitle(Locale.US))
        assertEquals("Título indisponível", context.unavailableMangaTitle(Locale.forLanguageTag("pt-BR")))
        assertEquals("タイトル情報なし", context.unavailableMangaTitle(Locale.JAPAN))
    }
}
