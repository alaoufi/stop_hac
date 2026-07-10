package com.privacyshield.monitor.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.privacyshield.monitor.data.prefs.AppLanguage

/**
 * Applies the user's language choice using the AndroidX per-app language APIs,
 * so switching between Arabic and English takes effect immediately and persists
 * across restarts (backed by the framework on Android 13+, AppCompat below).
 */
object LocaleController {
    fun apply(language: AppLanguage) {
        val locales = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
