package com.slim.timespinner.ui

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.slim.timespinner.R
import com.slim.timespinner.databinding.ActivityTimerBinding

class TimerActivity : AppCompatActivity() {

    private val viewModel: TimerViewModel by viewModels {
        TimerViewModelFactory(application)
    }
    private lateinit var binding: ActivityTimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_timer)

        binding.apply {
            lifecycleOwner = this@TimerActivity
            viewmodel = viewModel
        }
        volumeControlStream = AudioManager.STREAM_ALARM

        binding.include?.apply {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.gilroy_bold)
            numberPickerHours.typeface = typeface
            numberPickerHours.setSelectedTypeface(typeface)
            numberPickerMinutes.typeface = typeface
            numberPickerMinutes.setSelectedTypeface(typeface)
            numberPickerSeconds.typeface = typeface
            numberPickerSeconds.setSelectedTypeface(typeface)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        viewModel.toggleNotification(!hasFocus)
    }

}