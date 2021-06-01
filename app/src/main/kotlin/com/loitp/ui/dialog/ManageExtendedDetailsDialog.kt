package com.loitp.ui.dialog

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_manage_extended_details.view.*

class ManageExtendedDetailsDialog(
    val activity: BaseSimpleActivity,
    val callback: (result: Int) -> Unit
) {
    @SuppressLint("InflateParams")
    private var view =
        activity.layoutInflater.inflate(R.layout.dialog_manage_extended_details, null)

    init {
        val details = activity.config.extendedDetails
        view.apply {
            cbManageExtendedDetailsName.isChecked = details and EXT_NAME != 0
            cbManageExtendedDetailsPath.isChecked = details and EXT_PATH != 0
            cbManageExtendedDetailsSize.isChecked = details and EXT_SIZE != 0
            cbManageExtendedDetailsResolution.isChecked = details and EXT_RESOLUTION != 0
            cbManageExtendedDetailsLastModified.isChecked = details and EXT_LAST_MODIFIED != 0
            cbManageExtendedDetailsDateTaken.isChecked = details and EXT_DATE_TAKEN != 0
            cbManageExtendedDetailsCamera.isChecked = details and EXT_CAMERA_MODEL != 0
            cbManageExtendedDetailsExif.isChecked = details and EXT_EXIF_PROPERTIES != 0
            cbManageExtendedDetailsGpsCoordinates.isChecked = details and EXT_GPS != 0
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
            if (cbManageExtendedDetailsName.isChecked)
                result += EXT_NAME
            if (cbManageExtendedDetailsPath.isChecked)
                result += EXT_PATH
            if (cbManageExtendedDetailsSize.isChecked)
                result += EXT_SIZE
            if (cbManageExtendedDetailsResolution.isChecked)
                result += EXT_RESOLUTION
            if (cbManageExtendedDetailsLastModified.isChecked)
                result += EXT_LAST_MODIFIED
            if (cbManageExtendedDetailsDateTaken.isChecked)
                result += EXT_DATE_TAKEN
            if (cbManageExtendedDetailsCamera.isChecked)
                result += EXT_CAMERA_MODEL
            if (cbManageExtendedDetailsExif.isChecked)
                result += EXT_EXIF_PROPERTIES
            if (cbManageExtendedDetailsGpsCoordinates.isChecked)
                result += EXT_GPS
        }

        activity.config.extendedDetails = result
        callback(result)
    }
}
