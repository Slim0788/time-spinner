package com.slim.timespinner.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.slim.timespinner.R
import com.slim.timespinner.ui.TimerActivity
import com.slim.timespinner.utils.CountDownTimer
import com.slim.timespinner.utils.SoundPlayer
import java.util.*

private const val COUNT_DOWN_INTERVAL = 1000L   //1 second - count interval
private const val CHANNEL_ID = "Random number notification"

class TimerService : Service() {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val soundPlayer: SoundPlayer by lazy { createSoundPlayer() }

    private val binder: IBinder = TimerServiceBinder()

    private var wakeLock: PowerManager.WakeLock? = null

    private val _countDownMillis = MutableLiveData<Long>()
    val countDownMillis: LiveData<Long> = _countDownMillis

    companion object {
        const val BROADCAST_ACTION = "BROADCAST_ACTION"
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun getCountDown() =
        CountDownTimer(COUNT_DOWN_INTERVAL, object : CountDownTimer.OnCountDownListener {

            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished != 0L)
                    _countDownMillis.value = millisUntilFinished + COUNT_DOWN_INTERVAL
                else
                    _countDownMillis.value = millisUntilFinished
            }

            override fun onFinish() {
                soundPlayer.play()
                sendBroadcast(Intent(BROADCAST_ACTION))
                wakeLock?.release()
            }
        })


    private fun createSoundPlayer() = SoundPlayer(applicationContext)

    fun startForeground() {
        if (!timer.isRunning) {
            return
        }

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(1, "com.slim.timespinner.service:TimerService")
        wakeLock?.setReferenceCounted(false)

        wakeLock?.acquire(10)


        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
//            .addAction(R.drawable.ic_notification, "STOP", getPendingIntent())
//            .addAction(R.drawable.ic_notification, "RESET", getPendingIntent())
            .setAutoCancel(true)
            .build()
        startForeground(1, notification)
    }

    private fun getPendingIntent(): PendingIntent {

        val intent = Intent(this, TimerActivity::class.java)
        intent.apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return pendingIntent

//        val pendingShowApp: PendingIntent =
//            PendingIntent.getService(this, REQUEST_CODE_UPCOMING, showApp,
//                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT)
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


    inner class TimerServiceBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

}