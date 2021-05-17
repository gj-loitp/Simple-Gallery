package com.loitp.ui.dialog

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.loitp.adapter.MediaAdapter
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.ext.getCachedMedia
import com.loitp.pro.helpers.SHOW_ALL
import com.loitp.model.Medium
import com.loitp.pro.models.ThumbnailItem
import com.loitp.service.GetMediaAsyncTask
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getTimeFormat
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.views.MyGridLayoutManager
import kotlinx.android.synthetic.main.dialog_medium_picker.view.*

class PickMediumDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val callback: (path: String) -> Unit
) {
    var dialog: AlertDialog
    private var shownMedia = ArrayList<ThumbnailItem>()

    @SuppressLint("InflateParams")
    val view = activity.layoutInflater.inflate(R.layout.dialog_medium_picker, null)
    val viewType =
        activity.config.getFolderViewType(if (activity.config.showAll) SHOW_ALL else path)
    var isGridViewType = viewType == VIEW_TYPE_GRID

    init {
        (view.media_grid.layoutManager as MyGridLayoutManager).apply {
            orientation =
                if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.mediaColumnCnt else 1
        }

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.other_folder) { _, _ -> showOtherFolder() }
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.select_photo)
            }

        activity.getCachedMedia(path) {
            val media = it.filter { it is Medium } as ArrayList
            if (media.isNotEmpty()) {
                activity.runOnUiThread {
                    gotMedia(media)
                }
            }
        }

        GetMediaAsyncTask(
            context = activity,
            mPath = path,
            isPickImage = false,
            isPickVideo = false,
            showAll = false
        ) {
            gotMedia(it)
        }.execute()
    }

    private fun showOtherFolder() {
        PickDirectoryDialog(
            activity = activity,
            sourcePath = path,
            showOtherFolderButton = true,
            showFavoritesBin = true
        ) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun gotMedia(media: ArrayList<ThumbnailItem>) {
        if (media.hashCode() == shownMedia.hashCode())
            return

        shownMedia = media
        val adapter = MediaAdapter(
            activity = activity,
            media = shownMedia.clone() as ArrayList<ThumbnailItem>,
            listener = null,
            isAGetIntent = true,
            allowMultiplePicks = false,
            path = path,
            recyclerView = view.media_grid,
            fastScroller = null
        ) {
            if (it is Medium) {
                callback(it.path)
                dialog.dismiss()
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        val sorting = activity.config.getFolderSorting(if (path.isEmpty()) SHOW_ALL else path)
        val dateFormat = activity.config.dateFormat
        val timeFormat = activity.getTimeFormat()
        view.apply {
            media_grid.adapter = adapter

            media_vertical_fastscroller.isHorizontal = false
            media_vertical_fastscroller.beGoneIf(scrollHorizontally)

            media_horizontal_fastscroller.isHorizontal = true
            media_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                media_horizontal_fastscroller.setViews(media_grid) {
                    val medium = (media[it] as? Medium)
                    media_horizontal_fastscroller.updateBubbleText(
                        medium?.getBubbleText(
                            sorting = sorting,
                            context = activity,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        ) ?: ""
                    )
                }
            } else {
                media_vertical_fastscroller.setViews(media_grid) {
                    val medium = (media[it] as? Medium)
                    media_vertical_fastscroller.updateBubbleText(
                        medium?.getBubbleText(
                            sorting = sorting,
                            context = activity,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        ) ?: ""
                    )
                }
            }
        }
    }
}
