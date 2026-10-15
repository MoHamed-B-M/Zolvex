package com.gov.employeedb.presentation.settings

import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.core.LocaleManager
import com.gov.employeedb.data.preferences.PreferencesRepository
import com.gov.employeedb.data.work.UpdateCheckScheduler
import com.gov.employeedb.data.work.UpdateNotificationHelper
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.domain.model.UpdateInfo
import com.gov.employeedb.domain.usecase.CheckForUpdateUseCase
import com.gov.employeedb.domain.usecase.ExportAnalyticsUseCase
import com.gov.employeedb.domain.usecase.ExportEmployeesUseCase
import com.gov.employeedb.testutil.FakeEmployeeRepository
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The update half of the settings screen (updateservice.md Sections 9 and 11): a manual check walks
 * from idle through checking to its result and notifies when an update exists, the auto-check toggle
 * schedules or cancels the daily work, and the pre-release toggle persists before it re-checks so the
 * checker reads the new flag rather than the old one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val exportEmployees = mockk<ExportEmployeesUseCase>(relaxed = true)
    private val exportAnalytics = mockk<ExportAnalyticsUseCase>(relaxed = true)
    private val checkForUpdate = mockk<CheckForUpdateUseCase>()
    private val updateCheckScheduler = mockk<UpdateCheckScheduler>(relaxed = true)
    private val updateNotificationHelper = mockk<UpdateNotificationHelper>(relaxed = true)
    private val localeManager = mockk<LocaleManager>(relaxed = true)

    private val availableInfo = UpdateInfo(
        tagName = "v9.9.9",
        name = "Version 9.9.9",
        body = "A newer release.",
        htmlUrl = "https://example.com/releases/v9.9.9",
    )

    @Before
    fun setUp() {
        // The screen observes these on construction, so they must emit before any event is sent.
        every { preferencesRepository.language } returns flowOf(AppLanguage.DEFAULT)
        every { preferencesRepository.themeMode } returns flowOf(AppThemeMode.DEFAULT)
        every { preferencesRepository.autoCheckEnabled } returns flowOf(true)
        every { preferencesRepository.includePrereleases } returns flowOf(false)
        every { preferencesRepository.lastUpdateCheckTime } returns flowOf(null)
    }

    private fun newViewModel(): SettingsViewModel = SettingsViewModel(
        preferencesRepository = preferencesRepository,
        employeeRepository = FakeEmployeeRepository(),
        exportEmployees = exportEmployees,
        exportAnalytics = exportAnalytics,
        checkForUpdate = checkForUpdate,
        updateCheckScheduler = updateCheckScheduler,
        updateNotificationHelper = updateNotificationHelper,
        localeManager = localeManager,
    )

    @Test
    fun `a manual check flows from idle through checking to the result`() = runTest {
        coEvery { checkForUpdate(force = true) } coAnswers {
            delay(CHECK_TAKES_MS)
            UpdateCheckResult.UpdateAvailable(availableInfo)
        }
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.updateState).isEqualTo(UpdateCheckResult.Idle)

        viewModel.onEvent(SettingsEvent.CheckForUpdates)
        runCurrent()
        assertThat(viewModel.uiState.value.updateState).isEqualTo(UpdateCheckResult.Checking)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.updateState)
            .isEqualTo(UpdateCheckResult.UpdateAvailable(availableInfo))
        verify(exactly = 1) { updateNotificationHelper.showUpdateNotification(availableInfo) }
    }

    @Test
    fun `an update that is not newer reports up to date without notifying`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.UpToDate
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.CheckForUpdates)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.updateState).isEqualTo(UpdateCheckResult.UpToDate)
        verify(exactly = 0) { updateNotificationHelper.showUpdateNotification(any()) }
    }

    @Test
    fun `disabling auto-check persists the flag and cancels the periodic work`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.SetAutoCheck(false))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.autoCheckEnabled).isFalse()
        coVerify(exactly = 1) { preferencesRepository.setAutoCheckEnabled(false) }
        verify(exactly = 1) { updateCheckScheduler.cancel() }
        verify(exactly = 0) { updateCheckScheduler.schedulePeriodic() }
    }

    @Test
    fun `enabling auto-check persists the flag and schedules the periodic work`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.SetAutoCheck(true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.autoCheckEnabled).isTrue()
        coVerify(exactly = 1) { preferencesRepository.setAutoCheckEnabled(true) }
        verify(exactly = 1) { updateCheckScheduler.schedulePeriodic() }
        verify(exactly = 0) { updateCheckScheduler.cancel() }
    }

    @Test
    fun `the pre-release toggle persists its flag before re-checking`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.UpToDate
        val viewModel = newViewModel()
        advanceUntilIdle()
        coVerify(exactly = 0) { checkForUpdate(force = true) }

        viewModel.onEvent(SettingsEvent.SetIncludePrereleases(true))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.includePrereleases).isTrue()
        // The write must land first: the checker reads this flag from DataStore to pick the
        // endpoint, so a re-check that raced ahead of the write would ask the wrong feed.
        coVerifyOrder {
            preferencesRepository.setIncludePrereleases(true)
            checkForUpdate(force = true)
        }
        assertThat(viewModel.uiState.value.updateState).isEqualTo(UpdateCheckResult.UpToDate)
    }

    @Test
    fun `a second check tap while one is in flight is ignored`() = runTest {
        coEvery { checkForUpdate(force = true) } coAnswers {
            delay(CHECK_TAKES_MS)
            UpdateCheckResult.UpToDate
        }
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.CheckForUpdates)
        runCurrent()
        viewModel.onEvent(SettingsEvent.CheckForUpdates)
        advanceUntilIdle()

        verify(exactly = 1) { checkForUpdate(force = true) }
        assertThat(viewModel.uiState.value.updateState).isEqualTo(UpdateCheckResult.UpToDate)
    }

    private companion object {
        /** Long enough to be observable on virtual time, short enough to keep the test instant. */
        const val CHECK_TAKES_MS = 500L
    }
}
