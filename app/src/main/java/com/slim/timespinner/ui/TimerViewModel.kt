package com.slim.timespinner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.shawnlin.numberpicker.NumberPicker
import com.slim.timespinner.R
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.utils.CountDownTimer
import com.slim.timespinner.utils.SoundPlayer

private const val secondsInMilli = 1000                 //1 second = 1000 milliseconds
private const val minutesInMilli = secondsInMilli * 60  //1 minute = 60 seconds
private const val hoursInMilli = minutesInMilli * 60    //1 hour = 60 x 60 = 3600 seconds
private const val COUNT_DOWN_INTERVAL = 1000L           //1 second - count interval

class TimerViewModel(
        application: Application,
        private val soundPlayer: SoundPlayer,
        private val prefProvider: PrefProvider
) : AndroidViewModel(application) {

    private val timer: CountDownTimer by lazy { getCountDown() }
    private val toggleButtonObserver = Observer(::toggleTimer)

    val hoursPicker = MutableLiveData<Int>()
    val minutesPicker = MutableLiveData<Int>()
    val secondsPicker = MutableLiveData<Int>()
    val toggleButtonState = MutableLiveData(false)

    init {
        toggleButtonState.observeForever(toggleButtonObserver)
        setNumbersToPickers(prefProvider.lastTime)
    }

    private fun getCountDown() =
            CountDownTimer(COUNT_DOWN_INTERVAL, object : CountDownTimer.OnCountDownListener {

                override fun onTick(millisUntilFinished: Long) {
                    if (millisUntilFinished != 0L)
                        setNumbersToPickers(millisUntilFinished + COUNT_DOWN_INTERVAL)
                    else
                        setNumbersToPickers(millisUntilFinished)
                }

                override fun onFinish() {
                    toggleButtonState.value = false
                    soundPlayer.play()
                    setNumbersToPickers(prefProvider.lastTime)
                }
            })

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
            timer.start(time)
        } else {
            if (timer.isRunning)
                timer.cancel()
        }
    }

    fun onScrollStateChange(picker: NumberPicker?, oldValue: Int, newValue: Int) {
        when (picker?.id) {
            R.id.numberPicker_hours -> {
                updateTimer(oldValue, newValue, hoursInMilli)
                hoursPicker.value = newValue
            }
            R.id.numberPicker_minutes -> {
                updateTimer(oldValue, newValue, minutesInMilli)
                minutesPicker.value = newValue
            }
            R.id.numberPicker_seconds -> {
                updateTimer(oldValue, newValue, secondsInMilli)
                secondsPicker.value = newValue
            }
        }
    }

    private fun updateTimer(oldValue: Int, newValue: Int, updateTime: Int) {
        if (timer.isRunning) {
            timer.update(((newValue - oldValue) * updateTime).toLong())
        }
    }

    override fun onCleared() {
        toggleButtonState.removeObserver(toggleButtonObserver)
    }
}