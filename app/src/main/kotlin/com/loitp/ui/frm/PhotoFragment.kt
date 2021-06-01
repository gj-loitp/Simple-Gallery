package com.loitp.ui.frm

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.exifinterface.media.ExifInterface.*
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.DecoderFactory
import com.davemorrissey.labs.subscaleview.ImageDecoder
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.penfeizhou.animation.webp.WebPDrawable
import com.google.vr.sdk.widgets.video.deps.y
import com.loitp.adapter.PortraitPhotosAdapter
import com.loitp.ext.config
import com.loitp.ext.saveRotatedImageToFile
import com.loitp.ext.sendFakeClick
import com.loitp.helper.MyGlideImageDecoder
import com.loitp.helper.PicassoRegionDecoder
import com.loitp.pro.R
import com.loitp.pro.helpers.*
import com.loitp.model.Medium
import com.loitp.helper.svg.SvgSoftwareLayerSetter
import com.loitp.ui.activity.PanoramaPhotoActivity
import com.loitp.ui.activity.PhotoActivity
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.item_pager_photo.view.*
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import pl.droidsonroids.gif.InputSource
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

class PhotoFragment : ViewPagerFragment() {

    companion object {
        private const val DEFAULT_DOUBLE_TAP_ZOOM = 2f
        private const val ZOOMABLE_VIEW_LOAD_DELAY = 100L
        private const val SAME_ASPECT_RATIO_THRESHOLD = 0.01

        // devices with good displays, but the rest of the hardware not good enough for them
        private val WEIRD_DEVICES = arrayListOf(
            "motorola xt1685",
            "google nexus 5x"
        )
    }

    var mCurrentRotationDegrees = 0
    private var mIsFragmentVisible = false
    private var mIsFullscreen = false
    private var mWasInit = false
    private var mIsPanorama = false
    private var mIsSubsamplingVisible =
        false    // checking view.visibility is unreliable, use an extra variable for it
    private var mCurrentPortraitPhotoPath = ""
    private var mOriginalPath = ""
    private var mImageOrientation = -1
    private var mLoadZoomableViewHandler = Handler()
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mCurrentGestureViewZoom = 1f
    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredAllowDeepZoomableImages = false
    private var mStoredShowHighestQuality = false
    private var mStoredExtendedDetails = 0
    private lateinit var mView: ViewGroup
    private lateinit var mMedium: Medium

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mView = (inflater.inflate(R.layout.item_pager_photo, container, false) as ViewGroup)
        if (!arguments!!.getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return mView
        }

        mMedium = arguments!!.getSerializable(MEDIUM) as Medium
        mOriginalPath = mMedium.path

