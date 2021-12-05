package com.slim.timespinner.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.slim.timespinner.utils.*

private const val COUNT_DOWN_INTERVAL = 1000L   // 1 second - count interval

// https://android.googlesource.com/platform/packages/apps/DeskClock/
class TimerService : Service() {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val soundPlayer: SoundPlayer by lazy { createSoundPlayer() }

    private val binder: IBinder = TimerServiceBinder()

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationUtils.createNotificationManager(this)
    }
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationUtils.createNotification(this)
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    private val _countDownMillis = MutableLiveData<Long>()
    val countDownMillis: LiveData<Long> = _countDownMillis

    companion object {
        const val BROADCAST_ACTION = "BROADCAST_ACTION"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NOTIFICATION_START -> {
                startTimer((countDownMillis.value ?: 0) - 1000)
            }
            ACTION_NOTIFICATION_STOP -> {
                stopTimer()
                updateNotification(countDownMillis.value ?: 0)
            }
            ACTION_NOTIFICATION_RESET -> {
                stopTimer()
                stopForeground()
                onDestroy()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder = binder

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
        notificationBuilder.apply {
            NotificationUtils.setActionsToNotification(
                this@TimerService, this, timer.isRunning
            )
        }
            .build()
            .also {
                startForeground(NOTIFICATION_ID, it)
            }

        isForeground = true
    }

    fun stopForeground() {
        stopForeground(true)
        isForeground = false
    }

    private fun updateNotification(timeInMillis: Long) {
        if (isForeground) {
            notificationBuilder.apply {
                setContentText(TimeFormatter.getFormattedTime(timeInMillis))
                clearActions()
                NotificationUtils.setActionsToNotification(
                    this@TimerService, this, timer.isRunning
                )
            }.also {
                notificationManager.notify(NOTIFICATION_ID, it.build())
            }
        }
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