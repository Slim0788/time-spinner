package com.slim.timespinner.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.slim.timespinner.R

class SoundPlayer(context: Context) {

    private val soundPool: SoundPool = createSoundPoolWithBuilder()
    private val soundId = soundPool.load(context, R.raw.timer_sound, 1)

    private fun createSoundPoolWithBuilder(): SoundPool {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(1)
            .build()
    }

    fun play() {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
    }

    fun stop() {
        soundPool.stop(soundId)
    }
}