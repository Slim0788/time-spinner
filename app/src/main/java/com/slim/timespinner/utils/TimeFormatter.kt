package com.slim.timespinner.utils

object TimeFormatter {
    const val secondsInMilli = 1000                 // 1 second = 1000 milliseconds
    const val minutesInMilli = secondsInMilli * 60  // 1 minute = 60 seconds
    const val hoursInMilli = minutesInMilli * 60    // 1 hour = 60 x 60 = 3600 seconds

    private fun numberFormat(number: Int) = number.toString().padStart(2, '0')

    fun getFormattedTime(timestamp: Long): String {
        var timeRest = timestamp.toInt()
        val hours = timeRest / hoursInMilli
        timeRest %= hoursInMilli
        val minutes = timeRest / minutesInMilli
        timeRest %= minutesInMilli
        val seconds = timeRest / secondsInMilli
        return when {
            minutes == 0 && hours == 0 -> numberFormat(seconds)
            hours == 0 -> "${numberFormat(minutes)}:${numberFormat(seconds)}"
            else -> "$hours:${numberFormat(minutes)}:${numberFormat(seconds)}"
        }
    }

}