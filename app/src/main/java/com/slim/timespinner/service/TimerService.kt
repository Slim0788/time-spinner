package com.slim.timespinner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.slim.timespinner.R
import com.slim.timespinner.ui.TimerActivity
import com.slim.timespinner.utils.CountDownTimer
import com.slim.timespinner.utils.SoundPlayer
import com.slim.timespinner.utils.TimeFormatter

private const val COUNT_DOWN_INTERVAL = 1000L   // 1 second - count interval
private const val NOTIFICATION_ID = 2186
private const val CHANNEL_ID = "Time Spinner channel"
private const val CHANNEL_NAME = "Time Spinner"

private const val ACTION_NOTIFICATION_START = "com.slim.timespinner.service:Start"
private const val ACTION_NOTIFICATION_STOP = "com.slim.timespinner.service:Stop"
private const val ACTION_NOTIFICATION_RESET = "com.slim.timespinner.service:Reset"

// https://android.googlesource.com/platform/packages/apps/DeskClock/
class TimerService : Service() {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val soundPlayer: SoundPlayer by lazy { createSoundPlayer() }

    private val binder: IBinder = TimerServiceBinder()

    private lateinit var notificationManager: NotificationManagerCompat

    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    private val _countDownMillis = MutableLiveData<Long>()
    val countDownMillis: LiveData<Long> = _countDownMillis

    companion object {
        const val BROADCAST_ACTION = "BROADCAST_ACTION"
    }

    override fun onCreate() {
        notificationManager = NotificationManagerCompat.from(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NOTIFICATION_START -> {
                startTimer(5000L)
            }
            ACTION_NOTIFICATION_STOP -> {
                stopTimer()
            }
            ACTION_NOTIFICATION_RESET -> {
                stopTimer()
                stopForeground()
                onDestroy()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun getCountDown() =
        CountDownTimer(COUNT_DOWN_INTERVAL, object : CountDownTimer.OnCountDownListener {

            override fun onTick(millisUntilFinished: Long) {
                val millisLeft = if (millisUntilFinished != 0L) {
                    millisUntilFinished + COUNT_DOWN_INTERVAL
                } else {
                    millisUntilFinished
                }
                _countDownMillis.value = millisLeft
                updateNotification(millisLeft)
            }

            override fun onFinish() {
                soundPlayer.play()
                sendBroadcast(Intent(BROADCAST_ACTION))
                wakeLock?.release()
            }
        })

    private fun createSoundPlayer() = SoundPlayer(applicationContext)

    fun startForeground() {
        if (!timer.isRunning) return

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(1, "com.slim.timespinner.service:TimerService")
        wakeLock?.apply {
            setReferenceCounted(false)
            acquire(10)
        }
        startForeground(NOTIFICATION_ID, createNotification().build())
        isForeground = true
    }

    fun stopForeground() {
        stopForeground(true)
        isForeground = false
    }

    private fun updateNotification(timeInMillis: Long) {
        if (isForeground) {
            val notification = createNotification()
            notification.setContentText(TimeFormatter.getFormattedTime(timeInMillis))
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_text_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setShowWhen(false)
            .setOngoing(true)
            .setLocalOnly(true)
            .setAutoCancel(true)
            .setColorized(true)
            .setSilent(true)
            .setContentIntent(getPendingIntent())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
            .addAction(
                R.drawable.ic_notification_start,
                applicationContext.getString(R.string.button_start),
                getPendingIntentStartTimer()
            )
            .addAction(
                R.drawable.ic_notification_stop,
                applicationContext.getString(R.string.button_stop),
                getPendingIntentStopTimer()
            )
            .addAction(
                R.drawable.ic_notification_reset,
                applicationContext.getString(R.string.button_reset),
                getPendingIntentResetTimer()
            )
//            .build()
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, TimerActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingIntentStartTimer(): PendingIntent {
        val intent = Intent(this, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_START
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getPendingIntentStopTimer(): PendingIntent {
        val intent = Intent(this, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_STOP
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getPendingIntentResetTimer(): PendingIntent {
        val intent = Intent(this, TimerService::class.java)
        intent.action = ACTION_NOTIFICATION_RESET
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun startTimer(millisInFuture: Long) {
        if (!timer.isRunning)
            timer.start(millisInFuture)
    }

    fun updateTimer(updateTime: Long) {
        if (timer.isRunning)
            timer.update(updateTime)
    }

    fun stopTimer() {
        if (timer.isRunning)
            timer.cancel()
    }

    fun isTimerRunning() = timer.isRunning


    inner class TimerServiceBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

}