package com.loitp.ui.activity

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import com.loitp.ext.config
import com.loitp.ext.hasNavBar
import com.loitp.ext.hideSystemUI
import com.loitp.ext.showSystemUI
import com.simplemobiletools.commons.extensions.*
import com.loitp.pro.R
import com.loitp.pro.helpers.MIN_SKIP_LENGTH
import com.loitp.pro.helpers.PATH
import kotlinx.android.synthetic.main.activity_panorama_video.*
import kotlinx.android.synthetic.main.bottom_video_time_holder.*
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanoramaVideoActivity : SimpleActivity(), SeekBar.OnSeekBarChangeListener {
    companion object {
        private const val CARDBOARD_DISPLAY_MODE = 3
    }

    private var mIsFullscreen = false
    private var mIsExploreEnabled = true
    private var mIsRendering = false
    private var mIsPlaying = false
    private var mIsDragged = false
    private var mPlayOnReady = false
    private var mDuration = 0
    private var mCurrTime = 0
    private var mTimerHandler = Handler(Looper.getMainLooper())

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama_video)
        supportActionBar?.hide()

        checkNotchSupport()
        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        vrVideoView.resumeRendering()
        mIsRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.circle_black_background)

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }
    }

    override fun onPause() {
        super.onPause()
        vrVideoView.pauseRendering()
        mIsRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mIsRendering) {
            vrVideoView.shutdown()
        }

        if (!isChangingConfigurations) {
            mTimerHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun checkIntent() {
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        setupButtons()
        intent.removeExtra(PATH)

        video_curr_time.setOnClickListener { skip(false) }
        video_duration.setOnClickListener { skip(true) }

        try {
            val options = VrVideoView.Options()
            options.inputType = VrVideoView.Options.TYPE_MONO
            val uri = if (path.startsWith("content://")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }

            vrVideoView.apply {
                loadVideo(uri, options)
                pauseVideo()

                setFlingingEnabled(true)
                setPureTouchTracking(true)

                // add custom buttons so we can position them and toggle visibility as desired
                setFullscreenButtonEnabled(false)
                setInfoButtonEnabled(false)
                setTransitionViewEnabled(false)
                setStereoModeButtonEnabled(false)

                setOnClickListener {
                    handleClick()
                }

                setEventListener(object : VrVideoEventListener() {
                    override fun onClick() {
                        handleClick()
                    }

                    override fun onLoadSuccess() {
                        if (mDuration == 0) {
                            setupDuration(duration)
                            setupTimer()
                        }

                        if (mPlayOnReady || config.autoplayVideos) {
                            mPlayOnReady = false
                            mIsPlaying = true
                            resumeVideo()
                        } else {
                            video_toggle_play_pause.setImageResource(R.drawable.ic_play_outline_vector)
                        }
                        video_toggle_play_pause.beVisible()
                    }

                    override fun onCompletion() {
                        videoCompleted()
                    }
                })
            }

            video_toggle_play_pause.setOnClickListener {
                togglePlayPause()
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupButtons()
    }

    private fun setupDuration(duration: Long) {
        mDuration = (duration / 1000).toInt()
        video_seekbar.max = mDuration
        video_duration.text = mDuration.getFormattedDuration()
        setVideoProgress(0)
    }

    private fun setupTimer() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mIsPlaying && !mIsDragged) {
                    mCurrTime = (vrVideoView!!.currentPosition / 1000).toInt()
                    video_seekbar.progress = mCurrTime
                    video_curr_time.text = mCurrTime.getFormattedDuration()
                }

                mTimerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun togglePlayPause() {
        mIsPlaying = !mIsPlaying
        if (mIsPlaying) {
            resumeVideo()
        } else {
            pauseVideo()
        }
    }

    private fun resumeVideo() {
        video_toggle_play_pause.setImageResource(R.drawable.ic_pause_outline_vector)
        if (mCurrTime == mDuration) {
            setVideoProgress(0)
            mPlayOnReady = true
            return
        }

        vrVideoView.playVideo()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pauseVideo() {
        vrVideoView.pauseVideo()
        video_toggle_play_pause.setImageResource(R.drawable.ic_play_outline_vector)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setVideoProgress(seconds: Int) {
        vrVideoView.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        mCurrTime = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun videoCompleted() {
        mIsPlaying = false
        mCurrTime = (vrVideoView.duration / 1000).toInt()
        video_seekbar.progress = video_seekbar.max
        video_curr_time.text = mDuration.getFormattedDuration()
        pauseVideo()
    }

    private fun setupButtons() {
        var right = 0
        var bottom = 0

        if (hasNavBar()) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += navigationBarHeight
            } else {
                right += navigationBarWidth
                bottom += navigationBarHeight
            }
        }

        video_time_holder.setPadding(0, 0, right, bottom)
        video_time_holder.background =
            ContextCompat.getDrawable(this, R.drawable.gradient_background)
        video_time_holder.onGlobalLayout {
            val newBottomMargin =
                video_time_holder.height - resources.getDimension(R.dimen.video_player_play_pause_size)
                    .toInt() - resources.getDimension(R.dimen.activity_margin).toInt()
            (ivExplore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = newBottomMargin

            (ivCardboard.layoutParams as RelativeLayout.LayoutParams).apply {
                bottomMargin = newBottomMargin
                rightMargin = navigationBarWidth
            }
            ivExplore.requestLayout()
        }
        video_toggle_play_pause.setImageResource(R.drawable.ic_play_outline_vector)

        ivCardboard.setOnClickListener {
            vrVideoView.displayMode = CARDBOARD_DISPLAY_MODE
        }

        ivExplore.setOnClickListener {
            mIsExploreEnabled = !mIsExploreEnabled
            vrVideoView.setPureTouchTracking(mIsExploreEnabled)
            ivExplore.setImageResource(if (mIsExploreEnabled) R.drawable.ic_explore_vector else R.drawable.ic_explore_off_vector)
        }
    }

    private fun toggleButtonVisibility() {
        val newAlpha = if (mIsFullscreen) 0f else 1f
        arrayOf(ivCardboard, ivExplore).forEach {
            it.animate().alpha(newAlpha)
        }

        arrayOf(
            ivCardboard,
            ivExplore,
            video_toggle_play_pause,
            video_curr_time,
            video_duration
        ).forEach {
            it.isClickable = !mIsFullscreen
        }

        video_seekbar.setOnSeekBarChangeListener(if (mIsFullscreen) null else this)
        video_time_holder.animate().alpha(newAlpha).start()
    }

    private fun handleClick() {
        mIsFullscreen = !mIsFullscreen
        toggleButtonVisibility()
        if (mIsFullscreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }

    private fun skip(forward: Boolean) {
        if (forward && mCurrTime == mDuration) {
            return
        }

        val curr = vrVideoView.currentPosition
        val twoPercents = max((vrVideoView.duration / 50).toInt(), MIN_SKIP_LENGTH)
        val newProgress = if (forward) curr + twoPercents else curr - twoPercents
        val roundProgress = (newProgress / 1000f).roundToInt()
        val limitedProgress = max(
            a = min(a = vrVideoView.duration.toInt(), b = roundProgress),
            b = 0
        )
        setVideoProgress(limitedProgress)
        if (!mIsPlaying) {
            togglePlayPause()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            setVideoProgress(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        vrVideoView.pauseVideo()
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        mIsPlaying = true
        resumeVideo()
        mIsDragged = false
    }
}
