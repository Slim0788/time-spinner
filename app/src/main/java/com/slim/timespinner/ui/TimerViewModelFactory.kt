package com.slim.timespinner.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.slim.timespinner.settings.PrefProvider

class TimerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass == TimerViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            TimerViewModel(
                application,
                createPrefProvider()
            ) as T
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun createPrefProvider() = PrefProvider(application.applicationContext)

}