package com.gabedev.mangako.e2e

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.gabedev.mangako.MainActivity
import com.gabedev.mangako.ui.TestTags
import org.junit.Rule
import org.junit.Test

class NavigationReselectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun reselectingSearchScreensFocusesTheirSearchBars() {
        composeRule.onNodeWithTag(TestTags.LibraryNavigation).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.CollectionSearch).assertIsFocused()

        composeRule.onNodeWithTag(TestTags.ExploreNavigation).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(TestTags.ExploreSearch).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.ExploreNavigation).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.ExploreSearch).assertIsFocused()
    }
}
