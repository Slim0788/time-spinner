package com.slim.timespinner.service

import android.annotation.SuppressLint
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
import androidx.lifecycle.Observer
import com.slim.timespinner.utils.CountDownTimer
import com.slim.timespinner.utils.NotificationUtils
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_RESET
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_START
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_STOP
import com.slim.timespinner.utils.NotificationUtils.NOTIFICATION_ID
import com.slim.timespinner.utils.SoundPlayer
import com.slim.timespinner.utils.TimeFormatter

private const val COUNT_DOWN_INTERVAL = 1000L   // 1 second - count interval

// https://android.googlesource.com/platform/packages/apps/DeskClock/
class TimerService : Service() {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val soundPlayer: SoundPlayer by lazy { createSoundPlayer() }

    private val countDownMillisObserver = Observer<Long> {
        updateNotification(TimeFormatter.getFormattedTime(it))
    }

    private val binder: IBinder = TimerServiceBinder()

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationUtils.createNotificationManager(this)
    }
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationUtils.createNotification(this)
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "com.slim.timespinner.service:TimerService"
            )
            .apply {
                setReferenceCounted(false)
            }
    }
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
                updateNotification(
                    TimeFormatter.getFormattedTime(countDownMillis.value ?: 0)
                )
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
            }

            override fun onFinish() {
                soundPlayer.play()
                sendBroadcast(Intent(BROADCAST_ACTION))
                wakeLock.release()
            }
        })

    private fun createSoundPlayer() = SoundPlayer(applicationContext)

    fun startForeground() {
        if (!isTimerRunning) return

        notificationBuilder.apply {
            NotificationUtils.setActionsToNotification(
                this@TimerService, this, isTimerRunning
            )
        }
            .build()
            .also {
                startForeground(NOTIFICATION_ID, it)
            }
        countDownMillis.observeForever(countDownMillisObserver)
        isForeground = true
    }

    fun stopForeground() {
        stopForeground(true)
        countDownMillis.removeObserver(countDownMillisObserver)
        isForeground = false
    }

    private fun updateNotification(time: String) {
        if (isForeground) {
            notificationBuilder.apply {
                setContentText(time)
                clearActions()
                NotificationUtils.setActionsToNotification(
                    this@TimerService, this, isTimerRunning
                )
            }.also {
                notificationManager.notify(NOTIFICATION_ID, it.build())
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    fun startTimer(millisInFuture: Long) {
        if (!isTimerRunning) {
            timer.start(millisInFuture)
            wakeLock.acquire()
        }
    }

    fun updateTimer(updateTime: Long) {
        if (isTimerRunning) {
            timer.update(updateTime)
            wakeLock.release()
        }
    }

    fun stopTimer() {
        if (isTimerRunning)
            timer.cancel()
    }

    val isTimerRunning: Boolean
        get() = timer.isRunning


    inner class TimerServiceBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

}