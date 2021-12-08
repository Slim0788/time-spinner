package com.slim.timespinner.ui

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.slim.timespinner.R
import com.slim.timespinner.databinding.ActivityTimerBinding
import com.slim.timespinner.utils.NotificationUtils.ACTION_NOTIFICATION_OPEN_APP

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

        binding.include.apply {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.gilroy_bold)
            numberPickerHours.typeface = typeface
            numberPickerHours.setSelectedTypeface(typeface)
            numberPickerMinutes.typeface = typeface
            numberPickerMinutes.setSelectedTypeface(typeface)
            numberPickerSeconds.typeface = typeface
            numberPickerSeconds.setSelectedTypeface(typeface)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.minimize.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    enterPictureInPictureMode(
                        updatePictureInPictureParams(viewModel.toggleButtonState.value == true)
                    )
                }
            }
        }

        viewModel.toggleButtonState.observe(this) {
            updatePictureInPictureParams(it)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (it.action == ACTION_NOTIFICATION_OPEN_APP && isInPictureInPictureMode) {
                    moveLauncherTaskToFront()
                }
            }
        }
    }

    private fun moveLauncherTaskToFront() {
        val activityManager = (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        val appTasks = activityManager.appTasks
        for (task in appTasks) {
            val baseIntent = task.taskInfo.baseIntent
            val categories = baseIntent.categories
            if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                task.moveToFront()
                return
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (isInPictureInPictureMode) {
            binding.minimize.visibility = View.GONE
            binding.timerToggle.visibility = View.GONE
            binding.include.apply {
                resources.getDimension(R.dimen.spinner_text_minimize).let {
                    numberPickerHours.textSize = it
                    numberPickerMinutes.textSize = it
                    numberPickerSeconds.textSize = it
                    numberPickerHours.selectedTextSize = it
                    numberPickerMinutes.selectedTextSize = it
                    numberPickerSeconds.selectedTextSize = it
                }
            }
        } else {
            binding.minimize.visibility = View.VISIBLE
            binding.timerToggle.visibility = View.VISIBLE
            binding.include.apply {
                resources.getDimension(R.dimen.spinner_text_normal).let {
                    numberPickerHours.textSize = it
                    numberPickerMinutes.textSize = it
                    numberPickerSeconds.textSize = it
                    numberPickerHours.selectedTextSize = it
                    numberPickerMinutes.selectedTextSize = it
                    numberPickerSeconds.selectedTextSize = it
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams(started: Boolean): PictureInPictureParams {
//        val aspectRatio = Rational(binding.include.timer.width, binding.include.timer.height)
        val aspectRatio = Rational(3, 4)
        val visibleRect = Rect()
        binding.include.timer.getGlobalVisibleRect(visibleRect)
        return PictureInPictureParams.Builder()
            .setActions(
                listOf(
                    if (started) {
                        createRemoteAction(
                            R.drawable.ic_stop,
                            R.string.stop,
                            REQUEST_CODE_CONTROL_STOP,
                            CONTROL_TYPE_STOP
                        )
                    } else {
                        createRemoteAction(
                            R.drawable.ic_start,
                            R.string.start,
                            REQUEST_CODE_CONTROL_START,
                            CONTROL_TYPE_START
                        )
                    }
                )
            )
            .setAspectRatio(aspectRatio)
            .setSourceRectHint(visibleRect)
            .build()
            .also {
                setPictureInPictureParams(it)
            }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_TIMER_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        viewModel.apply {
            toggleNotification(!hasFocus)
            checkButtonState()
        }
    }

}