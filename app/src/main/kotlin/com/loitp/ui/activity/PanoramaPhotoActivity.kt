package com.loitp.ui.activity

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.ext.hideSystemUI
import com.loitp.ext.showSystemUI
import com.loitp.pro.helpers.PATH
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.activity_panorama_photo.*

open class PanoramaPhotoActivity : SimpleActivity() {
    companion object {
        private const val CARDBOARD_DISPLAY_MODE = 3
    }

    private var isFullscreen = false
    private var isExploreEnabled = true
    private var isRendering = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama_photo)
        supportActionBar?.hide()

        checkNotchSupport()
        setupButtonMargins()

        ivCardboard.setOnClickListener {
            panoramaView.displayMode = CARDBOARD_DISPLAY_MODE
        }

        ivExplore.setOnClickListener {
            isExploreEnabled = !isExploreEnabled
            panoramaView.setPureTouchTracking(isExploreEnabled)
            ivExplore.setImageResource(if (isExploreEnabled) R.drawable.ic_explore_vector else R.drawable.ic_explore_off_vector)
        }

        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        panoramaView.resumeRendering()
        isRendering = true
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
        panoramaView.pauseRendering()
        isRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRendering) {
            panoramaView.shutdown()
        }
    }

    private fun checkIntent() {
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        intent.removeExtra(PATH)

        try {
            val options = VrPanoramaView.Options()
            options.inputType = VrPanoramaView.Options.TYPE_MONO
            ensureBackgroundThread {
                val bitmap = getBitmapToLoad(path)
                runOnUiThread {
                    panoramaView.apply {
                        beVisible()
                        loadImageFromBitmap(bitmap, options)
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

                        setEventListener(object : VrPanoramaEventListener() {
                            override fun onClick() {
                                handleClick()
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupButtonMargins()
    }

    private fun getBitmapToLoad(path: String): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        var bitmap: Bitmap? = null

        for (i in 0..10) {
            try {
                bitmap = if (path.startsWith("content://")) {
                    val inputStream = contentResolver.openInputStream(Uri.parse(path))
                    BitmapFactory.decodeStream(inputStream)
                } else {
                    BitmapFactory.decodeFile(path, options)
                }
                break
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            }
        }

        return bitmap
    }

    private fun setupButtonMargins() {
        val navBarHeight = navigationBarHeight
        (ivCardboard.layoutParams as RelativeLayout.LayoutParams).apply {
            bottomMargin = navBarHeight
            rightMargin = navigationBarWidth
        }

        (ivExplore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = navigationBarHeight

        ivCardboard.onGlobalLayout {
            ivPanoramaGradientBackground.layoutParams.height = navBarHeight + ivCardboard.height
        }
    }

    private fun toggleButtonVisibility() {
        arrayOf(ivCardboard, ivExplore, ivPanoramaGradientBackground).forEach {
            it.animate().alpha(if (isFullscreen) 0f else 1f)
            it.isClickable = !isFullscreen
        }
    }

    private fun handleClick() {
        isFullscreen = !isFullscreen
        toggleButtonVisibility()
        if (isFullscreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }
}
