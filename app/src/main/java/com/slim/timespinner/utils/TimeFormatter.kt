package com.slim.timespinner.utils

import java.text.NumberFormat

object TimeFormatter {
    const val secondsInMilli = 1000                 // 1 second = 1000 milliseconds
    const val minutesInMilli = secondsInMilli * 60  // 1 minute = 60 seconds
    const val hoursInMilli = minutesInMilli * 60    // 1 hour = 60 x 60 = 3600 seconds

    private val numberFormat = NumberFormat.getInstance().apply {
        minimumIntegerDigits = 2
    }

    fun getFormattedTime(timestamp: Long): String {
        var timeRest = timestamp.toInt()
        val hours = timeRest / hoursInMilli
        timeRest %= hoursInMilli
        val minutes = timeRest / minutesInMilli
        timeRest %= minutesInMilli
        val seconds = timeRest / secondsInMilli
        return when {
            minutes == 0 && hours == 0 -> numberFormat.format(seconds)
            hours == 0 -> "${numberFormat.format(minutes)}:${numberFormat.format(seconds)}"
            else -> "$hours:${numberFormat.format(minutes)}:${numberFormat.format(seconds)}"
        }
    }

}