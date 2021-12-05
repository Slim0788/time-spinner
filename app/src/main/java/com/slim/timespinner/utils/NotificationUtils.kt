package com.slim.timespinner.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.slim.timespinner.R
import com.slim.timespinner.service.TimerService
import com.slim.timespinner.ui.TimerActivity

object NotificationUtils {

    private const val CHANNEL_ID = "Time Spinner channel"
    private const val CHANNEL_NAME = "Time Spinner"

    const val NOTIFICATION_ID = 2186

    const val ACTION_NOTIFICATION_START = "com.slim.timespinner.service:Start"
    const val ACTION_NOTIFICATION_STOP = "com.slim.timespinner.service:Stop"
    const val ACTION_NOTIFICATION_RESET = "com.slim.timespinner.service:Reset"

    fun createNotificationManager(context: Context) = NotificationManagerCompat.from(context)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                createNotificationChannel(channel)
            }
        }

    fun createNotification(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_text_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setShowWhen(false)
            .setOngoing(true)
            .setLocalOnly(true)
            .setAutoCancel(true)
            .setColorized(true)
            .setSilent(true)
            .setContentIntent(getPendingIntent(context))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
    }

    fun setActionsToNotification(
        context: Context,
        notificationBuilder: NotificationCompat.Builder,
        isTimerRunning: Boolean
    ): NotificationCompat.Builder {
        return notificationBuilder.apply {
            if (isTimerRunning) {
                setContentTitle(context.getString(R.string.notification_text_running))
                addAction(
                    R.drawable.ic_notification_stop,
                    context.getString(R.string.button_stop),
                    getPendingIntentStopTimer(context)
                )
            } else {
                setContentTitle(context.getString(R.string.notification_text_stopped))
                addAction(
                    R.drawable.ic_notification_start,
                    context.getString(R.string.button_start),
                    getPendingIntentStartTimer(context)
                )
            }
            addAction(
                R.drawable.ic_notification_reset,
                context.getString(R.string.button_reset),
                getPendingIntentResetTimer(context)
            )
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TimerActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingIntentStartTimer(context: Context): PendingIntent {
        val intent = Intent(context, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_START
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingIntentStopTimer(context: Context): PendingIntent {
        val intent = Intent(context, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_STOP
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingIntentResetTimer(context: Context): PendingIntent {
        val intent = Intent(context, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_RESET
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}