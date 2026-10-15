package com.gov.employeedb.data.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.domain.model.UpdateInfo
import com.gov.employeedb.domain.usecase.CheckForUpdateUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The worker's result mapping (updateservice.md Section 6): a newer release posts the notification
 * and succeeds, a failed check asks WorkManager to retry, and an up-to-date app succeeds without
 * waking the notification shade.
 *
 * The worker is built through [TestListenableWorkerBuilder] with a factory that hands it its two
 * collaborators, because the default factory cannot satisfy an `@AssistedInject` constructor that
 * takes a use case. [UpdateCheckWorker.doWork] is public so the mapping can be read off the return
 * value directly instead of through a future.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateCheckWorkerTest {

    private val checkForUpdate: CheckForUpdateUseCase = mockk()
    private val notificationHelper: UpdateNotificationHelper = mockk(relaxed = true)

    private val update = UpdateInfo(
        tagName = "v2.0.0",
        name = "Version 2",
        body = "A newer release.",
        htmlUrl = "https://github.com/MoHamed-B-M/Zolvex/releases/tag/v2.0.0",
    )

    private fun newWorker(): UpdateCheckWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? = UpdateCheckWorker(
                appContext,
                workerParameters,
                checkForUpdate,
                notificationHelper,
            )
        }
        return TestListenableWorkerBuilder.from(context, UpdateCheckWorker::class.java)
            .setWorkerFactory(factory)
            .build()
    }

    @Test
    fun `a newer release notifies and succeeds`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.UpdateAvailable(update)

        val result = newWorker().doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        verify(exactly = 1) { notificationHelper.showUpdateNotification(update) }
    }

    @Test
    fun `a failed check asks for a retry and stays silent`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.Error("boom")

        val result = newWorker().doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        verify(exactly = 0) { notificationHelper.showUpdateNotification(any()) }
    }

    @Test
    fun `an up-to-date app succeeds without a notification`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.UpToDate

        val result = newWorker().doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        verify(exactly = 0) { notificationHelper.showUpdateNotification(any()) }
    }

    @Test
    fun `a result that cannot happen still succeeds`() = runTest {
        coEvery { checkForUpdate(force = true) } returns UpdateCheckResult.Idle

        val result = newWorker().doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }
}
