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
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.ui.ACTION_TIMER_FINISH
import com.slim.timespinner.utils.CountDownTimer
import com.slim.timespinner.utils.NotificationUtils
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_RESET
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_START
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_STOP
import com.slim.timespinner.utils.NotificationUtils.NOTIFICATION_ID
import com.slim.timespinner.utils.SoundPlayer
import com.slim.timespinner.utils.TimeFormatter

private const val COUNT_DOWN_INTERVAL = 1000L   // 1 second - count interval
private const val WAKE_LOCK_TAG = "com.slim.timespinner.service:TimerService"

class TimerService : Service() {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val soundPlayer: SoundPlayer by lazy { createSoundPlayer() }
    private val prefProvider: PrefProvider by lazy { PrefProvider(this) }

    private val binder: IBinder = TimerServiceBinder()

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationUtils.createNotificationManager(this)
    }
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationUtils.createNotification(this)
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply {
                setReferenceCounted(false)
            }
    }

    private val _countDownMillis = MutableLiveData<Long>()
    val countDownMillis: LiveData<Long> = _countDownMillis

    private val countDownMillisObserver = Observer<Long> {
        updateNotification(TimeFormatter.getFormattedTime(it))
    }

    private var isForeground = false

    val isTimerRunning: Boolean
        get() = timer.isRunning

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
                stopTimer()
                soundPlayer.play()
                sendBroadcast(Intent(ACTION_TIMER_FINISH))
                _countDownMillis.value = prefProvider.lastTime
                updateNotification(TimeFormatter.getFormattedTime(0))
                wakeLock.release()
            }
        })

    private fun createSoundPlayer() = SoundPlayer(applicationContext)

    fun startForeground() {
        if (!isTimerRunning || isForeground) return

        notificationBuilder.apply {
            clearActions()
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


    inner class TimerServiceBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

}