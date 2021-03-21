package com.slim.timespinner.ui

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus)
            viewModel.showNotification()
        else
            viewModel.hideNotification()
    }

}