        mView.apply {
            subSamplingView.setOnClickListener { photoClicked() }
            gesturesImageView.setOnClickListener { photoClicked() }
            gifView.setOnClickListener { photoClicked() }
            instantPrevItem.setOnClickListener { listener?.goToPrevItem() }
            instantNextItem.setOnClickListener { listener?.goToNextItem() }
            ivPanoramaOutline.setOnClickListener { openPanorama() }

            instantPrevItem.parentView = container
            instantNextItem.parentView = container

            scrollPhotoBrightnessController.initialize(
                activity!!,
                tvSlideInfo,
                true,
                container,
                singleTap = { x, y ->
                    mView.apply {
                        if (subSamplingView.isVisible()) {
                            subSamplingView.sendFakeClick(x, y)
                        } else {
                            gesturesImageView.sendFakeClick(x, y)
                        }
                    }
                })

            if (context.config.allowDownGesture) {
                gifView.setOnTouchListener { _, event ->
                    if (layoutGifViewFrame.controller.state.zoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                gesturesImageView.controller.addOnStateChangeListener(object :
                    GestureController.OnStateChangeListener {
                    override fun onStateChanged(state: State) {
                        mCurrentGestureViewZoom = state.zoom
                    }
                })

                gesturesImageView.setOnTouchListener { _, event ->
                    if (mCurrentGestureViewZoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                subSamplingView.setOnTouchListener { _, event ->
                    if (subSamplingView.isZoomedOut()) {
                        handleEvent(event)
                    }
                    false
                }
            }
        }

        checkScreenDimensions()
        storeStateVariables()
        if (!mIsFragmentVisible && activity is PhotoActivity) {
            mIsFragmentVisible = true
        }

        if (mMedium.path.startsWith("content://") && !mMedium.path.startsWith("content://mms/")) {
            mMedium.path = context!!.getRealPathFromURI(Uri.parse(mOriginalPath)) ?: mMedium.path

            if (mMedium.path.isEmpty()) {
                var out: FileOutputStream? = null
                try {
                    var inputStream =
                        context!!.contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val exif = ExifInterface()
                    exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                    val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                    val orientation = tag?.getValueAsInt(-1) ?: -1
                    inputStream =
                        context!!.contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val original = BitmapFactory.decodeStream(inputStream)
                    val rotated = rotateViaMatrix(original, orientation)
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1)
                    exif.removeCompressedThumbnail()

                    val file =
                        File(context!!.externalCacheDir, Uri.parse(mOriginalPath).lastPathSegment)
                    out = FileOutputStream(file)
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    mMedium.path = file.absolutePath
                } catch (e: Exception) {
                    activity!!.toast(R.string.unknown_error_occurred)
                    return mView
                } finally {
                    out?.close()
                }
            }
        }

        mIsFullscreen =
            activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        loadImage()
        initExtendedDetails()
        mWasInit = true
        checkIfPanorama()
        updateInstantSwitchWidths()

        return mView
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        val config = context!!.config
        if (mWasInit && (config.showExtendedDetails != mStoredShowExtendedDetails || config.extendedDetails != mStoredExtendedDetails)) {
            initExtendedDetails()
        }

        if (mWasInit) {
            if (config.allowZoomingImages != mStoredAllowDeepZoomableImages || config.showHighestQuality != mStoredShowHighestQuality) {
                mIsSubsamplingVisible = false
                mView.subSamplingView.beGone()
                loadImage()
            } else if (mMedium.isGIF()) {
                loadGif()
            }
        }

        val allowPhotoGestures = config.allowPhotoGestures
        val allowInstantChange = config.allowInstantChange

        mView.apply {
            scrollPhotoBrightnessController.beVisibleIf(allowPhotoGestures)
            instantPrevItem.beVisibleIf(allowInstantChange)
            instantNextItem.beVisibleIf(allowInstantChange)
        }

        storeStateVariables()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity?.isDestroyed == false) {
            mView.subSamplingView.recycle()

            try {
                if (context != null) {
                    Glide.with(context!!).clear(mView.gesturesImageView)
                }
            } catch (ignored: Exception) {
            }
        }

        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        if (mCurrentRotationDegrees != 0) {
            ensureBackgroundThread {
                val path = mMedium.path
                (activity as? BaseSimpleActivity)?.saveRotatedImageToFile(
                    oldPath = path,
                    newPath = path,
                    degrees = mCurrentRotationDegrees,
                    showToasts = false
                ) {}
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!mWasInit) {
            return
        }

        // avoid GIFs being skewed, played in wrong aspect ratio
        if (mMedium.isGIF()) {
            mView.onGlobalLayout {
                if (activity != null) {
                    measureScreen()
                    Handler(Looper.getMainLooper()).postDelayed({
                        mView.layoutGifViewFrame.controller.resetState()
                        loadGif()
                    }, 50)
                }
            }
        } else {
            hideZoomableView()
            loadImage()
        }

        measureScreen()
        initExtendedDetails()
        updateInstantSwitchWidths()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        mIsFragmentVisible = menuVisible
        if (mWasInit) {
            if (!mMedium.isGIF() && !mMedium.isWebP()) {
                photoFragmentVisibilityChanged(menuVisible)
            }
        }
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredAllowDeepZoomableImages = allowZoomingImages
            mStoredShowHighestQuality = showHighestQuality
            mStoredExtendedDetails = extendedDetails
        }
    }

    private fun checkScreenDimensions() {
        if (mScreenWidth == 0 || mScreenHeight == 0) {
            measureScreen()
        }
    }

    private fun measureScreen() {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getRealMetrics(metrics)
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels
    }

    private fun photoFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            scheduleZoomableView()
        } else {
            hideZoomableView()
        }
    }

    private fun degreesForRotation(orientation: Int) = when (orientation) {
        ORIENTATION_ROTATE_270 -> 270
        ORIENTATION_ROTATE_180 -> 180
        ORIENTATION_ROTATE_90 -> 90
        else -> 0
    }

    private fun rotateViaMatrix(original: Bitmap, orientation: Int): Bitmap {
        val degrees = degreesForRotation(orientation).toFloat()
        return if (degrees == 0f) {
            original
        } else {
            val matrix = Matrix()
            matrix.setRotate(degrees)
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        }
    }

    private fun loadImage() {
        checkScreenDimensions()

        if (mMedium.isPortrait() && context != null) {
            showPortraitStripe()
        }

        ensureBackgroundThread {
            mImageOrientation = getImageOrientation()
            activity?.runOnUiThread {
                when {
                    mMedium.isGIF() -> loadGif()
                    mMedium.isSVG() -> loadSVG()
                    else -> loadBitmap()
                }
            }
        }
    }

    private fun loadGif() {
        try {
            val pathToLoad = getPathToLoad(mMedium)
            val source =
                if (pathToLoad.startsWith("content://") || pathToLoad.startsWith("file://")) {
                    InputSource.UriSource(context!!.contentResolver, Uri.parse(pathToLoad))
                } else {
                    InputSource.FileSource(pathToLoad)
                }

            mView.apply {
                gesturesImageView.beGone()
                layoutGifViewFrame.beVisible()
                ensureBackgroundThread {
                    gifView.setInputSource(source)
                }
            }
        } catch (e: Exception) {
            loadBitmap()
        } catch (e: OutOfMemoryError) {
            loadBitmap()
        }
    }

    private fun loadSVG() {
        if (context != null) {
            Glide.with(context!!)
                .`as`(PictureDrawable::class.java)
                .listener(SvgSoftwareLayerSetter())
                .load(mMedium.path)
                .into(mView.gesturesImageView)
        }
    }

    private fun loadBitmap(addZoomableView: Boolean = true) {
        if (context == null) {
            return
        }

        val path = getFilePathToShow()
        if (path.isWebP()) {
            val drawable = WebPDrawable.fromFile(path)
            if (drawable.intrinsicWidth == 0) {
                loadWithGlide(path, addZoomableView)
            } else {
                drawable.setLoopLimit(0)
                mView.gesturesImageView.setImageDrawable(drawable)
            }
        } else {
            loadWithGlide(path, addZoomableView)
        }
    }

    private fun loadWithGlide(path: String, addZoomableView: Boolean) {
        val priority = if (mIsFragmentVisible) Priority.IMMEDIATE else Priority.NORMAL
        val options = RequestOptions()
            .signature(mMedium.getKey())
            .format(DecodeFormat.PREFER_ARGB_8888)
            .priority(priority)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .fitCenter()

        if (mCurrentRotationDegrees != 0) {
            options.transform(Rotate(mCurrentRotationDegrees))
            options.diskCacheStrategy(DiskCacheStrategy.NONE)
        }

        Glide.with(context!!)
            .load(path)
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (activity != null && !activity!!.isDestroyed && !activity!!.isFinishing) {
                        tryLoadingWithPicasso(addZoomableView)
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    mView.gesturesImageView.controller.settings.isZoomEnabled =
                        mMedium.isRaw() || mCurrentRotationDegrees != 0 || context?.config?.allowZoomingImages == false
                    if (mIsFragmentVisible && addZoomableView) {
                        scheduleZoomableView()
                    }
                    return false
                }
            }).into(mView.gesturesImageView)
    }

    private fun tryLoadingWithPicasso(addZoomableView: Boolean) {
        var pathToLoad =
            if (getFilePathToShow().startsWith("content://")) getFilePathToShow() else "file://${getFilePathToShow()}"
        pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")

        try {
            val picasso = Picasso.get()
                .load(pathToLoad)
                .centerInside()
                .stableKey(mMedium.getSignature())
                .resize(mScreenWidth, mScreenHeight)

            if (mCurrentRotationDegrees != 0) {
                picasso.rotate(mCurrentRotationDegrees.toFloat())
            } else {
                degreesForRotation(mImageOrientation).toFloat()
            }

            picasso.into(mView.gesturesImageView, object : Callback {
                override fun onSuccess() {
                    mView.gesturesImageView.controller.settings.isZoomEnabled =
                        mMedium.isRaw() || mCurrentRotationDegrees != 0 || context?.config?.allowZoomingImages == false
                    if (mIsFragmentVisible && addZoomableView) {
                        scheduleZoomableView()
                    }
                }

                override fun onError(e: Exception?) {
                    if (mMedium.path != mOriginalPath) {
                        mMedium.path = mOriginalPath
                        loadImage()
                        checkIfPanorama()
                    }
                }
            })
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }

    private fun showPortraitStripe() {
        val files = File(mMedium.parentPath).listFiles()?.toMutableList() as? ArrayList<File>
        if (files != null) {
            val screenWidth = context!!.realScreenSize.x
            val itemWidth = context!!.resources.getDimension(R.dimen.portrait_photos_stripe_height)
                .toInt() + context!!.resources.getDimension(R.dimen.one_dp).toInt()
            val sideWidth = screenWidth / 2 - itemWidth / 2
            val fakeItemsCnt = ceil(sideWidth / itemWidth.toDouble()).toInt()

            val paths = fillPhotoPaths(files, fakeItemsCnt)
            var curWidth = itemWidth
            while (curWidth < screenWidth) {
                curWidth += itemWidth
            }

            val sideElementWidth = curWidth - screenWidth
            val adapter = PortraitPhotosAdapter(context!!, paths, sideElementWidth) { position, x ->
                if (mIsFullscreen) {
                    return@PortraitPhotosAdapter
                }

                mView.rvPhotoPortraitStripe.smoothScrollBy((x + itemWidth / 2) - screenWidth / 2, 0)
                if (paths[position] != mCurrentPortraitPhotoPath) {
                    mCurrentPortraitPhotoPath = paths[position]
                    hideZoomableView()
                    loadBitmap()
                }
            }

            mView.rvPhotoPortraitStripe.adapter = adapter
            setupStripeBottomMargin()

            val coverIndex = getCoverImageIndex(paths)
            if (coverIndex != -1) {
                mCurrentPortraitPhotoPath = paths[coverIndex]
                setupStripeUpListener(
                    adapter = adapter,
                    screenWidth = screenWidth,
                    itemWidth = itemWidth
                )

                mView.rvPhotoPortraitStripe.onGlobalLayout {
                    mView.rvPhotoPortraitStripe.scrollBy((coverIndex - fakeItemsCnt) * itemWidth, 0)
                    adapter.setCurrentPhoto(coverIndex)
                    mView.layoutPhotoPortraitStripeWrapper.beVisible()
                    if (mIsFullscreen) {
                        mView.layoutPhotoPortraitStripeWrapper.alpha = 0f
                    }
                }
            }
        }
    }

    private fun fillPhotoPaths(files: ArrayList<File>, fakeItemsCnt: Int): ArrayList<String> {
        val paths = ArrayList<String>()
        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }

        files.forEach {
            paths.add(it.absolutePath)
        }

        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }
        return paths
    }

    private fun setupStripeBottomMargin() {
        var bottomMargin =
            context!!.navigationBarHeight + context!!.resources.getDimension(R.dimen.normal_margin)
                .toInt()
        if (context!!.config.bottomActions) {
            bottomMargin += context!!.resources.getDimension(R.dimen.bottom_actions_height).toInt()
        }
        (mView.layoutPhotoPortraitStripeWrapper.layoutParams as RelativeLayout.LayoutParams).bottomMargin =
            bottomMargin
    }

    private fun getCoverImageIndex(paths: ArrayList<String>): Int {
        var coverIndex = -1
        paths.forEachIndexed { index, path ->
            if (path.contains("cover", true)) {
                coverIndex = index
            }
        }

        if (coverIndex == -1) {
            paths.forEachIndexed { index, path ->
                if (path.isNotEmpty()) {
                    coverIndex = index
                }
            }
        }
        return coverIndex
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupStripeUpListener(
        adapter: PortraitPhotosAdapter,
        screenWidth: Int,
        itemWidth: Int
    ) {
        mView.rvPhotoPortraitStripe.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                var closestIndex = -1
                var closestDistance = Integer.MAX_VALUE
                val center = screenWidth / 2
                for ((key, value) in adapter.views) {
                    val distance = abs(value.x.toInt() + itemWidth / 2 - center)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestIndex = key
                    }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    adapter.performClickOn(closestIndex)
                }, 100)
            }
            false
        }
    }

    private fun getFilePathToShow() =
        if (mMedium.isPortrait()) mCurrentPortraitPhotoPath else getPathToLoad(mMedium)

    private fun openPanorama() {
        Intent(context, PanoramaPhotoActivity::class.java).apply {
            putExtra(PATH, mMedium.path)
            startActivity(this)
        }
    }

    private fun scheduleZoomableView() {
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        mLoadZoomableViewHandler.postDelayed({
            if (mIsFragmentVisible && context?.config?.allowZoomingImages == true &&
                (mMedium.isImage() || mMedium.isPortrait()) &&
                !mIsSubsamplingVisible
            ) {
                addZoomableView()
            }
        }, ZOOMABLE_VIEW_LOAD_DELAY)
    }

    private fun addZoomableView() {
        val rotation = degreesForRotation(mImageOrientation)
        mIsSubsamplingVisible = true
        val config = context!!.config
        val showHighestQuality = config.showHighestQuality
        val minTileDpi = if (showHighestQuality) -1 else getMinTileDpi()

        val bitmapDecoder = object : DecoderFactory<ImageDecoder> {
            override fun make() = MyGlideImageDecoder(rotation, mMedium.getKey())
        }

        val regionDecoder = object : DecoderFactory<ImageRegionDecoder> {
            override fun make() =
                PicassoRegionDecoder(
                    showHighestQuality = showHighestQuality,
                    screenWidth = mScreenWidth,
                    screenHeight = mScreenHeight,
                    minTileDpi = minTileDpi
                )
        }

        var newOrientation = (rotation + mCurrentRotationDegrees) % 360
        if (newOrientation < 0) {
            newOrientation += 360
        }

        mView.subSamplingView.apply {
            setMaxTileSize(if (showHighestQuality) Integer.MAX_VALUE else 4096)
            setMinimumTileDpi(minTileDpi)
            background = ColorDrawable(Color.TRANSPARENT)
            bitmapDecoderFactory = bitmapDecoder
            regionDecoderFactory = regionDecoder
            maxScale = 10f
            beVisible()
            rotationEnabled = config.allowRotatingWithGestures
            isOneToOneZoomEnabled = config.allowOneToOneZoom
            orientation = newOrientation
            setImage(getFilePathToShow())

            onImageEventListener = object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onReady() {
                    background =
                        ColorDrawable(if (config.blackBackground) Color.BLACK else config.backgroundColor)
                    val useWidth =
                        if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sHeight else sWidth
                    val useHeight =
                        if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                }

                override fun onImageLoadError(e: Exception) {
                    mView.gesturesImageView.controller.settings.isZoomEnabled = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    mIsSubsamplingVisible = false
                    beGone()
                }

                override fun onImageRotation(degrees: Int) {
                    val fullRotation = (rotation + degrees) % 360
                    val useWidth =
                        if (fullRotation == 90 || fullRotation == 270) sHeight else sWidth
                    val useHeight =
                        if (fullRotation == 90 || fullRotation == 270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                    mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
                    loadBitmap(false)
                    activity?.invalidateOptionsMenu()
                }
            }
        }
    }

    private fun getMinTileDpi(): Int {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        val device = "${Build.BRAND} ${Build.MODEL}".toLowerCase(Locale.getDefault())
        return when {
            WEIRD_DEVICES.contains(device) -> WEIRD_TILE_DPI
            averageDpi > 400 -> HIGH_TILE_DPI
            averageDpi > 300 -> NORMAL_TILE_DPI
            else -> LOW_TILE_DPI
        }
    }

    private fun checkIfPanorama() {
        mIsPanorama = try {
            val inputStream =
                if (mMedium.path.startsWith("content:/")) context!!.contentResolver.openInputStream(
                    Uri.parse(mMedium.path)
                ) else File(mMedium.path).inputStream()
            val imageParser = JpegImageParser().getXmpXml(
                ByteSourceInputStream(inputStream, mMedium.name),
                HashMap<String, Any>()
            )
            imageParser.contains("GPano:UsePanoramaViewer=\"True\"", true) ||
                imageParser.contains(
                    "<GPano:UsePanoramaViewer>True</GPano:UsePanoramaViewer>",
                    true
                ) ||
                imageParser.contains("GPano:FullPanoWidthPixels=") ||
                imageParser.contains("GPano:ProjectionType>Equirectangular")
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }

        mView.ivPanoramaOutline.beVisibleIf(mIsPanorama)
        if (mIsFullscreen) {
            mView.ivPanoramaOutline.alpha = 0f
        }
    }

    private fun getImageOrientation(): Int {
        val defaultOrientation = -1
        var orient = defaultOrientation

        try {
            val path = getFilePathToShow()
            orient = if (path.startsWith("content:/")) {
                val inputStream = context!!.contentResolver.openInputStream(Uri.parse(path))
                val exif = ExifInterface()
                exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                tag?.getValueAsInt(defaultOrientation) ?: defaultOrientation
            } else {
                val exif = androidx.exifinterface.media.ExifInterface(path)
                exif.getAttributeInt(TAG_ORIENTATION, defaultOrientation)
            }

            if (orient == defaultOrientation || context!!.isPathOnOTG(getFilePathToShow())) {
                val uri =
                    if (path.startsWith("content:/")) Uri.parse(path) else Uri.fromFile(File(path))
                val inputStream = context!!.contentResolver.openInputStream(uri)
                val exif2 = ExifInterface()
                exif2.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                orient =
                    exif2.getTag(ExifInterface.TAG_ORIENTATION)?.getValueAsInt(defaultOrientation)
                        ?: defaultOrientation
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        } catch (ignored: OutOfMemoryError) {
            ignored.printStackTrace()
        }
        return orient
    }

    private fun getDoubleTapZoomScale(width: Int, height: Int): Float {
        val bitmapAspectRatio = height / width.toFloat()
        val screenAspectRatio = mScreenHeight / mScreenWidth.toFloat()

        return if (context == null || abs(bitmapAspectRatio - screenAspectRatio) < SAME_ASPECT_RATIO_THRESHOLD) {
            DEFAULT_DOUBLE_TAP_ZOOM
        } else if (context!!.portrait && bitmapAspectRatio <= screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else if (context!!.portrait && bitmapAspectRatio > screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio >= screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio < screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else {
            DEFAULT_DOUBLE_TAP_ZOOM
        }
    }

    fun rotateImageViewBy(degrees: Int) {
        if (mIsSubsamplingVisible) {
            mView.subSamplingView.rotateBy(degrees)
        } else {
            mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
            mIsSubsamplingVisible = false
            loadBitmap()
        }
    }

    private fun initExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            mView.tvPhotoDetails.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(mMedium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha =
                                if (!context!!.config.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            mView.tvPhotoDetails.beGone()
        }
    }

    private fun hideZoomableView() {
        if (context?.config?.allowZoomingImages == true) {
            mIsSubsamplingVisible = false
            mView.subSamplingView.recycle()
            mView.subSamplingView.beGone()
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }

    private fun updateInstantSwitchWidths() {
        mView.instantPrevItem.layoutParams.width = mScreenWidth / 7
        mView.instantNextItem.layoutParams.width = mScreenWidth / 7
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        this.mIsFullscreen = isFullscreen
        mView.apply {
            tvPhotoDetails.apply {
                if (mStoredShowExtendedDetails && isVisible() && context != null && resources != null) {
                    animate().y(getExtendedDetailsY(height))

                    if (mStoredHideExtendedDetails) {
                        animate().alpha(if (isFullscreen) 0f else 1f).start()
                    }
                }
            }

            if (mIsPanorama) {
                ivPanoramaOutline.animate().alpha(if (isFullscreen) 0f else 1f).start()
                ivPanoramaOutline.isClickable = !isFullscreen
            }

            if (mWasInit && mMedium.isPortrait()) {
                layoutPhotoPortraitStripeWrapper.animate().alpha(if (isFullscreen) 0f else 1f).start()
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = context?.resources?.getDimension(R.dimen.small_margin) ?: return 0f
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else context!!.navigationBarHeight
        val actionsHeight =
            if (context!!.config.bottomActions && !mIsFullscreen) resources.getDimension(R.dimen.bottom_actions_height) else 0f
        return context!!.realScreenSize.y - height - actionsHeight - fullscreenOffset
    }
}
