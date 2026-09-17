package com.gabedev.mangako.core

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import java.util.Locale

// Also respects the saved app language when a background worker starts without an Activity.
fun Context.appTextLocale(): Locale =
    LocaleManagerCompat.getApplicationLocales(this)[0]
        ?: LocaleManagerCompat.getSystemLocales(this)[0]
        ?: Locale.getDefault()
