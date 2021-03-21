package com.slim.timespinner.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.shawnlin.numberpicker.NumberPicker
import com.slim.timespinner.R
import com.slim.timespinner.service.TimerService
import com.slim.timespinner.service.TimerService.TimerServiceBinder
import com.slim.timespinner.settings.PrefProvider

private const val secondsInMilli = 1000                 //1 second = 1000 milliseconds
private const val minutesInMilli = secondsInMilli * 60  //1 minute = 60 seconds
private const val hoursInMilli = minutesInMilli * 60    //1 hour = 60 x 60 = 3600 seconds

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
    private var bound = false
    private val bindServiceIntent = Intent(context, TimerService::class.java)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // TODO verify intent
            setNumbersToPickers(prefProvider.lastTime)
            toggleButtonState.value = false
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            val binder = iBinder as TimerServiceBinder
            service = binder.service
            bound = true
            service?.countDownMillis?.observeForever(countDownMillisObserver)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    init {
        toggleButtonState.observeForever(toggleButtonObserver)
        setNumbersToPickers(prefProvider.lastTime)
        bindService()
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
        if (isChecked) {
            val time = getMillisFromPickers()
            prefProvider.lastTime = time
            service?.startTimer(time)
        } else {
            service?.stopTimer()
        }
    }

    private fun bindService() {
        getApplication<Application>().apply {
            bindService(
                bindServiceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            val intFilter = IntentFilter(TimerService.BROADCAST_ACTION)
            registerReceiver(broadcastReceiver, intFilter)
        }
    }

    fun showNotification() {
//        service?.startForeground()
    }

    fun hideNotification() {
//        Toast.makeText(getApplication(), "Hide notification", Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        toggleButtonState.removeObserver(toggleButtonObserver)
        service?.countDownMillis?.removeObserver(countDownMillisObserver)
        service = null
        getApplication<Application>().unregisterReceiver(broadcastReceiver)
    }

}