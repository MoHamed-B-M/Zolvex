package com.gov.employeedb.core

import android.app.LocaleManager as SystemLocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns a [Context] whose resources resolve in [language].
 *
 * Only the locale and the layout direction are overridden; everything else (uiMode, density,
 * screen size, night mode) is inherited from the current configuration. The result is what the
 * Compose tree publishes as `LocalContext`/`LocalConfiguration`, which is how a language change
 * re-renders the UI without recreating the Activity (see PLAN.md ADR-9).
 */
fun Context.withLocale(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(language.locale))
    configuration.setLayoutDirection(language.locale)
    return createConfigurationContext(configuration)
}

/**
 * Holds the app language in a lock-free cache and mirrors the Android 13+ per-app language
 * setting into the app's own preference store.
 *
 * The cached value is read synchronously, before DataStore's first emission lands, so the very
 * first frame of a cold start is already drawn in the persisted language instead of flashing
 * the system locale.
 */
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * The most recently persisted language. Seeded from the system locale so a first launch (no
     * stored value yet) starts in the user's own language.
     */
    @Volatile
    var cachedLanguage: AppLanguage = AppLanguage.fromTag(Locale.getDefault().language)
        private set

    /** Called whenever the persisted language is (re)read from the preference store. */
    fun cache(language: AppLanguage) {
        cachedLanguage = language
    }

    /**
     * The language chosen on the system "App languages" screen.
     *
     * Returns `null` on API < 33 and when the user has never used that screen, in which case the
     * app's own stored preference remains authoritative.
     */
    fun systemLanguage(): AppLanguage? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val localeManager = context.getSystemService(SystemLocaleManager::class.java) ?: return null
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) return null
        return AppLanguage.fromTag(locales[0].language)
    }
}
