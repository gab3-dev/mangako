package com.gabedev.mangako.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gabedev.mangako.MainActivity
import com.gabedev.mangako.R
import com.gabedev.mangako.core.Utils
import com.gabedev.mangako.domain.MangaNewVolumes

object NewVolumeNotificationHelper {
    private const val CHANNEL_ID = "new_volumes"

    fun notifyNewVolumes(context: Context, newVolumesByManga: List<MangaNewVolumes>) {
        if (newVolumesByManga.isEmpty() || !canPostNotifications(context)) return

        createNotificationChannel(context)
        newVolumesByManga.forEach { update ->
            NotificationManagerCompat.from(context).notify(
                update.manga.id.hashCode(),
                buildNotification(context, update),
            )
        }
    }

    private fun buildNotification(
        context: Context,
        update: MangaNewVolumes,
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(
            context.resources.getQuantityString(
                R.plurals.notification_new_volumes_title,
                update.volumes.size,
                update.volumes.size,
                update.manga.title,
            )
        )
        .setContentText(volumeListText(context, update))
        .setStyle(NotificationCompat.BigTextStyle().bigText(volumeListText(context, update)))
        .setContentIntent(appIntent(context))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    private fun volumeListText(context: Context, update: MangaNewVolumes): String {
        val volumes = update.volumes
            .sortedWith(compareBy(nullsLast()) { it.volume })
            .joinToString { volume ->
                context.getString(
                    R.string.notification_volume_label,
                    Utils.handleVolumeLabel(volume.volume, volume.locale, volume.isSpecialEdition),
                )
            }

        return volumes.ifBlank {
            context.getString(R.string.notification_new_volumes_unknown_volume)
        }
    }

    private fun appIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_new_volumes_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_new_volumes_channel_description)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
