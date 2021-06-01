package com.loitp.ui.dialog

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_manage_bottom_actions.view.*

class ManageBottomActionsDialog(
    val activity: BaseSimpleActivity,
    val callback: (result: Int) -> Unit
) {
    @SuppressLint("InflateParams")
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_bottom_actions, null)

    init {
        val actions = activity.config.visibleBottomActions
        view.apply {
            cbManageBottomActionsToggleFavorite.isChecked =
                actions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0
            cbManageBottomActionsEdit.isChecked = actions and BOTTOM_ACTION_EDIT != 0
            cbManageBottomActionsShare.isChecked = actions and BOTTOM_ACTION_SHARE != 0
            cbManageBottomActionsDelete.isChecked = actions and BOTTOM_ACTION_DELETE != 0
            cbManageBottomActionsRotate.isChecked = actions and BOTTOM_ACTION_ROTATE != 0
            cbManageBottomActionsProperties.isChecked = actions and BOTTOM_ACTION_PROPERTIES != 0
            cbManageBottomActionsChangeOrientation.isChecked =
                actions and BOTTOM_ACTION_CHANGE_ORIENTATION != 0
            cbManageBottomActionsSlideshow.isChecked = actions and BOTTOM_ACTION_SLIDESHOW != 0
            cbManageBottomActionsShowOnMap.isChecked = actions and BOTTOM_ACTION_SHOW_ON_MAP != 0
            cbManageBottomActionsToggleVisibility.isChecked =
                actions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0
            cbManageBottomActionsRename.isChecked = actions and BOTTOM_ACTION_RENAME != 0
            cbManageBottomActionsSetAs.isChecked = actions and BOTTOM_ACTION_SET_AS != 0
            cbManageBottomActionsCopy.isChecked = actions and BOTTOM_ACTION_COPY != 0
            cbManageBottomActionsMove.isChecked = actions and BOTTOM_ACTION_MOVE != 0
            cbManageBottomActionsResize.isChecked = actions and BOTTOM_ACTION_RESIZE != 0
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        view.apply {
            if (cbManageBottomActionsToggleFavorite.isChecked)
                result += BOTTOM_ACTION_TOGGLE_FAVORITE
            if (cbManageBottomActionsEdit.isChecked)
                result += BOTTOM_ACTION_EDIT
            if (cbManageBottomActionsShare.isChecked)
                result += BOTTOM_ACTION_SHARE
            if (cbManageBottomActionsDelete.isChecked)
                result += BOTTOM_ACTION_DELETE
            if (cbManageBottomActionsRotate.isChecked)
                result += BOTTOM_ACTION_ROTATE
            if (cbManageBottomActionsProperties.isChecked)
                result += BOTTOM_ACTION_PROPERTIES
            if (cbManageBottomActionsChangeOrientation.isChecked)
                result += BOTTOM_ACTION_CHANGE_ORIENTATION
            if (cbManageBottomActionsSlideshow.isChecked)
                result += BOTTOM_ACTION_SLIDESHOW
            if (cbManageBottomActionsShowOnMap.isChecked)
                result += BOTTOM_ACTION_SHOW_ON_MAP
            if (cbManageBottomActionsToggleVisibility.isChecked)
                result += BOTTOM_ACTION_TOGGLE_VISIBILITY
            if (cbManageBottomActionsRename.isChecked)
                result += BOTTOM_ACTION_RENAME
            if (cbManageBottomActionsSetAs.isChecked)
                result += BOTTOM_ACTION_SET_AS
            if (cbManageBottomActionsCopy.isChecked)
                result += BOTTOM_ACTION_COPY
            if (cbManageBottomActionsMove.isChecked)
                result += BOTTOM_ACTION_MOVE
            if (cbManageBottomActionsResize.isChecked)
                result += BOTTOM_ACTION_RESIZE
        }

        activity.config.visibleBottomActions = result
        callback(result)
    }
}
