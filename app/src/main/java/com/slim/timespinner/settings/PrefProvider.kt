package com.slim.timespinner.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREF_PACKAGE_NAME = "com.slim.timespinner.settings"
private const val PREF_KEY_LAST_TIME = "lastTime"

private const val DEFAULT_COUNT_DOWN_TIME = 30000L  // 30 seconds default value for timer

class PrefProvider(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_PACKAGE_NAME, Context.MODE_PRIVATE)

    var lastTime: Long
        get() = sharedPreferences.getLong(PREF_KEY_LAST_TIME, DEFAULT_COUNT_DOWN_TIME)
        set(value) = sharedPreferences.edit { putLong(PREF_KEY_LAST_TIME, value) }

}