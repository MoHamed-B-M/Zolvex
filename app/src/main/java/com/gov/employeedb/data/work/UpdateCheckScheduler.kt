package com.gov.employeedb.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels the update check.
 *
 * The periodic request runs at most once a day, only with a network, and only when the battery is
 * not low (a background check must never be the reason the phone dies). It is unique work, so
 * toggling auto-check on and off never stacks duplicate checks.
 */
@Singleton
class UpdateCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Enqueues (or updates) the daily periodic check. */
    fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(PERIOD_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(INITIAL_DELAY_HOURS, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Cancels the periodic check when the user turns auto-check off. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UpdateCheckWorker.WORK_NAME)
    }

    /** Enqueues an immediate one-time check (manual "check now", network required). */
    fun checkNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private companion object {
        const val PERIOD_HOURS = 24L
        const val INITIAL_DELAY_HOURS = 1L
        const val BACKOFF_MINUTES = 15L
    }
}
