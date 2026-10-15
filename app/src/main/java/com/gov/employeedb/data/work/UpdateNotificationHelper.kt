package com.gov.employeedb.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.UpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the update notification channel and posts the "new version" notification.
 *
 * The channel is created in [init], guarded for API 26+, so the first posted notification always
 * has a channel to attach to. On API 33+ the post is silently skipped when the user has not granted
 * `POST_NOTIFICATIONS` (the app never prompts; the system settings toggle is the only grant path).
 */
@Singleton
class UpdateNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_updates),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_updates_desc)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Posts the update notification, unless notifications are not permitted on API 33+. */
    fun showUpdateNotification(info: UpdateInfo) {
        if (!canPostNotifications()) return

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse(info.htmlUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_update)
            .setContentTitle(context.getString(R.string.notif_update_title, info.tagName))
            .setContentText(context.getString(R.string.notif_update_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(info.body.take(RELEASE_NOTES_PREVIEW_CHARS)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "zolvex_updates"
        const val NOTIFICATION_ID = 1001
        const val RELEASE_NOTES_PREVIEW_CHARS = 200
    }
}
