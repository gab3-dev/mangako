package com.gabedev.mangako

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class MangaKoE2ETestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        classLoader: ClassLoader,
        className: String,
        context: Context,
    ): Application {
        return super.newApplication(classLoader, E2ETestApplication::class.java.name, context)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        (targetContext.applicationContext as? E2ETestApplication)?.closeE2EResources()
        super.finish(resultCode, results)
    }
}
