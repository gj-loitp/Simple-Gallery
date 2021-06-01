package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.commons.models.RadioItem
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.SLIDESHOW_ANIMATION_FADE
import com.loitp.pro.helpers.SLIDESHOW_ANIMATION_NONE
import com.loitp.pro.helpers.SLIDESHOW_ANIMATION_SLIDE
import com.loitp.pro.helpers.SLIDESHOW_DEFAULT_INTERVAL
import kotlinx.android.synthetic.main.dialog_slideshow.view.*

@SuppressLint("InflateParams")
class SlideshowDialog(
    val activity: BaseSimpleActivity,
    val callback: () -> Unit
) {
    val view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_slideshow, null).apply {
            etIntervalValue.setOnClickListener {
                val text = etIntervalValue.text
                if (text.isNotEmpty()) {
                    text.replace(0, 1, text.subSequence(0, 1), 0, 1)
                    etIntervalValue.selectAll()
                }
            }

            etIntervalValue.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus)
                    activity.hideKeyboard(v)
            }

            layoutAnimation.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(SLIDESHOW_ANIMATION_NONE, activity.getString(R.string.no_animation)),
                    RadioItem(SLIDESHOW_ANIMATION_SLIDE, activity.getString(R.string.slide)),
                    RadioItem(SLIDESHOW_ANIMATION_FADE, activity.getString(R.string.fade))
                )

                RadioGroupDialog(activity, items, activity.config.slideshowAnimation) {
                    activity.config.slideshowAnimation = it as Int
                    tvAnimationValue.text = getAnimationText()
                }
            }

            layoutIncludeVideos.setOnClickListener {
                etIntervalValue.clearFocus()
                include_videos.toggle()
            }

            layoutIncludeGifs.setOnClickListener {
                etIntervalValue.clearFocus()
                swIncludeGifs.toggle()
            }

            layoutRandomOrder.setOnClickListener {
                etIntervalValue.clearFocus()
                swRandomOrder.toggle()
            }

            layoutMoveBackwards.setOnClickListener {
                etIntervalValue.clearFocus()
                swMoveBackwards.toggle()
            }

            layoutLoopSlideshow.setOnClickListener {
                etIntervalValue.clearFocus()
                swLoopSlideshow.toggle()
            }
        }
        setupValues()

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    hideKeyboard()
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        storeValues()
                        callback()
                        dismiss()
                    }
                }
            }
    }

    private fun setupValues() {
        val config = activity.config
        view.apply {
            etIntervalValue.setText(config.slideshowInterval.toString())
            tvAnimationValue.text = getAnimationText()
            include_videos.isChecked = config.slideshowIncludeVideos
            swIncludeGifs.isChecked = config.slideshowIncludeGIFs
            swRandomOrder.isChecked = config.slideshowRandomOrder
            swMoveBackwards.isChecked = config.slideshowMoveBackwards
            swLoopSlideshow.isChecked = config.loopSlideshow
        }
    }

    private fun storeValues() {
        var interval = view.etIntervalValue.text.toString()
        if (interval.trim('0').isEmpty())
            interval = SLIDESHOW_DEFAULT_INTERVAL.toString()

        activity.config.apply {
            slideshowAnimation = getAnimationValue(view.tvAnimationValue.value)
            slideshowInterval = interval.toInt()
            slideshowIncludeVideos = view.include_videos.isChecked
            slideshowIncludeGIFs = view.swIncludeGifs.isChecked
            slideshowRandomOrder = view.swRandomOrder.isChecked
            slideshowMoveBackwards = view.swMoveBackwards.isChecked
            loopSlideshow = view.swLoopSlideshow.isChecked
        }
    }

    private fun getAnimationText(): String {
        return when (activity.config.slideshowAnimation) {
            SLIDESHOW_ANIMATION_SLIDE -> activity.getString(R.string.slide)
            SLIDESHOW_ANIMATION_FADE -> activity.getString(R.string.fade)
            else -> activity.getString(R.string.no_animation)
        }
    }

    private fun getAnimationValue(text: String): Int {
        return when (text) {
            activity.getString(R.string.slide) -> SLIDESHOW_ANIMATION_SLIDE
            activity.getString(R.string.fade) -> SLIDESHOW_ANIMATION_FADE
            else -> SLIDESHOW_ANIMATION_NONE
        }
    }
}
