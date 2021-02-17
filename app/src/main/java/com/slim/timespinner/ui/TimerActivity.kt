package com.slim.timespinner.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.slim.timespinner.R
import com.slim.timespinner.databinding.ActivityTimerBinding
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.utils.SoundPlayer

class TimerActivity : AppCompatActivity() {

    private val viewModel: TimerViewModel by viewModels {
        TimerViewModelFactory(
            SoundPlayer(application),
            PrefProvider(application)
        )
    }
    private lateinit var binding: ActivityTimerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_timer)

        binding.apply {
            lifecycleOwner = this@TimerActivity
            viewmodel = viewModel
        }
    }

}