package com.gabedev.mangako.core

import android.content.Context
import android.content.res.Configuration
import com.gabedev.mangako.R
import java.util.Locale

fun Context.unavailableMangaTitle(locale: Locale): String {
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(configuration).getString(R.string.manga_title_unavailable)
}
