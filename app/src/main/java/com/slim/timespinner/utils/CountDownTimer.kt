package com.slim.timespinner.utils

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock

private const val MSG = 1

class CountDownTimer(
    private val countDownInterval: Long,
    private val onCountDownListener: OnCountDownListener
) {
    interface OnCountDownListener {

        /**
         * Callback fired on regular interval.
         *
         * @param millisUntilFinished The amount of time until finished.
         */
        fun onTick(millisUntilFinished: Long)

        /**
         * Callback fired when the time is up.
         */
        fun onFinish()
    }

    /**
     * The time in millis when timer will be cancelled
     */
    @Volatile
    private var stopTimeInFuture: Long = 0

    /**
     * boolean representing if the timer was cancelled
     */
    var isRunning = false
        private set

    /**
     * Cancel the countdown.
     */
    @Synchronized
    fun cancel() {
        isRunning = false
        handler.removeMessages(MSG)
    }

    /**
     * Start the countdown.
     */
    @Synchronized
    fun start(millisInFuture: Long) {
        isRunning = true
        if (millisInFuture <= 0) {
            onCountDownListener.onFinish()
        }
        stopTimeInFuture = SystemClock.elapsedRealtime() + millisInFuture
        handler.sendMessage(handler.obtainMessage(MSG))
    }

    /**
     * Update the countdown.
     */
    @Synchronized
    fun update(updateTime: Long) {
        stopTimeInFuture += updateTime
    }

    /**
     * Handles counting down
     */
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {

            synchronized(this@CountDownTimer) {

                if (!isRunning) return

                val millisLeft = stopTimeInFuture - SystemClock.elapsedRealtime()

                if (millisLeft <= 0) {
                    onCountDownListener.onFinish()
                } else {
                    val lastTickStart = SystemClock.elapsedRealtime()
                    onCountDownListener.onTick(millisLeft)

                    // take into account user's onTick taking time to execute
                    val lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart
                    var delay: Long

                    if (millisLeft < countDownInterval) {
                        // just delay until done
                        delay = millisLeft - lastTickDuration

                        // special case: user's onTick took more than interval to
                        // complete, trigger onFinish without delay
                        if (delay < 0) delay = 0
                    } else {
                        delay = countDownInterval - lastTickDuration

                        // special case: user's onTick took more than interval to
                        // complete, skip to next interval
                        while (delay < 0) delay += countDownInterval
                    }

                    sendMessageDelayed(obtainMessage(MSG), delay)
                }
            }
        }
    }
}