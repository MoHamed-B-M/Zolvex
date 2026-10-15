package com.gov.employeedb.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The update-check defaults of updateservice.md Section 3, read back through the real DataStore on
 * the JVM. The defaults are the user-visible contract of a fresh install: auto-check is on, GitHub
 * pre-releases are not offered, and nothing has been checked yet, so the boot-time check in Section 8
 * is free to run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PreferencesRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Start from an empty store so the first test reads the defaults rather than a previous
        // test's writes. The repository itself never clears the file.
        context.filesDir.resolve("datastore").deleteRecursively()
        repository = PreferencesRepository(context)
    }

    @Test
    fun `update preferences default to auto-check on, no pre-releases, never checked`() = runTest {
        assertThat(repository.autoCheckEnabled.first()).isTrue()
        assertThat(repository.includePrereleases.first()).isFalse()
        assertThat(repository.lastUpdateCheckTime.first()).isNull()
    }

    @Test
    fun `written update preferences round-trip through the store`() = runTest {
        repository.setAutoCheckEnabled(false)
        repository.setIncludePrereleases(true)
        repository.setLastUpdateCheckTime(42L)

        assertThat(repository.autoCheckEnabled.first()).isFalse()
        assertThat(repository.includePrereleases.first()).isTrue()
        assertThat(repository.lastUpdateCheckTime.first()).isEqualTo(42L)
    }
}
