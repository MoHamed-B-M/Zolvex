package com.gov.employeedb.core

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the locale wrapper against the startup crash it once caused.
 *
 * `androidx.hilt.navigation.HiltViewModelFactory.create` unwraps the base chain of `LocalContext`
 * looking for a `ComponentActivity`, so `withLocale` has to hand Compose a `ContextWrapper` around
 * the caller (the Activity) rather than the bare `ContextImpl` that `createConfigurationContext`
 * produces. The localized resources have to survive the wrapping, because `getString`, `getText`
 * and Compose's `stringResource` all resolve through `getResources`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLocaleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun withLocale_keepsTheCallerInTheBaseChain() {
        val localized = context.withLocale(AppLanguage.AR)

        assertThat(localized).isInstanceOf(ContextWrapper::class.java)
        assertThat((localized as ContextWrapper).baseContext).isSameInstanceAs(context)
    }

    @Test
    fun withLocale_appliesTheRequestedLanguage() {
        val localized = context.withLocale(AppLanguage.AR)

        assertThat(localized.resources.configuration.locales[0].language).isEqualTo("ar")
    }
}
