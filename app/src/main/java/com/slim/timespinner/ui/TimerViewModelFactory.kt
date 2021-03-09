package com.slim.timespinner.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.slim.timespinner.settings.PrefProvider
import com.slim.timespinner.utils.SoundPlayer

class TimerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass == TimerViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            TimerViewModel(
                createSoundPlayer(),
                createPrefProvider()
            ) as T
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun createSoundPlayer() = SoundPlayer(context)

    private fun createPrefProvider() = PrefProvider(context)

}