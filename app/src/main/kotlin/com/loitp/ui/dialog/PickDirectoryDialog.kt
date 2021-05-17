package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.loitp.adapter.DirectoryAdapter
import com.loitp.ext.*
import com.loitp.pro.R
import com.loitp.pro.models.Directory
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.views.MyGridLayoutManager
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*

class PickDirectoryDialog(
    val activity: BaseSimpleActivity,
    val sourcePath: String,
    showOtherFolderButton: Boolean,
    val showFavoritesBin: Boolean,
    val callback: (path: String) -> Unit
) {
    private var dialog: AlertDialog
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private var openedSubFolders = arrayListOf("")

    @SuppressLint("InflateParams")
    private var view = activity.layoutInflater.inflate(R.layout.dialog_directory_picker, null)
    private var isGridViewType = activity.config.viewTypeFolders == VIEW_TYPE_GRID
    private var showHidden = activity.config.shouldShowHidden
    private var currentPathPrefix = ""

    init {
        (view.directories_grid.layoutManager as MyGridLayoutManager).apply {
            orientation =
                if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.dirColumnCnt else 1
        }

        val builder = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setOnKeyListener { _, i, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP && i == KeyEvent.KEYCODE_BACK) {
                    backPressed()
                }
                true
            }

        if (showOtherFolderButton) {
            builder.setNeutralButton(R.string.other_folder) { _, _ -> showOtherFolder() }
        }

        dialog = builder.create().apply {
            activity.setupDialogStuff(
                view = view,
                dialog = this,
                titleId = R.string.select_destination
            ) {
                view.directories_show_hidden.beVisibleIf(!context.config.shouldShowHidden)
                view.directories_show_hidden.setOnClickListener {
                    activity.handleHiddenFolderPasswordProtection {
                        view.directories_show_hidden.beGone()
                        showHidden = true
                        fetchDirectories(true)
                    }
                }
            }
        }

        fetchDirectories(false)
    }

    private fun fetchDirectories(forceShowHidden: Boolean) {
        activity.getCachedDirectories(forceShowHidden = forceShowHidden) {
            if (it.isNotEmpty()) {
                it.forEach { d ->
                    d.subfoldersMediaCount = d.mediaCnt
                }

                activity.runOnUiThread {
                    gotDirectories(activity.addTempFolderIfNeeded(it))
                }
            }
        }
    }

    private fun showOtherFolder() {
        FilePickerDialog(
            activity = activity,
            currPath = sourcePath,
            pickFile = false,
            showHidden = showHidden,
            showFAB = true,
            canAddShowHiddenButton = true
        ) {
            activity.handleLockedFolderOpening(it) { success ->
                if (success) {
                    callback(it)
                }
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        if (allDirectories.isEmpty()) {
            allDirectories = newDirs.clone() as ArrayList<Directory>
        }

        val distinctDirs =
            newDirs.filter { showFavoritesBin || (!it.isRecycleBin() && !it.areFavorites()) }
                .distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<Directory>
        val sortedDirs = activity.getSortedDirectories(distinctDirs)
        val dirs = activity.getDirsToShow(
            dirs = sortedDirs,
            allDirs = allDirectories,
            currentPathPrefix = currentPathPrefix
        )
            .clone() as ArrayList<Directory>
        if (dirs.hashCode() == shownDirectories.hashCode()) {
            return
        }

        shownDirectories = dirs
        val adapter = DirectoryAdapter(
            activity = activity,
            dirs = dirs.clone() as ArrayList<Directory>,
            listener = null,
            recyclerView = view.directories_grid,
            isPickIntent = true
        ) {
            val clickedDir = it as Directory
            val path = clickedDir.path
            if (clickedDir.subfoldersCount == 1 || !activity.config.groupDirectSubfolders) {
                if (path.trimEnd('/') == sourcePath) {
                    activity.toast(R.string.source_and_destination_same)
                    return@DirectoryAdapter
                } else {
                    activity.handleLockedFolderOpening(path) { success ->
                        if (success) {
                            callback(path)
                        }
                    }
                    dialog.dismiss()
                }
            } else {
                currentPathPrefix = path
                openedSubFolders.add(path)
                gotDirectories(allDirectories)
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        val sorting = activity.config.directorySorting
        val dateFormat = activity.config.dateFormat
        val timeFormat = activity.getTimeFormat()
        view.apply {
            directories_grid.adapter = adapter

            directories_vertical_fastscroller.isHorizontal = false
            directories_vertical_fastscroller.beGoneIf(scrollHorizontally)

            directories_horizontal_fastscroller.isHorizontal = true
            directories_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                directories_horizontal_fastscroller.setViews(directories_grid) {
                    directories_horizontal_fastscroller.updateBubbleText(
                        dirs[it].getBubbleText(
                            sorting = sorting,
                            context = activity,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        )
                    )
                }
            } else {
                directories_vertical_fastscroller.setViews(directories_grid) {
                    directories_vertical_fastscroller.updateBubbleText(
                        dirs[it].getBubbleText(
                            sorting = sorting,
                            context = activity,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        )
                    )
                }
            }
        }
    }

    private fun backPressed() {
        if (activity.config.groupDirectSubfolders) {
            if (currentPathPrefix.isEmpty()) {
                dialog.dismiss()
            } else {
                openedSubFolders.removeAt(openedSubFolders.size - 1)
                currentPathPrefix = openedSubFolders.last()
                gotDirectories(allDirectories)
            }
        } else {
            dialog.dismiss()
        }
    }
}
