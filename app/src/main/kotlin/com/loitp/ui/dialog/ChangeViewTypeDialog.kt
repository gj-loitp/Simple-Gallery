package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.SHOW_ALL
import kotlinx.android.synthetic.main.dialog_change_view_type.view.*

@SuppressLint("InflateParams")
class ChangeViewTypeDialog(
    val activity: BaseSimpleActivity,
    val fromFoldersView: Boolean,
    val path: String = "",
    val callback: () -> Unit
) {
    private var view: View
    private var config = activity.config
    private var pathToUse = if (path.isEmpty()) SHOW_ALL else path

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_view_type, null).apply {
            val viewToCheck = if (fromFoldersView) {
                if (config.viewTypeFolders == VIEW_TYPE_GRID) {
                    rbChangeViewTypeDialogRadioGrid.id
                } else {
                    rbChangeViewTypeDialogRadioList.id
                }
            } else {
                val currViewType = config.getFolderViewType(pathToUse)
                if (currViewType == VIEW_TYPE_GRID) {
                    rbChangeViewTypeDialogRadioGrid.id
                } else {
                    rbChangeViewTypeDialogRadioList.id
                }
            }

            rgChangeViewTypeDialogRadio.check(viewToCheck)
            cbChangeViewTypeDialogGroupDirectSubfolders.apply {
                beVisibleIf(fromFoldersView)
                isChecked = config.groupDirectSubfolders
            }

            cbChangeViewTypeDialogUseForThisFolder.apply {
                beVisibleIf(!fromFoldersView)
                isChecked = config.hasCustomViewType(pathToUse)
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val viewType =
            if (view.rgChangeViewTypeDialogRadio.checkedRadioButtonId == view.rbChangeViewTypeDialogRadioGrid.id) VIEW_TYPE_GRID else VIEW_TYPE_LIST
        if (fromFoldersView) {
            config.viewTypeFolders = viewType
            config.groupDirectSubfolders =
                view.cbChangeViewTypeDialogGroupDirectSubfolders.isChecked
        } else {
            if (view.cbChangeViewTypeDialogUseForThisFolder.isChecked) {
                config.saveFolderViewType(pathToUse, viewType)
            } else {
                config.removeFolderViewType(pathToUse)
                config.viewTypeFiles = viewType
            }
        }

        callback()
    }
}
