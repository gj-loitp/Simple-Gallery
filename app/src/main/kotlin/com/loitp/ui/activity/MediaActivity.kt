package com.loitp.ui.activity

import android.app.Activity
import android.app.SearchManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.loitp.pro.R
import com.loitp.adapter.MediaAdapter
import com.loitp.service.GetMediaAsyncTask
import com.loitp.db.GalleryDatabase
import com.loitp.ext.*
import com.loitp.helper.MediaFetcher
import com.loitp.ui.dialog.ChangeGroupingDialog
import com.loitp.ui.dialog.ChangeSortingDialog
import com.loitp.ui.dialog.ChangeViewTypeDialog
import com.loitp.ui.dialog.FilterMediaDialog
import com.loitp.pro.helpers.*
import com.loitp.interfaces.MediaOperationsListener
import com.loitp.model.Medium
import com.loitp.model.ThumbnailItem
import com.loitp.model.ThumbnailSection
import com.loitp.ui.view.GridSpacingItemDecoration
import com.simplemobiletools.commons.dialogs.CreateNewFolderDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MediaActivity : SimpleActivity(), MediaOperationsListener {
    companion object {
        private const val LAST_MEDIA_CHECK_PERIOD = 3000L
        var mMedia = ArrayList<ThumbnailItem>()
    }

    private var mPath = ""
    private var mIsGetImageIntent = false
    private var mIsGetVideoIntent = false
    private var mIsGetAnyIntent = false
    private var mIsGettingMedia = false
    private var mAllowPickingMultiple = false
    private var mShowAll = false
    private var mLoadedInitialPhotos = false
    private var mIsSearchOpen = false
    private var mLastSearchedText = ""
    private var mDateFormat = ""
    private var mTimeFormat = ""
    private var mLatestMediaId = 0L
    private var mLatestMediaDateId = 0L
    private var mLastMediaHandler = Handler(Looper.getMainLooper())
    private var mTempShowHiddenHandler = Handler(Looper.getMainLooper())
    private var mCurrAsyncTask: GetMediaAsyncTask? = null
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredShowFileTypes = true
    private var mStoredRoundedCorners = false
    private var mStoredTextColor = 0
    private var mStoredAdjustedPrimaryColor = 0
    private var mStoredThumbnailSpacing = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        intent.apply {
            mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(GET_ANY_INTENT, false)
            mAllowPickingMultiple = getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        layoutMediaRefresh.setOnRefreshListener { getMedia() }
        try {
            mPath = intent.getStringExtra(DIRECTORY) ?: ""
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
            return
        }

        storeStateVariables()

        if (mShowAll) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            registerFileUpdateListener()
        }

        tvMediaEmpty2.setOnClickListener {
            showFilterMediaDialog()
        }

        updateWidgets()
    }

    override fun onStart() {
        super.onStart()
        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        mDateFormat = config.dateFormat
        mTimeFormat = getTimeFormat()

        if (mStoredAnimateGifs != config.animateGifs) {
            getMediaAdapter()?.updateAnimateGifs(config.animateGifs)
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            getMediaAdapter()?.updateCropThumbnails(config.cropThumbnails)
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            mLoadedInitialPhotos = false
            rvMedia.adapter = null
            getMedia()
        }

        if (mStoredShowFileTypes != config.showThumbnailFileTypes) {
            getMediaAdapter()?.updateShowFileTypes(config.showThumbnailFileTypes)
        }

        if (mStoredTextColor != config.textColor) {
            getMediaAdapter()?.updateTextColor(config.textColor)
        }

        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        if (mStoredAdjustedPrimaryColor != adjustedPrimaryColor) {
            getMediaAdapter()?.updatePrimaryColor(config.primaryColor)
            fsMediaHorizontal.updatePrimaryColor(adjustedPrimaryColor)
            fsMediaVertical.updatePrimaryColor(adjustedPrimaryColor)
        }

        if (mStoredThumbnailSpacing != config.thumbnailSpacing) {
            rvMedia.adapter = null
            setupAdapter()
        }

        if (mStoredRoundedCorners != config.fileRoundedCorners) {
            rvMedia.adapter = null
            setupAdapter()
        }

        fsMediaHorizontal.updateBubbleColors()
        fsMediaVertical.updateBubbleColors()
        layoutMediaRefresh.isEnabled = config.enablePullToRefresh
        tvMediaEmpty.setTextColor(config.textColor)
        tvMediaEmpty2.setTextColor(getAdjustedPrimaryColor())

        if (!mIsSearchOpen) {
            invalidateOptionsMenu()
        }

        if (mMedia.isEmpty() || config.getFolderSorting(mPath) and SORT_BY_RANDOM == 0) {
            if (shouldSkipAuthentication()) {
                tryLoadGallery()
            } else {
                handleLockedFolderOpening(mPath) { success ->
                    if (success) {
                        tryLoadGallery()
                    } else {
                        finish()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mIsGettingMedia = false
        layoutMediaRefresh.isRefreshing = false
        storeStateVariables()
        mLastMediaHandler.removeCallbacksAndMessages(null)

        if (mMedia.isNotEmpty()) {
            mCurrAsyncTask?.stopFetching()
        }
    }

    override fun onStop() {
        super.onStop()

        if (config.temporarilyShowHidden || config.tempSkipDeleteConfirmation) {
            mTempShowHiddenHandler.postDelayed({
                config.temporarilyShowHidden = false
                config.tempSkipDeleteConfirmation = false
            }, SHOW_TEMP_HIDDEN_DURATION)
        } else {
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (config.showAll && !isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            unregisterFileUpdateListener()
            GalleryDatabase.destroyInstance()
        }

        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        mMedia.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_media, menu)

        val isDefaultFolder =
            config.defaultFolder.isNotEmpty() && File(config.defaultFolder).compareTo(File(mPath)) == 0

        menu.apply {
            findItem(R.id.menuGroupBy).isVisible = !config.scrollHorizontally

            findItem(R.id.menuEmptyRecycleBin).isVisible = mPath == RECYCLE_BIN
            findItem(R.id.menuEmptyDisableRecycleBin).isVisible = mPath == RECYCLE_BIN
            findItem(R.id.menuRestoreAllFiles).isVisible = mPath == RECYCLE_BIN

            findItem(R.id.menuFolderView).isVisible = mShowAll
            findItem(R.id.menuOpenCamera).isVisible = mShowAll
            findItem(R.id.menuCreateNewFolder).isVisible =
                !mShowAll && mPath != RECYCLE_BIN && mPath != FAVORITES

            findItem(R.id.menuTemporarilyShowHidden).isVisible = !config.shouldShowHidden
            findItem(R.id.menuStopShowingHidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.menuSetAsDefaultFolder).isVisible = !isDefaultFolder
            findItem(R.id.menuUnsetAsDefaultFolder).isVisible = isDefaultFolder

            val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
            findItem(R.id.menuIncreaseColumnCount).isVisible =
                viewType == VIEW_TYPE_GRID && config.mediaColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.menuReduceColumnCount).isVisible =
                viewType == VIEW_TYPE_GRID && config.mediaColumnCnt > 1
            findItem(R.id.menuToggleFileName).isVisible = viewType == VIEW_TYPE_GRID
        }

        setupSearch(menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSort -> showSortingDialog()
            R.id.menuFilter -> showFilterMediaDialog()
            R.id.menuEmptyRecycleBin -> emptyRecycleBin()
            R.id.menuEmptyDisableRecycleBin -> emptyAndDisableRecycleBin()
            R.id.menuRestoreAllFiles -> restoreAllFiles()
            R.id.menuToggleFileName -> toggleFilenameVisibility()
            R.id.menuOpenCamera -> launchCamera()
            R.id.menuFolderView -> switchToFolderView()
            R.id.menuChangeViewType -> changeViewType()
            R.id.menuGroupBy -> showGroupByDialog()
            R.id.menuCreateNewFolder -> createNewFolder()
            R.id.menuTemporarilyShowHidden -> tryToggleTemporarilyShowHidden()
            R.id.menuStopShowingHidden -> tryToggleTemporarilyShowHidden()
            R.id.menuIncreaseColumnCount -> increaseColumnCount()
            R.id.menuReduceColumnCount -> reduceColumnCount()
            R.id.menuSetAsDefaultFolder -> setAsDefaultFolder()
            R.id.menuUnsetAsDefaultFolder -> unsetAsDefaultFolder()
            R.id.menuSlideShow -> startSlideshow()
            R.id.menuSettings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startSlideshow() {
        if (mMedia.isNotEmpty()) {
            Intent(this, ViewPagerActivity::class.java).apply {
                val item = mMedia.firstOrNull { it is Medium } as? Medium ?: return
                putExtra(SKIP_AUTHENTICATION, shouldSkipAuthentication())
                putExtra(PATH, item.path)
                putExtra(SHOW_ALL, mShowAll)
                putExtra(SLIDESHOW_START_ON_ENTER, true)
                startActivity(this)
            }
        }
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredAnimateGifs = animateGifs
            mStoredCropThumbnails = cropThumbnails
            mStoredScrollHorizontally = scrollHorizontally
            mStoredShowFileTypes = showThumbnailFileTypes
            mStoredTextColor = textColor
            mStoredThumbnailSpacing = thumbnailSpacing
            mStoredRoundedCorners = fileRoundedCorners
            mShowAll = showAll
        }
        mStoredAdjustedPrimaryColor = getAdjustedPrimaryColor()
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.menuSearch)
        (mSearchMenuItem?.actionView as? SearchView)?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        mLastSearchedText = newText
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(
            mSearchMenuItem,
            object : MenuItemCompat.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    mIsSearchOpen = true
                    layoutMediaRefresh.isEnabled = false
                    return true
                }

                // this triggers on device rotation too, avoid doing anything
                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    if (mIsSearchOpen) {
                        mIsSearchOpen = false
                        mLastSearchedText = ""

                        layoutMediaRefresh.isEnabled = config.enablePullToRefresh
                        searchQueryChanged("")
                    }
                    return true
                }
            })
    }

    private fun searchQueryChanged(text: String) {
        ensureBackgroundThread {
            try {
                val filtered =
                    mMedia.filter { it is Medium && it.name.contains(text, true) } as ArrayList
                filtered.sortBy { it is Medium && !it.name.startsWith(text, true) }
                val grouped = MediaFetcher(applicationContext).groupMedia(
                    filtered as ArrayList<Medium>,
                    mPath
                )
                runOnUiThread {
                    if (grouped.isEmpty()) {
                        tvMediaEmpty.text = getString(R.string.no_items_found)
                        tvMediaEmpty.beVisible()
                    } else {
                        tvMediaEmpty.beGone()
                    }

                    handleGridSpacing(grouped)
                    getMediaAdapter()?.updateMedia(grouped)
                    measureRecyclerViewContent(grouped)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun tryLoadGallery() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                val dirName = when (mPath) {
                    FAVORITES -> getString(R.string.favorites)
                    RECYCLE_BIN -> getString(R.string.recycle_bin)
                    config.OTGPath -> getString(R.string.usb)
                    else -> getHumanizedFilename(mPath)
                }
                updateActionBarTitle(if (mShowAll) resources.getString(R.string.all_folders) else dirName)
                getMedia()
                setupLayoutManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun getMediaAdapter() = rvMedia.adapter as? MediaAdapter

    private fun setupAdapter() {
        if (!mShowAll && isDirEmpty()) {
            return
        }

        val currAdapter = rvMedia.adapter
        if (currAdapter == null) {
            initZoomListener()
            val fastScroller =
                if (config.scrollHorizontally) fsMediaHorizontal else fsMediaVertical
            MediaAdapter(
                this,
                mMedia.clone() as ArrayList<ThumbnailItem>,
                this,
                mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent,
                mAllowPickingMultiple,
                mPath,
                rvMedia,
                fastScroller
            ) {
                if (it is Medium && !isFinishing) {
                    itemClicked(it.path)
                }
            }.apply {
                setupZoomListener(mZoomListener)
                rvMedia.adapter = this
            }

            val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
            if (viewType == VIEW_TYPE_LIST) {
                rvMedia.scheduleLayoutAnimation()
            }

            setupLayoutManager()
            handleGridSpacing()
            measureRecyclerViewContent(mMedia)
        } else if (mLastSearchedText.isEmpty()) {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
            handleGridSpacing()
            measureRecyclerViewContent(mMedia)
        } else {
            searchQueryChanged(mLastSearchedText)
        }

        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        val allowHorizontalScroll = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
        fsMediaVertical.isHorizontal = false
        fsMediaVertical.beGoneIf(allowHorizontalScroll)

        fsMediaHorizontal.isHorizontal = true
        fsMediaHorizontal.beVisibleIf(allowHorizontalScroll)

        val sorting = config.getFolderSorting(if (mShowAll) SHOW_ALL else mPath)
        if (allowHorizontalScroll) {
            fsMediaHorizontal.setViews(
                recyclerView = rvMedia,
                swipeRefreshLayout = layoutMediaRefresh
            ) {
                fsMediaHorizontal.updateBubbleText(
                    getBubbleTextItem(
                        index = it,
                        sorting = sorting
                    )
                )
            }
        } else {
            fsMediaVertical.setViews(
                recyclerView = rvMedia,
                swipeRefreshLayout = layoutMediaRefresh
            ) {
                fsMediaVertical.updateBubbleText(
                    getBubbleTextItem(
                        index = it,
                        sorting = sorting
                    )
                )
            }
        }
    }

    private fun getBubbleTextItem(index: Int, sorting: Int): String {
        var realIndex = index
        val mediaAdapter = getMediaAdapter()
        if (mediaAdapter?.isASectionTitle(index) == true) {
            realIndex++
        }
        return mediaAdapter?.getItemBubbleText(
            position = realIndex,
            sorting = sorting,
            dateFormat = mDateFormat,
            timeFormat = mTimeFormat
        ) ?: ""
    }

    private fun checkLastMediaChanged() {
        if (isDestroyed || config.getFolderSorting(mPath) and SORT_BY_RANDOM != 0) {
            return
        }

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            ensureBackgroundThread {
                val mediaId = getLatestMediaId()
                val mediaDateId = getLatestMediaByDateId()
                if (mLatestMediaId != mediaId || mLatestMediaDateId != mediaDateId) {
                    mLatestMediaId = mediaId
                    mLatestMediaDateId = mediaDateId
                    runOnUiThread {
                        getMedia()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(
            activity = this,
            isDirectorySorting = false,
            showFolderCheckbox = true,
            path = mPath
        ) {
            mLoadedInitialPhotos = false
            rvMedia.adapter = null
            getMedia()
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(this) {
            mLoadedInitialPhotos = false
            layoutMediaRefresh.isRefreshing = true
            rvMedia.adapter = null
            getMedia()
        }
    }

    private fun emptyRecycleBin() {
        showRecycleBinEmptyingDialog {
            emptyTheRecycleBin {
                finish()
            }
        }
    }

    private fun emptyAndDisableRecycleBin() {
        showRecycleBinEmptyingDialog {
            emptyAndDisableTheRecycleBin {
                finish()
            }
        }
    }

    private fun restoreAllFiles() {
        val paths = mMedia.filterIsInstance<Medium>().map { it.path } as ArrayList<String>
        restoreRecycleBinPaths(paths) {
            ensureBackgroundThread {
                directoryDao.deleteDirPath(RECYCLE_BIN)
            }
            finish()
        }
    }

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        getMediaAdapter()?.updateDisplayFilenames(config.displayFileNames)
    }

    private fun switchToFolderView() {
        config.showAll = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(activity = this, fromFoldersView = false, path = mPath) {
            invalidateOptionsMenu()
            setupLayoutManager()
            rvMedia.adapter = null
            setupAdapter()
        }
    }

    private fun showGroupByDialog() {
        ChangeGroupingDialog(activity = this, path = mPath) {
            mLoadedInitialPhotos = false
            rvMedia.adapter = null
            getMedia()
        }
    }

    private fun deleteDirectoryIfEmpty() {
        if (config.deleteEmptyFolders) {
            val fileDirItem = FileDirItem(mPath, mPath.getFilenameFromPath(), true)
            if (!fileDirItem.isDownloadsFolder() && fileDirItem.isDirectory) {
                ensureBackgroundThread {
                    if (fileDirItem.getProperFileCount(context = this, countHidden = true) == 0) {
                        tryDeleteFileDirItem(
                            fileDirItem = fileDirItem,
                            allowDeleteFolder = true,
                            deleteFromDatabase = true
                        )
                    }
                }
            }
        }
    }

    private fun getMedia() {
        if (mIsGettingMedia) {
            return
        }

        mIsGettingMedia = true
        if (mLoadedInitialPhotos) {
            startAsyncTask()
        } else {
            getCachedMedia(
                path = mPath,
                getVideosOnly = mIsGetVideoIntent,
                getImagesOnly = mIsGetImageIntent
            ) {
                if (it.isEmpty()) {
                    runOnUiThread {
                        layoutMediaRefresh.isRefreshing = true
                    }
                } else {
                    gotMedia(it, true)
                }
                startAsyncTask()
            }
        }

        mLoadedInitialPhotos = true
    }

    private fun startAsyncTask() {
        mCurrAsyncTask?.stopFetching()
        mCurrAsyncTask = GetMediaAsyncTask(
            context = applicationContext,
            mPath = mPath,
            isPickImage = mIsGetImageIntent,
            isPickVideo = mIsGetVideoIntent,
            showAll = mShowAll
        ) {
            ensureBackgroundThread {
                val oldMedia = mMedia.clone() as ArrayList<ThumbnailItem>
                val newMedia = it
                try {
                    gotMedia(newMedia, false)
                    oldMedia.filter { !newMedia.contains(it) }.mapNotNull { it as? Medium }
                        .filter { !getDoesFilePathExist(it.path) }.forEach {
                            mediaDB.deleteMediumPath(it.path)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        mCurrAsyncTask?.execute()
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0 && config.filterMedia > 0) {
            if (mPath != FAVORITES && mPath != RECYCLE_BIN) {
                deleteDirectoryIfEmpty()
                deleteDBDirectory()
            }

            if (mPath == FAVORITES) {
                ensureBackgroundThread {
                    directoryDao.deleteDirPath(FAVORITES)
                }
            }

            finish()
            true
        } else {
            false
        }
    }

    private fun deleteDBDirectory() {
        ensureBackgroundThread {
            try {
                directoryDao.deleteDirPath(mPath)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun createNewFolder() {
        CreateNewFolderDialog(activity = this, path = mPath) {
            config.tempFolderPath = it
        }
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        mLoadedInitialPhotos = false
        config.temporarilyShowHidden = show
        getMedia()
        invalidateOptionsMenu()
    }

    private fun setupLayoutManager() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = rvMedia.layoutManager as MyGridLayoutManager
        (rvMedia.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = 0
            bottomMargin = 0
        }

        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            layoutMediaRefresh.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            layoutMediaRefresh.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        layoutManager.spanCount = config.mediaColumnCnt
        val adapter = getMediaAdapter()
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isASectionTitle(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun measureRecyclerViewContent(media: ArrayList<ThumbnailItem>) {
        rvMedia.onGlobalLayout {
            if (config.scrollHorizontally) {
                calculateContentWidth(media)
            } else {
                calculateContentHeight(media)
            }
        }
    }

    private fun calculateContentWidth(media: ArrayList<ThumbnailItem>) {
        val layoutManager = rvMedia.layoutManager as MyGridLayoutManager
        val thumbnailWidth = layoutManager.getChildAt(0)?.width ?: 0
        val spacing = config.thumbnailSpacing
        val fullWidth =
            ((media.size - 1) / layoutManager.spanCount + 1) * (thumbnailWidth + spacing) - spacing
        fsMediaHorizontal.setContentWidth(fullWidth)
        fsMediaHorizontal.setScrollToX(rvMedia.computeHorizontalScrollOffset())
    }

    private fun calculateContentHeight(media: ArrayList<ThumbnailItem>) {
        val layoutManager = rvMedia.layoutManager as MyGridLayoutManager
        val pathToCheck = if (mPath.isEmpty()) SHOW_ALL else mPath
        val hasSections =
            config.getFolderGrouping(pathToCheck) and GROUP_BY_NONE == 0 && !config.scrollHorizontally
        val sectionTitleHeight = if (hasSections) layoutManager.getChildAt(0)?.height ?: 0 else 0
        val thumbnailHeight =
            if (hasSections) layoutManager.getChildAt(1)?.height ?: 0 else layoutManager.getChildAt(
                0
            )?.height ?: 0

        var fullHeight = 0
        var curSectionItems = 0
        media.forEach {
            if (it is ThumbnailSection) {
                fullHeight += sectionTitleHeight
                if (curSectionItems != 0) {
                    val rows = ((curSectionItems - 1) / layoutManager.spanCount + 1)
                    fullHeight += rows * thumbnailHeight
                }
                curSectionItems = 0
            } else {
                curSectionItems++
            }
        }

        val spacing = config.thumbnailSpacing
        fullHeight += ((curSectionItems - 1) / layoutManager.spanCount + 1) * (thumbnailHeight + spacing) - spacing
        fsMediaVertical.setContentHeight(fullHeight)
        fsMediaVertical.setScrollToY(rvMedia.computeVerticalScrollOffset())
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem> = mMedia) {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val useGridPosition = media.firstOrNull() is ThumbnailSection

            var currentGridDecoration: GridSpacingItemDecoration? = null
            if (rvMedia.itemDecorationCount > 0) {
                currentGridDecoration =
                    rvMedia.getItemDecorationAt(0) as GridSpacingItemDecoration
                currentGridDecoration.items = media
            }

            val newGridDecoration = GridSpacingItemDecoration(
                spanCount,
                spacing,
                config.scrollHorizontally,
                config.fileRoundedCorners,
                media,
                useGridPosition
            )
            if (currentGridDecoration.toString() != newGridDecoration.toString()) {
                if (currentGridDecoration != null) {
                    rvMedia.removeItemDecoration(currentGridDecoration)
                }
                rvMedia.addItemDecoration(newGridDecoration)
            }
        }
    }

    private fun initZoomListener() {
        val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
        if (viewType == VIEW_TYPE_GRID) {
            val layoutManager = rvMedia.layoutManager as MyGridLayoutManager
            mZoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getMediaAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        getMediaAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            mZoomListener = null
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = rvMedia.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutMediaRefresh.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val smallMargin = resources.getDimension(R.dimen.small_margin).toInt()
        (rvMedia.layoutParams as RelativeLayout.LayoutParams).apply {
            topMargin = smallMargin
            bottomMargin = smallMargin
        }

        mZoomListener = null
    }

    private fun increaseColumnCount() {
        config.mediaColumnCnt = ++(rvMedia.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun reduceColumnCount() {
        config.mediaColumnCnt = --(rvMedia.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun columnCountChanged() {
        handleGridSpacing()
        invalidateOptionsMenu()
        getMediaAdapter()?.apply {
            notifyItemRangeChanged(0, media.size)
            measureRecyclerViewContent(media)
        }
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(SET_WALLPAPER_INTENT, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mMedia.clear()
                refreshItems()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun itemClicked(path: String) {
        if (isSetWallpaperIntent()) {
            toast(R.string.setting_wallpaper)

            val wantedWidth = wallpaperDesiredMinimumWidth
            val wantedHeight = wallpaperDesiredMinimumHeight
            val ratio = wantedWidth.toFloat() / wantedHeight

            val options = RequestOptions()
                .override((wantedWidth * ratio).toInt(), wantedHeight)
                .fitCenter()

            Glide.with(this)
                .asBitmap()
                .load(File(path))
                .apply(options)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        try {
                            WallpaperManager.getInstance(applicationContext).setBitmap(resource)
                            setResult(Activity.RESULT_OK)
                        } catch (ignored: IOException) {
                        }

                        finish()
                    }
                })
        } else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
            Intent().apply {
                data = Uri.parse(path)
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        } else {
            val isVideo = path.isVideoFast()
            if (isVideo) {
                val extras = HashMap<String, Boolean>()
                extras[SHOW_FAVORITES] = mPath == FAVORITES

                if (shouldSkipAuthentication()) {
                    extras[SKIP_AUTHENTICATION] = true
                }
                openPath(path, false, extras)
            } else {
                Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(SKIP_AUTHENTICATION, shouldSkipAuthentication())
                    putExtra(PATH, path)
                    putExtra(SHOW_ALL, mShowAll)
                    putExtra(SHOW_FAVORITES, mPath == FAVORITES)
                    putExtra(SHOW_RECYCLE_BIN, mPath == RECYCLE_BIN)
                    startActivity(this)
                }
            }
        }
    }

    private fun gotMedia(media: ArrayList<ThumbnailItem>, isFromCache: Boolean) {
        mIsGettingMedia = false
        checkLastMediaChanged()
        mMedia = media

        runOnUiThread {
            layoutMediaRefresh.isRefreshing = false
            tvMediaEmpty.beVisibleIf(media.isEmpty() && !isFromCache)
            tvMediaEmpty2.beVisibleIf(media.isEmpty() && !isFromCache)

            if (tvMediaEmpty.isVisible()) {
                tvMediaEmpty.text = getString(R.string.no_media_with_filters)
            }
            rvMedia.beVisibleIf(tvMediaEmpty.isGone())

            val viewType = config.getFolderViewType(if (mShowAll) SHOW_ALL else mPath)
            val allowHorizontalScroll = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
            fsMediaVertical.beVisibleIf(rvMedia.isVisible() && !allowHorizontalScroll)
            fsMediaHorizontal.beVisibleIf(rvMedia.isVisible() && allowHorizontalScroll)
            setupAdapter()
        }

        mLatestMediaId = getLatestMediaId()
        mLatestMediaDateId = getLatestMediaByDateId()
        if (!isFromCache) {
            val mediaToInsert =
                (mMedia).filter { it is Medium && it.deletedTS == 0L }.map { it as Medium }
            Thread {
                try {
                    mediaDB.insertAll(mediaToInsert)
                } catch (e: Exception) {
                }
            }.start()
        }
    }

    override fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>) {
        val filtered =
            fileDirItems.filter { !getIsPathDirectory(it.path) && it.path.isMediaFile() } as ArrayList
        if (filtered.isEmpty()) {
            return
        }

        if (config.useRecycleBin && !filtered.first().path.startsWith(recycleBinPath)) {
            val movingItems = resources.getQuantityString(
                R.plurals.moving_items_into_bin,
                filtered.size,
                filtered.size
            )
            toast(movingItems)

            movePathsInRecycleBin(filtered.map { it.path } as ArrayList<String>) {
                if (it) {
                    deleteFilteredFiles(filtered)
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            val deletingItems =
                resources.getQuantityString(R.plurals.deleting_items, filtered.size, filtered.size)
            toast(deletingItems)
            deleteFilteredFiles(filtered)
        }
    }

    private fun shouldSkipAuthentication() = intent.getBooleanExtra(SKIP_AUTHENTICATION, false)

    private fun deleteFilteredFiles(filtered: ArrayList<FileDirItem>) {
        deleteFiles(filtered) {
            if (!it) {
                toast(R.string.unknown_error_occurred)
                return@deleteFiles
            }

            mMedia.removeAll { filtered.map { it.path }.contains((it as? Medium)?.path) }

            ensureBackgroundThread {
                val useRecycleBin = config.useRecycleBin
                filtered.forEach {
                    if (it.path.startsWith(recycleBinPath) || !useRecycleBin) {
                        deleteDBPath(it.path)
                    }
                }
            }

            if (mMedia.isEmpty()) {
                deleteDirectoryIfEmpty()
                deleteDBDirectory()
                finish()
            }
        }
    }

    override fun refreshItems() {
        getMedia()
    }

    override fun selectedPaths(paths: ArrayList<String>) {
        Intent().apply {
            putExtra(PICKED_PATHS, paths)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    override fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>) {
        var currentGridPosition = 0
        media.forEach {
            if (it is Medium) {
                it.gridPosition = currentGridPosition++
            } else if (it is ThumbnailSection) {
                currentGridPosition = 0
            }
        }

        if (rvMedia.itemDecorationCount > 0) {
            val currentGridDecoration =
                rvMedia.getItemDecorationAt(0) as GridSpacingItemDecoration
            currentGridDecoration.items = media
        }
    }

    private fun setAsDefaultFolder() {
        config.defaultFolder = mPath
        invalidateOptionsMenu()
    }

    private fun unsetAsDefaultFolder() {
        config.defaultFolder = ""
        invalidateOptionsMenu()
    }
}
