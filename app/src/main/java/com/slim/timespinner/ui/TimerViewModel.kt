package com.slim.timespinner.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.slim.timespinner.R
import com.slim.timespinner.service.TimerService
import com.slim.timespinner.service.TimerService.TimerServiceBinder
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.ui.picker.NumberPicker
import com.slim.timespinner.utils.TimeFormatter.hoursInMilli
import com.slim.timespinner.utils.TimeFormatter.minutesInMilli
import com.slim.timespinner.utils.TimeFormatter.secondsInMilli

const val ACTION_TIMER_FINISH = "ACTION_TIMER_FINISH"
const val ACTION_TIMER_CONTROL = "ACTION_TIMER_CONTROL"

const val EXTRA_CONTROL_TYPE = "EXTRA_CONTROL_TYPE"
const val CONTROL_TYPE_START = 23
const val CONTROL_TYPE_STOP = 21

const val REQUEST_CODE_CONTROL_START = 78
const val REQUEST_CODE_CONTROL_STOP = 54

class TimerViewModel(
    application: Application,
    private val prefProvider: PrefProvider
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val toggleButtonObserver = Observer(::toggleTimer)
    private val countDownMillisObserver = Observer(::setNumbersToPickers)

    val hoursPicker = MutableLiveData<Int>()
    val minutesPicker = MutableLiveData<Int>()
    val secondsPicker = MutableLiveData<Int>()
    val toggleButtonState = MutableLiveData(false)

    @SuppressLint("StaticFieldLeak")
    private var service: TimerService? = null
    private var isServiceBind = false
    private val serviceIntent = Intent(context, TimerService::class.java)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                ACTION_TIMER_FINISH -> {
                    setNumbersToPickers(prefProvider.lastTime)
                    toggleButtonState.value = false
                }
                ACTION_TIMER_CONTROL -> {
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_TYPE_START -> toggleButtonState.value = true
                        CONTROL_TYPE_STOP -> toggleButtonState.value = false
                    }
                    toggleNotification(true)
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            val binder = iBinder as TimerServiceBinder
            service = binder.service.also {
                it.countDownMillis.observeForever(countDownMillisObserver)
            }
            isServiceBind = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isServiceBind = false
        }
    }

    init {
        toggleButtonState.observeForever(toggleButtonObserver)
        setNumbersToPickers(prefProvider.lastTime)
        bindService()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_TIMER_FINISH)
            addAction(ACTION_TIMER_CONTROL)
        }
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    fun onScrollStateChange(picker: NumberPicker?, oldValue: Int, newValue: Int) {
        when (picker?.id) {
            R.id.numberPicker_hours -> {
                updateTimer(oldValue, newValue, hoursInMilli)
            }
            R.id.numberPicker_minutes -> {
                updateTimer(oldValue, newValue, minutesInMilli)
            }
            R.id.numberPicker_seconds -> {
                updateTimer(oldValue, newValue, secondsInMilli)
            }
        }
    }

    private fun updateTimer(oldValue: Int, newValue: Int, updateTime: Int) {
        service?.updateTimer(((newValue - oldValue) * updateTime).toLong())
    }

    private fun getMillisFromPickers() =
        (hoursPicker.value!! * hoursInMilli +
                minutesPicker.value!! * minutesInMilli +
                secondsPicker.value!! * secondsInMilli).toLong()

    private fun setNumbersToPickers(timestamp: Long) {
        var timeRest = timestamp.toInt()
        hoursPicker.value = timeRest / hoursInMilli
        timeRest %= hoursInMilli
        minutesPicker.value = timeRest / minutesInMilli
        timeRest %= minutesInMilli
        secondsPicker.value = timeRest / secondsInMilli
    }

    private fun toggleTimer(isChecked: Boolean) {
        service?.let {
            if (isChecked) {
                val time = getMillisFromPickers()
                prefProvider.lastTime = time
                it.startTimer(time)
            } else {
                it.stopTimer()
            }
        } ?: bindService()
    }

    private fun bindService() {
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (!isServiceBind) return
        service?.countDownMillis?.removeObserver(countDownMillisObserver)
        context.unbindService(serviceConnection)
        isServiceBind = false
    }

    fun toggleNotification(isShow: Boolean) {
        service?.let {
            if (isShow) it.startForeground() else it.stopForeground()
        }
    }

    fun checkButtonState() {
        service?.let {
            toggleButtonState.value = it.isTimerRunning
        }
    }

    override fun onCleared() {
        toggleButtonState.removeObserver(toggleButtonObserver)
        context.unregisterReceiver(broadcastReceiver)
        unbindService()
    }

}