package com.loitp.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loitp.pro.R
import com.loitp.ui.dialog.ChangeFileThumbnailStyleDialog
import com.loitp.ui.dialog.ChangeFolderThumbnailStyleDialog
import com.loitp.ui.dialog.ManageBottomActionsDialog
import com.loitp.ui.dialog.ManageExtendedDetailsDialog
import com.loitp.ext.config
import com.loitp.ext.emptyTheRecycleBin
import com.loitp.ext.mediaDB
import com.loitp.ext.showRecycleBinEmptyingDialog
import com.loitp.pro.helpers.*
import com.loitp.model.AlbumCover
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    companion object {
        private const val PICK_IMPORT_SOURCE_INTENT = 1
    }

    private var mRecycleBinContentSize = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()
        setupSettingItems()
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupUseEnglish()
        setupChangeDateTimeFormat()
        setupFileLoadingPriority()
        setupManageIncludedFolders()
        setupManageExcludedFolders()
        setupManageHiddenFolders()
        setupShowHiddenItems()
        setupAutoplayVideos()
        setupRememberLastVideo()
        setupLoopVideos()
        setupOpenVideosOnSeparateScreen()
        setupMaxBrightness()
        setupCropThumbnails()
        setupDarkBackground()
        setupScrollHorizontally()
        setupScreenRotation()
        setupHideSystemUI()
        setupHiddenItemPasswordProtection()
        setupAppPasswordProtection()
        setupFileDeletionPasswordProtection()
        setupDeleteEmptyFolders()
        setupAllowPhotoGestures()
        setupAllowDownGesture()
        setupAllowRotatingWithGestures()
        setupShowNotch()
        setupBottomActions()
        setupFileThumbnailStyle()
        setupFolderThumbnailStyle()
        setupKeepLastModified()
        setupEnablePullToRefresh()
        setupAllowZoomingImages()
        setupShowHighestQuality()
        setupAllowOneToOneZoom()
        setupAllowInstantChange()
        setupShowExtendedDetails()
        setupHideExtendedDetails()
        setupManageExtendedDetails()
        setupSkipDeleteConfirmation()
        setupManageBottomActions()
        setupUseRecycleBin()
        setupShowRecycleBin()
        setupShowRecycleBinLast()
        setupEmptyRecycleBin()
        updateTextColors(layoutSettingsHolder)
        setupSectionColors()
        setupClearCache()
        setupExportSettings()
        setupImportSettings()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        }
    }

    private fun setupSectionColors() {
        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        arrayListOf(
            tvVisibilityLabel,
            tvVideosLabel,
            tvThumbnailsLabel,
            tvScrollingLabel,
            tvFullScreenMediaLabel,
            tvSecurityLabel,
            tvFileOperationsLabel,
            tvDeepZoomableImagesLabel,
            tvExtendedDetailsLabel,
            tvBottomActionsLabel,
            tvRecyclebinLabel,
            tvMigratingLabel
        ).forEach {
            it.setTextColor(adjustedPrimaryColor)
        }
    }

    private fun setupCustomizeColors() {
        layoutSettingsCustomizeColorsHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        layoutSettingsUseEnglishHolder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        swSettingsUseEnglish.isChecked = config.useEnglish
        layoutSettingsUseEnglishHolder.setOnClickListener {
            swSettingsUseEnglish.toggle()
            config.useEnglish = swSettingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupChangeDateTimeFormat() {
        layoutSettingsChangeDateTimeFormat.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }
    }

    private fun setupFileLoadingPriority() {
        tvSettingsFileLoadingPriority.text = getFileLoadingPriorityText()
        layoutSettingsFileLoadingPriority.setOnClickListener {
            val items = arrayListOf(
                RadioItem(PRIORITY_SPEED, getString(R.string.speed)),
                RadioItem(PRIORITY_COMPROMISE, getString(R.string.compromise)),
                RadioItem(PRIORITY_VALIDITY, getString(R.string.avoid_showing_invalid_files))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fileLoadingPriority) {
                config.fileLoadingPriority = it as Int
                tvSettingsFileLoadingPriority.text = getFileLoadingPriorityText()
            }
        }
    }

    private fun getFileLoadingPriorityText() = getString(
        when (config.fileLoadingPriority) {
            PRIORITY_SPEED -> R.string.speed
            PRIORITY_COMPROMISE -> R.string.compromise
            else -> R.string.avoid_showing_invalid_files
        }
    )

    private fun setupManageIncludedFolders() {
        layoutSettingsManageIncludedFolders.setOnClickListener {
            startActivity(Intent(this, IncludedFoldersActivity::class.java))
        }
    }

    private fun setupManageExcludedFolders() {
        layoutSettingsManageExcludedFolders.setOnClickListener {
            startActivity(Intent(this, ExcludedFoldersActivity::class.java))
        }
    }

    private fun setupManageHiddenFolders() {
        layoutSettingsManageHiddenFolders.beVisibleIf(!isQPlus())
        layoutSettingsManageHiddenFolders.setOnClickListener {
            handleHiddenFolderPasswordProtection {
                startActivity(Intent(this, HiddenFoldersActivity::class.java))
            }
        }
    }

    private fun setupShowHiddenItems() {
        swSettingsShowHiddenItems.isChecked = config.showHiddenMedia
        layoutSettingsShowHiddenItems.setOnClickListener {
            if (config.showHiddenMedia) {
                toggleHiddenItems()
            } else {
                handleHiddenFolderPasswordProtection {
                    toggleHiddenItems()
                }
            }
        }
    }

    private fun toggleHiddenItems() {
        swSettingsShowHiddenItems.toggle()
        config.showHiddenMedia = swSettingsShowHiddenItems.isChecked
    }

    private fun setupAutoplayVideos() {
        swSettingsAutoplayVideos.isChecked = config.autoplayVideos
        layoutSettingsAutoplayVideos.setOnClickListener {
            swSettingsAutoplayVideos.toggle()
            config.autoplayVideos = swSettingsAutoplayVideos.isChecked
        }
    }

    private fun setupRememberLastVideo() {
        swSettingsRememberLastVideoPosition.isChecked = config.rememberLastVideoPosition
        layoutSettingsRememberLastVideoPosition.setOnClickListener {
            swSettingsRememberLastVideoPosition.toggle()
            config.rememberLastVideoPosition = swSettingsRememberLastVideoPosition.isChecked
        }
    }

    private fun setupLoopVideos() {
        swSettingsLoopVideos.isChecked = config.loopVideos
        layoutSettingsLoopVideos.setOnClickListener {
            swSettingsLoopVideos.toggle()
            config.loopVideos = swSettingsLoopVideos.isChecked
        }
    }

    private fun setupOpenVideosOnSeparateScreen() {
        swSettingsOpenVideosOnSeparateScreen.isChecked = config.openVideosOnSeparateScreen
        layoutSettingsOpenVideosOnSeparateScreen.setOnClickListener {
            swSettingsOpenVideosOnSeparateScreen.toggle()
            config.openVideosOnSeparateScreen = swSettingsOpenVideosOnSeparateScreen.isChecked
        }
    }

    private fun setupMaxBrightness() {
        swSettingsMaxBrightness.isChecked = config.maxBrightness
        layoutSettingsMaxBrightness.setOnClickListener {
            swSettingsMaxBrightness.toggle()
            config.maxBrightness = swSettingsMaxBrightness.isChecked
        }
    }

    private fun setupCropThumbnails() {
        swSettingsCropThumbnails.isChecked = config.cropThumbnails
        layoutSettingsCropThumbnailsHolder.setOnClickListener {
            swSettingsCropThumbnails.toggle()
            config.cropThumbnails = swSettingsCropThumbnails.isChecked
        }
    }

    private fun setupDarkBackground() {
        swSettingsBlackBackground.isChecked = config.blackBackground
        layoutSettingsBlackBackground.setOnClickListener {
            swSettingsBlackBackground.toggle()
            config.blackBackground = swSettingsBlackBackground.isChecked
        }
    }

    private fun setupScrollHorizontally() {
        swSettingsScrollHorizontally.isChecked = config.scrollHorizontally
        layoutSettingsScrollHorizontally.setOnClickListener {
            swSettingsScrollHorizontally.toggle()
            config.scrollHorizontally = swSettingsScrollHorizontally.isChecked

            if (config.scrollHorizontally) {
                config.enablePullToRefresh = false
                swSettingsEnablePullToRefresh.isChecked = false
            }
        }
    }

    private fun setupHideSystemUI() {
        swSettingsHideSystemUi.isChecked = config.hideSystemUI
        layoutSettingsHideSystemUi.setOnClickListener {
            swSettingsHideSystemUi.toggle()
            config.hideSystemUI = swSettingsHideSystemUi.isChecked
        }
    }

    private fun setupHiddenItemPasswordProtection() {
        swSettingsHiddenItemPasswordProtection.isChecked = config.isHiddenPasswordProtectionOn
        layoutSettingsHiddenItemPasswordProtection.setOnClickListener {
            val tabToShow =
                if (config.isHiddenPasswordProtectionOn) config.hiddenProtectionType else SHOW_ALL_TABS
            SecurityDialog(
                activity = this,
                requiredHash = config.hiddenPasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isHiddenPasswordProtectionOn
                    swSettingsHiddenItemPasswordProtection.isChecked = !hasPasswordProtection
                    config.isHiddenPasswordProtectionOn = !hasPasswordProtection
                    config.hiddenPasswordHash = if (hasPasswordProtection) "" else hash
                    config.hiddenProtectionType = type

                    if (config.isHiddenPasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.hiddenProtectionType == PROTECTION_FINGERPRINT)
                                R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(
                            activity = this,
                            message = "",
                            messageId = confirmationTextId,
                            positive = R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }

    private fun setupAppPasswordProtection() {
        swSettingsAppPasswordProtection.isChecked = config.isAppPasswordProtectionOn
        layoutSettingsAppPasswordProtection.setOnClickListener {
            val tabToShow =
                if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(
                activity = this,
                requiredHash = config.appPasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    swSettingsAppPasswordProtection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.appProtectionType == PROTECTION_FINGERPRINT)
                                R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(
                            activity = this,
                            message = "",
                            messageId = confirmationTextId,
                            positive = R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }

    private fun setupFileDeletionPasswordProtection() {
        swSettingsFileDeletionPasswordProtection.isChecked = config.isDeletePasswordProtectionOn
        layoutSettingsFileDeletionPasswordProtection.setOnClickListener {
            val tabToShow =
                if (config.isDeletePasswordProtectionOn) config.deleteProtectionType else SHOW_ALL_TABS
            SecurityDialog(
                activity = this,
                requiredHash = config.deletePasswordHash,
                showTabIndex = tabToShow
            ) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isDeletePasswordProtectionOn
                    swSettingsFileDeletionPasswordProtection.isChecked = !hasPasswordProtection
                    config.isDeletePasswordProtectionOn = !hasPasswordProtection
                    config.deletePasswordHash = if (hasPasswordProtection) "" else hash
                    config.deleteProtectionType = type

                    if (config.isDeletePasswordProtectionOn) {
                        val confirmationTextId =
                            if (config.deleteProtectionType == PROTECTION_FINGERPRINT)
                                R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(
                            activity = this,
                            message = "",
                            messageId = confirmationTextId,
                            positive = R.string.ok,
                            negative = 0
                        ) { }
                    }
                }
            }
        }
    }

    private fun setupDeleteEmptyFolders() {
        swSettingsDeleteEmptyFolders.isChecked = config.deleteEmptyFolders
        layoutSettingsDeleteEmptyFolders.setOnClickListener {
            swSettingsDeleteEmptyFolders.toggle()
            config.deleteEmptyFolders = swSettingsDeleteEmptyFolders.isChecked
        }
    }

    private fun setupAllowPhotoGestures() {
        swSettingsAllowPhotoGestures.isChecked = config.allowPhotoGestures
        layoutSettingsAllowPhotoGestures.setOnClickListener {
            swSettingsAllowPhotoGestures.toggle()
            config.allowPhotoGestures = swSettingsAllowPhotoGestures.isChecked
        }
    }

    private fun setupAllowDownGesture() {
        swSettingsAllowDownGesture.isChecked = config.allowDownGesture
        layoutSettingsAllowDownGesture.setOnClickListener {
            swSettingsAllowDownGesture.toggle()
            config.allowDownGesture = swSettingsAllowDownGesture.isChecked
        }
    }

    private fun setupAllowRotatingWithGestures() {
        swSettingsAllowRotatingWithGestures.isChecked = config.allowRotatingWithGestures
        layoutSettingsAllowRotatingWithGestures.setOnClickListener {
            swSettingsAllowRotatingWithGestures.toggle()
            config.allowRotatingWithGestures = swSettingsAllowRotatingWithGestures.isChecked
        }
    }

    private fun setupShowNotch() {
        layoutSettingsShowNotch.beVisibleIf(isPiePlus())
        swSettingsShowNotch.isChecked = config.showNotch
        layoutSettingsShowNotch.setOnClickListener {
            swSettingsShowNotch.toggle()
            config.showNotch = swSettingsShowNotch.isChecked
        }
    }

    private fun setupFileThumbnailStyle() {
        layoutSettingsFileThumbnailStyle.setOnClickListener {
            ChangeFileThumbnailStyleDialog(this)
        }
    }

    private fun setupFolderThumbnailStyle() {
        tvSettingsFolderThumbnailStyle.text = getFolderStyleText()
        layoutSettingsFolderThumbnailStyle.setOnClickListener {
            ChangeFolderThumbnailStyleDialog(this) {
                tvSettingsFolderThumbnailStyle.text = getFolderStyleText()
            }
        }
    }

    private fun getFolderStyleText() = getString(
        when (config.folderStyle) {
            FOLDER_STYLE_SQUARE -> R.string.square
            else -> R.string.rounded_corners
        }
    )

    private fun setupKeepLastModified() {
        swSettingsKeepLastModified.isChecked = config.keepLastModified
        layoutSettingsKeepLastModified.setOnClickListener {
            swSettingsKeepLastModified.toggle()
            config.keepLastModified = swSettingsKeepLastModified.isChecked
        }
    }

    private fun setupEnablePullToRefresh() {
        swSettingsEnablePullToRefresh.isChecked = config.enablePullToRefresh
        layoutSettingsEnablePullToRefresh.setOnClickListener {
            swSettingsEnablePullToRefresh.toggle()
            config.enablePullToRefresh = swSettingsEnablePullToRefresh.isChecked
        }
    }

    private fun setupAllowZoomingImages() {
        swSettingsAllowZoomingImages.isChecked = config.allowZoomingImages
        updateDeepZoomToggleButtons()
        layoutSettingsAllowZoomingImages.setOnClickListener {
            swSettingsAllowZoomingImages.toggle()
            config.allowZoomingImages = swSettingsAllowZoomingImages.isChecked
            updateDeepZoomToggleButtons()
        }
    }

    private fun updateDeepZoomToggleButtons() {
        layoutSettingsAllowRotatingWithGestures.beVisibleIf(config.allowZoomingImages)
        layoutSettingsShowHighestQuality.beVisibleIf(config.allowZoomingImages)
        layoutSettingsAllowOneToOneZoom.beVisibleIf(config.allowZoomingImages)
    }

    private fun setupShowHighestQuality() {
        swSettingsShowHighestQuality.isChecked = config.showHighestQuality
        layoutSettingsShowHighestQuality.setOnClickListener {
            swSettingsShowHighestQuality.toggle()
            config.showHighestQuality = swSettingsShowHighestQuality.isChecked
        }
    }

    private fun setupAllowOneToOneZoom() {
        swSettingsAllowOneTOneZoom.isChecked = config.allowOneToOneZoom
        layoutSettingsAllowOneToOneZoom.setOnClickListener {
            swSettingsAllowOneTOneZoom.toggle()
            config.allowOneToOneZoom = swSettingsAllowOneTOneZoom.isChecked
        }
    }

    private fun setupAllowInstantChange() {
        swSettingsAllowInstantChange.isChecked = config.allowInstantChange
        layoutSettingsAllowInstantChange.setOnClickListener {
            swSettingsAllowInstantChange.toggle()
            config.allowInstantChange = swSettingsAllowInstantChange.isChecked
        }
    }

    private fun setupShowExtendedDetails() {
        swSettingsShowExtendedDetails.isChecked = config.showExtendedDetails
        layoutSettingsShowExtendedDetails.setOnClickListener {
            swSettingsShowExtendedDetails.toggle()
            config.showExtendedDetails = swSettingsShowExtendedDetails.isChecked
            layoutSettingsManageExtendedDetails.beVisibleIf(config.showExtendedDetails)
            layoutSettingsHideExtendedDetails.beVisibleIf(config.showExtendedDetails)
        }
    }

    private fun setupHideExtendedDetails() {
        layoutSettingsHideExtendedDetails.beVisibleIf(config.showExtendedDetails)
        swSettingsHideExtendedDetails.isChecked = config.hideExtendedDetails
        layoutSettingsHideExtendedDetails.setOnClickListener {
            swSettingsHideExtendedDetails.toggle()
            config.hideExtendedDetails = swSettingsHideExtendedDetails.isChecked
        }
    }

    private fun setupManageExtendedDetails() {
        layoutSettingsManageExtendedDetails.beVisibleIf(config.showExtendedDetails)
        layoutSettingsManageExtendedDetails.setOnClickListener {
            ManageExtendedDetailsDialog(this) {
                if (config.extendedDetails == 0) {
                    layoutSettingsShowExtendedDetails.callOnClick()
                }
            }
        }
    }

    private fun setupSkipDeleteConfirmation() {
        swSettingsSkipDeleteConfirmation.isChecked = config.skipDeleteConfirmation
        layoutSettingsSkipDeleteConfirmation.setOnClickListener {
            swSettingsSkipDeleteConfirmation.toggle()
            config.skipDeleteConfirmation = swSettingsSkipDeleteConfirmation.isChecked
        }
    }

    private fun setupScreenRotation() {
        tvSettingsScreenRotation.text = getScreenRotationText()
        layoutSettingsScreenRotation.setOnClickListener {
            val items = arrayListOf(
                RadioItem(
                    id = ROTATE_BY_SYSTEM_SETTING,
                    title = getString(R.string.screen_rotation_system_setting)
                ),
                RadioItem(
                    id = ROTATE_BY_DEVICE_ROTATION,
                    title = getString(R.string.screen_rotation_device_rotation)
                ),
                RadioItem(ROTATE_BY_ASPECT_RATIO, getString(R.string.screen_rotation_aspect_ratio))
            )

            RadioGroupDialog(
                activity = this@SettingsActivity,
                items = items,
                checkedItemId = config.screenRotation
            ) {
                config.screenRotation = it as Int
                tvSettingsScreenRotation.text = getScreenRotationText()
            }
        }
    }

    private fun getScreenRotationText() = getString(
        when (config.screenRotation) {
            ROTATE_BY_SYSTEM_SETTING -> R.string.screen_rotation_system_setting
            ROTATE_BY_DEVICE_ROTATION -> R.string.screen_rotation_device_rotation
            else -> R.string.screen_rotation_aspect_ratio
        }
    )

    private fun setupBottomActions() {
        swSettingsBottomActions.isChecked = config.bottomActions
        layoutSettingsBottomActions.setOnClickListener {
            swSettingsBottomActions.toggle()
            config.bottomActions = swSettingsBottomActions.isChecked
            layoutSettingsManageBottomActions.beVisibleIf(config.bottomActions)
        }
    }

    private fun setupManageBottomActions() {
        layoutSettingsManageBottomActions.beVisibleIf(config.bottomActions)
        layoutSettingsManageBottomActions.setOnClickListener {
            ManageBottomActionsDialog(this) {
                if (config.visibleBottomActions == 0) {
                    layoutSettingsBottomActions.callOnClick()
                    config.bottomActions = false
                    config.visibleBottomActions = DEFAULT_BOTTOM_ACTIONS
                }
            }
        }
    }

    private fun setupUseRecycleBin() {
        layoutSettingsEmptyRecycleBin.beVisibleIf(config.useRecycleBin)
        layoutSettingsShowRecycleBin.beVisibleIf(config.useRecycleBin)
        layoutSettingsShowRecycleBinLast.beVisibleIf(config.useRecycleBin && config.showRecycleBinAtFolders)
        swSettingsUseRecycleBin.isChecked = config.useRecycleBin
        layoutSettingsUseRecyclebin.setOnClickListener {
            swSettingsUseRecycleBin.toggle()
            config.useRecycleBin = swSettingsUseRecycleBin.isChecked
            layoutSettingsEmptyRecycleBin.beVisibleIf(config.useRecycleBin)
            layoutSettingsShowRecycleBin.beVisibleIf(config.useRecycleBin)
            layoutSettingsShowRecycleBinLast.beVisibleIf(config.useRecycleBin && config.showRecycleBinAtFolders)
        }
    }

    private fun setupShowRecycleBin() {
        swSettingsShowRecycleBin.isChecked = config.showRecycleBinAtFolders
        layoutSettingsShowRecycleBin.setOnClickListener {
            swSettingsShowRecycleBin.toggle()
            config.showRecycleBinAtFolders = swSettingsShowRecycleBin.isChecked
            layoutSettingsShowRecycleBinLast.beVisibleIf(config.useRecycleBin && config.showRecycleBinAtFolders)
        }
    }

    private fun setupShowRecycleBinLast() {
        swSettingsShowRecycleBinLast.isChecked = config.showRecycleBinLast
        layoutSettingsShowRecycleBinLast.setOnClickListener {
            swSettingsShowRecycleBinLast.toggle()
            config.showRecycleBinLast = swSettingsShowRecycleBinLast.isChecked
            if (config.showRecycleBinLast) {
                config.removePinnedFolders(setOf(RECYCLE_BIN))
            }
        }
    }

    private fun setupEmptyRecycleBin() {
        ensureBackgroundThread {
            try {
                mRecycleBinContentSize = mediaDB.getDeletedMedia().sumByLong { it.size }
            } catch (ignored: Exception) {
            }
            runOnUiThread {
                tvSettingsEmptyRecycleBinSize.text = mRecycleBinContentSize.formatSize()
            }
        }

        layoutSettingsEmptyRecycleBin.setOnClickListener {
            if (mRecycleBinContentSize == 0L) {
                toast(R.string.recycle_bin_empty)
            } else {
                showRecycleBinEmptyingDialog {
                    emptyTheRecycleBin()
                    mRecycleBinContentSize = 0L
                    tvSettingsEmptyRecycleBinSize.text = 0L.formatSize()
                }
            }
        }
    }

    private fun setupClearCache() {
        ensureBackgroundThread {
            runOnUiThread {
                tvSettingsClearCacheSize.text = cacheDir.getProperSize(true).formatSize()
            }
        }

        layoutSettingsClearCache.setOnClickListener {
            ensureBackgroundThread {
                cacheDir.deleteRecursively()
                runOnUiThread {
                    tvSettingsClearCacheSize.text = cacheDir.getProperSize(true).formatSize()
                }
            }
        }
    }

    private fun setupExportSettings() {
        layoutSettingsExport.setOnClickListener {
            val configItems = LinkedHashMap<String, Any>().apply {
                put(IS_USING_SHARED_THEME, config.isUsingSharedTheme)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(APP_ICON_COLOR, config.appIconColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(DATE_FORMAT, config.dateFormat)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(INCLUDED_FOLDERS, TextUtils.join(",", config.includedFolders))
                put(EXCLUDED_FOLDERS, TextUtils.join(",", config.excludedFolders))
                put(SHOW_HIDDEN_MEDIA, config.showHiddenMedia)
                put(FILE_LOADING_PRIORITY, config.fileLoadingPriority)
                put(AUTOPLAY_VIDEOS, config.autoplayVideos)
                put(REMEMBER_LAST_VIDEO_POSITION, config.rememberLastVideoPosition)
                put(LOOP_VIDEOS, config.loopVideos)
                put(OPEN_VIDEOS_ON_SEPARATE_SCREEN, config.openVideosOnSeparateScreen)
                put(ALLOW_VIDEO_GESTURES, config.allowVideoGestures)
                put(ANIMATE_GIFS, config.animateGifs)
                put(CROP_THUMBNAILS, config.cropThumbnails)
                put(SHOW_THUMBNAIL_VIDEO_DURATION, config.showThumbnailVideoDuration)
                put(SHOW_THUMBNAIL_FILE_TYPES, config.showThumbnailFileTypes)
                put(SCROLL_HORIZONTALLY, config.scrollHorizontally)
                put(ENABLE_PULL_TO_REFRESH, config.enablePullToRefresh)
                put(MAX_BRIGHTNESS, config.maxBrightness)
                put(BLACK_BACKGROUND, config.blackBackground)
                put(HIDE_SYSTEM_UI, config.hideSystemUI)
                put(ALLOW_INSTANT_CHANGE, config.allowInstantChange)
                put(ALLOW_PHOTO_GESTURES, config.allowPhotoGestures)
                put(ALLOW_DOWN_GESTURE, config.allowDownGesture)
                put(ALLOW_ROTATING_WITH_GESTURES, config.allowRotatingWithGestures)
                put(SHOW_NOTCH, config.showNotch)
                put(SCREEN_ROTATION, config.screenRotation)
                put(ALLOW_ZOOMING_IMAGES, config.allowZoomingImages)
                put(SHOW_HIGHEST_QUALITY, config.showHighestQuality)
                put(ALLOW_ONE_TO_ONE_ZOOM, config.allowOneToOneZoom)
                put(SHOW_EXTENDED_DETAILS, config.showExtendedDetails)
                put(HIDE_EXTENDED_DETAILS, config.hideExtendedDetails)
                put(EXTENDED_DETAILS, config.extendedDetails)
                put(DELETE_EMPTY_FOLDERS, config.deleteEmptyFolders)
                put(KEEP_LAST_MODIFIED, config.keepLastModified)
                put(SKIP_DELETE_CONFIRMATION, config.skipDeleteConfirmation)
                put(BOTTOM_ACTIONS, config.bottomActions)
                put(VISIBLE_BOTTOM_ACTIONS, config.visibleBottomActions)
                put(USE_RECYCLE_BIN, config.useRecycleBin)
                put(SHOW_RECYCLE_BIN_AT_FOLDERS, config.showRecycleBinAtFolders)
                put(SHOW_RECYCLE_BIN_LAST, config.showRecycleBinLast)
                put(SORT_ORDER, config.sorting)
                put(DIRECTORY_SORT_ORDER, config.directorySorting)
                put(GROUP_BY, config.groupBy)
                put(GROUP_DIRECT_SUBFOLDERS, config.groupDirectSubfolders)
                put(PINNED_FOLDERS, TextUtils.join(",", config.pinnedFolders))
                put(DISPLAY_FILE_NAMES, config.displayFileNames)
                put(FILTER_MEDIA, config.filterMedia)
                put(DIR_COLUMN_CNT, config.dirColumnCnt)
                put(MEDIA_COLUMN_CNT, config.mediaColumnCnt)
                put(SHOW_ALL, config.showAll)
                put(SHOW_WIDGET_FOLDER_NAME, config.showWidgetFolderName)
                put(VIEW_TYPE_FILES, config.viewTypeFiles)
                put(VIEW_TYPE_FOLDERS, config.viewTypeFolders)
                put(SLIDESHOW_INTERVAL, config.slideshowInterval)
                put(SLIDESHOW_INCLUDE_VIDEOS, config.slideshowIncludeVideos)
                put(SLIDESHOW_INCLUDE_GIFS, config.slideshowIncludeGIFs)
                put(SLIDESHOW_RANDOM_ORDER, config.slideshowRandomOrder)
                put(SLIDESHOW_MOVE_BACKWARDS, config.slideshowMoveBackwards)
                put(SLIDESHOW_LOOP, config.loopSlideshow)
                put(LAST_EDITOR_CROP_ASPECT_RATIO, config.lastEditorCropAspectRatio)
                put(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X, config.lastEditorCropOtherAspectRatioX)
                put(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y, config.lastEditorCropOtherAspectRatioY)
                put(LAST_CONFLICT_RESOLUTION, config.lastConflictResolution)
                put(LAST_CONFLICT_APPLY_TO_ALL, config.lastConflictApplyToAll)
                put(EDITOR_BRUSH_COLOR, config.editorBrushColor)
                put(EDITOR_BRUSH_HARDNESS, config.editorBrushHardness)
                put(EDITOR_BRUSH_SIZE, config.editorBrushSize)
                put(ALBUM_COVERS, config.albumCovers)
                put(FOLDER_THUMBNAIL_STYLE, config.folderStyle)
                put(FOLDER_MEDIA_COUNT, config.showFolderMediaCount)
                put(LIMIT_FOLDER_TITLE, config.limitFolderTitle)
                put(THUMBNAIL_SPACING, config.thumbnailSpacing)
                put(FILE_ROUNDED_CORNERS, config.fileRoundedCorners)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        layoutSettingsImport.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                parseFile(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                APP_ICON_COLOR -> {
                    if (getAppIconColors().contains(value.toInt())) {
                        config.appIconColor = value.toInt()
                        checkAppIconColor()
                    }
                }
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                DATE_FORMAT -> config.dateFormat = value.toString()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                INCLUDED_FOLDERS -> config.addIncludedFolders(value.toStringSet())
                EXCLUDED_FOLDERS -> config.addExcludedFolders(value.toStringSet())
                SHOW_HIDDEN_MEDIA -> config.showHiddenMedia = value.toBoolean()
                FILE_LOADING_PRIORITY -> config.fileLoadingPriority = value.toInt()
                AUTOPLAY_VIDEOS -> config.autoplayVideos = value.toBoolean()
                REMEMBER_LAST_VIDEO_POSITION -> config.rememberLastVideoPosition = value.toBoolean()
                LOOP_VIDEOS -> config.loopVideos = value.toBoolean()
                OPEN_VIDEOS_ON_SEPARATE_SCREEN -> config.openVideosOnSeparateScreen =
                    value.toBoolean()
                ALLOW_VIDEO_GESTURES -> config.allowVideoGestures = value.toBoolean()
                ANIMATE_GIFS -> config.animateGifs = value.toBoolean()
                CROP_THUMBNAILS -> config.cropThumbnails = value.toBoolean()
                SHOW_THUMBNAIL_VIDEO_DURATION -> config.showThumbnailVideoDuration =
                    value.toBoolean()
                SHOW_THUMBNAIL_FILE_TYPES -> config.showThumbnailFileTypes = value.toBoolean()
                SCROLL_HORIZONTALLY -> config.scrollHorizontally = value.toBoolean()
                ENABLE_PULL_TO_REFRESH -> config.enablePullToRefresh = value.toBoolean()
                MAX_BRIGHTNESS -> config.maxBrightness = value.toBoolean()
                BLACK_BACKGROUND -> config.blackBackground = value.toBoolean()
                HIDE_SYSTEM_UI -> config.hideSystemUI = value.toBoolean()
                ALLOW_INSTANT_CHANGE -> config.allowInstantChange = value.toBoolean()
                ALLOW_PHOTO_GESTURES -> config.allowPhotoGestures = value.toBoolean()
                ALLOW_DOWN_GESTURE -> config.allowDownGesture = value.toBoolean()
                ALLOW_ROTATING_WITH_GESTURES -> config.allowRotatingWithGestures = value.toBoolean()
                SHOW_NOTCH -> config.showNotch = value.toBoolean()
                SCREEN_ROTATION -> config.screenRotation = value.toInt()
                ALLOW_ZOOMING_IMAGES -> config.allowZoomingImages = value.toBoolean()
                SHOW_HIGHEST_QUALITY -> config.showHighestQuality = value.toBoolean()
                ALLOW_ONE_TO_ONE_ZOOM -> config.allowOneToOneZoom = value.toBoolean()
                SHOW_EXTENDED_DETAILS -> config.showExtendedDetails = value.toBoolean()
                HIDE_EXTENDED_DETAILS -> config.hideExtendedDetails = value.toBoolean()
                EXTENDED_DETAILS -> config.extendedDetails = value.toInt()
                DELETE_EMPTY_FOLDERS -> config.deleteEmptyFolders = value.toBoolean()
                KEEP_LAST_MODIFIED -> config.keepLastModified = value.toBoolean()
                SKIP_DELETE_CONFIRMATION -> config.skipDeleteConfirmation = value.toBoolean()
                BOTTOM_ACTIONS -> config.bottomActions = value.toBoolean()
                VISIBLE_BOTTOM_ACTIONS -> config.visibleBottomActions = value.toInt()
                USE_RECYCLE_BIN -> config.useRecycleBin = value.toBoolean()
                SHOW_RECYCLE_BIN_AT_FOLDERS -> config.showRecycleBinAtFolders = value.toBoolean()
                SHOW_RECYCLE_BIN_LAST -> config.showRecycleBinLast = value.toBoolean()
                SORT_ORDER -> config.sorting = value.toInt()
                DIRECTORY_SORT_ORDER -> config.directorySorting = value.toInt()
                GROUP_BY -> config.groupBy = value.toInt()
                GROUP_DIRECT_SUBFOLDERS -> config.groupDirectSubfolders = value.toBoolean()
                PINNED_FOLDERS -> config.addPinnedFolders(value.toStringSet())
                DISPLAY_FILE_NAMES -> config.displayFileNames = value.toBoolean()
                FILTER_MEDIA -> config.filterMedia = value.toInt()
                DIR_COLUMN_CNT -> config.dirColumnCnt = value.toInt()
                MEDIA_COLUMN_CNT -> config.mediaColumnCnt = value.toInt()
                SHOW_ALL -> config.showAll = value.toBoolean()
                SHOW_WIDGET_FOLDER_NAME -> config.showWidgetFolderName = value.toBoolean()
                VIEW_TYPE_FILES -> config.viewTypeFiles = value.toInt()
                VIEW_TYPE_FOLDERS -> config.viewTypeFolders = value.toInt()
                SLIDESHOW_INTERVAL -> config.slideshowInterval = value.toInt()
                SLIDESHOW_INCLUDE_VIDEOS -> config.slideshowIncludeVideos = value.toBoolean()
                SLIDESHOW_INCLUDE_GIFS -> config.slideshowIncludeGIFs = value.toBoolean()
                SLIDESHOW_RANDOM_ORDER -> config.slideshowRandomOrder = value.toBoolean()
                SLIDESHOW_MOVE_BACKWARDS -> config.slideshowMoveBackwards = value.toBoolean()
                SLIDESHOW_LOOP -> config.loopSlideshow = value.toBoolean()
                LAST_EDITOR_CROP_ASPECT_RATIO -> config.lastEditorCropAspectRatio = value.toInt()
                LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X -> config.lastEditorCropOtherAspectRatioX =
                    value.toString().toFloat()
                LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y -> config.lastEditorCropOtherAspectRatioY =
                    value.toString().toFloat()
                LAST_CONFLICT_RESOLUTION -> config.lastConflictResolution = value.toInt()
                LAST_CONFLICT_APPLY_TO_ALL -> config.lastConflictApplyToAll = value.toBoolean()
                EDITOR_BRUSH_COLOR -> config.editorBrushColor = value.toInt()
                EDITOR_BRUSH_HARDNESS -> config.editorBrushHardness = value.toString().toFloat()
                EDITOR_BRUSH_SIZE -> config.editorBrushSize = value.toString().toFloat()
                FOLDER_THUMBNAIL_STYLE -> config.folderStyle = value.toInt()
                FOLDER_MEDIA_COUNT -> config.showFolderMediaCount = value.toInt()
                LIMIT_FOLDER_TITLE -> config.limitFolderTitle = value.toBoolean()
                THUMBNAIL_SPACING -> config.thumbnailSpacing = value.toInt()
                FILE_ROUNDED_CORNERS -> config.fileRoundedCorners = value.toBoolean()
                ALBUM_COVERS -> {
                    val existingCovers = config.parseAlbumCovers()
                    val existingCoverPaths =
                        existingCovers.map { it.path }.toMutableList() as ArrayList<String>

                    val listType = object : TypeToken<List<AlbumCover>>() {}.type
                    val covers = Gson().fromJson<ArrayList<AlbumCover>>(value.toString(), listType)
                        ?: ArrayList(1)
                    covers.filter { !existingCoverPaths.contains(it.path) && getDoesFilePathExist(it.tmb) }
                        .forEach {
                            existingCovers.add(it)
                        }

                    config.albumCovers = Gson().toJson(existingCovers)
                }
            }
        }

        toast(if (configValues.size > 0) R.string.settings_imported_successfully else R.string.no_entries_for_importing)
        runOnUiThread {
            setupSettingItems()
        }
    }
}
