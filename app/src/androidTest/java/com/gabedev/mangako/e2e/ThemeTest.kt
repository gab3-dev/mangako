package com.gabedev.mangako.e2e

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.gabedev.mangako.ui.theme.LocalAppDarkTheme
import com.gabedev.mangako.ui.theme.MangaKōTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun originalPaletteAndEffectiveModeRemainStable() {
        val dark = mutableStateOf(false)
        var primary = Color.Unspecified
        var background = Color.Unspecified
        var effectiveDark = false
        composeRule.setContent {
            MangaKōTheme(darkTheme = dark.value) {
                val scheme = MaterialTheme.colorScheme
                val mode = LocalAppDarkTheme.current
                SideEffect {
                    primary = scheme.primary
                    background = scheme.background
                    effectiveDark = mode
                }
            }
        }
        composeRule.runOnIdle {
            assertEquals(Color(0xFF8E4954), primary)
            assertEquals(Color(0xFFFFF8F7), background)
            assertEquals(false, effectiveDark)
            dark.value = true
        }
        composeRule.runOnIdle {
            assertEquals(Color(0xFFFFB2BB), primary)
            assertEquals(Color(0xFF151218), background)
            assertEquals(true, effectiveDark)
        }
    }

    @Test
    fun dynamicPaletteUsesSystemColorsOrOriginalFallback() {
        val dark = mutableStateOf(false)
        var actual = Color.Unspecified
        var expected = Color.Unspecified
        composeRule.setContent {
            val context = LocalContext.current
            val expectedPrimary = if (Build.VERSION.SDK_INT >= 31) {
                if (dark.value) dynamicDarkColorScheme(context).primary
                else dynamicLightColorScheme(context).primary
            } else {
                if (dark.value) Color(0xFFFFB2BB) else Color(0xFF8E4954)
            }
            MangaKōTheme(darkTheme = dark.value, dynamicColor = true) {
                val primary = MaterialTheme.colorScheme.primary
                SideEffect {
                    actual = primary
                    expected = expectedPrimary
                }
            }
        }
        composeRule.runOnIdle {
            assertEquals(expected, actual)
            dark.value = true
        }
        composeRule.runOnIdle { assertEquals(expected, actual) }
    }
}
