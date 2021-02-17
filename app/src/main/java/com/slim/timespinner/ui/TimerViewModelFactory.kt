package com.slim.timespinner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.utils.SoundPlayer

class TimerViewModelFactory(
    private val soundPlayer: SoundPlayer,
    private val prefProvider: PrefProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass == TimerViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            TimerViewModel(soundPlayer, prefProvider) as T
        } else {
            throw IllegalArgumentException()
        }
    }
}