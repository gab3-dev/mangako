package com.gabedev.mangako.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.gabedev.mangako.MangaKoApplication
import com.gabedev.mangako.MainActivity
import com.gabedev.mangako.ui.TestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun search_add_to_library_and_mark_volume_owned() {
        composeRule.onNodeWithTag(TestTags.ExploreNavigation).performClick()
        composeRule.onNodeWithTag(TestTags.ExploreSearch).performTextInput("One Piece")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.mangaSearchResult("manga-1"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(TestTags.mangaSearchResult("manga-1")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(TestTags.AddToLibrary)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.AddToLibrary).performClick()
        composeRule.onNodeWithTag(TestTags.volumeCard("volume-1")).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val app = composeRule.activity.application as MangaKoApplication
            runBlocking {
                app.appContainer.localRepository.getMangaWithVolume("manga-1")
                    ?.volumes
                    ?.any { it.id == "volume-1" && it.owned }
                    ?: false
            }
        }

        val app = composeRule.activity.application as MangaKoApplication
        val persisted = runBlocking {
            app.appContainer.localRepository.getMangaWithVolume("manga-1")
        }
        assertTrue(persisted?.manga?.isOnUserLibrary == true)
        assertTrue(persisted?.volumes?.any { it.id == "volume-1" && it.owned } == true)

        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag(TestTags.LibraryNavigation).performClick()
        composeRule.onNodeWithTag(TestTags.mangaCard("manga-1")).assertIsDisplayed()
    }
}
