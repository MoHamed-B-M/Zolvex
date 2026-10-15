package com.gov.employeedb.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.domain.usecase.CheckForUpdateUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that checks GitHub Releases and notifies on a newer version.
 *
 * The 24-hour cooldown is intentionally bypassed here (`force = true`): the periodic work request
 * itself only runs once per day, so a second cooldown on top would starve the notification on
 * days the app was not opened.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val checkForUpdate: CheckForUpdateUseCase,
    private val notificationHelper: UpdateNotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (val result = checkForUpdate(force = true)) {
        is UpdateCheckResult.UpdateAvailable -> {
            notificationHelper.showUpdateNotification(result.info)
            Result.success()
        }
        is UpdateCheckResult.Error -> Result.retry()
        is UpdateCheckResult.UpToDate -> Result.success()
        UpdateCheckResult.Idle, UpdateCheckResult.Checking -> Result.success()
    }

    companion object {
        /** Stable unique-work name; the scheduler keys both the periodic and manual work on it. */
        const val WORK_NAME = "zolvex_update_check"
    }
}
