package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.*
import kotlinx.android.synthetic.main.dialog_change_grouping.view.*

class ChangeGroupingDialog(
    val activity: BaseSimpleActivity,
    val path: String = "",
    val callback: () -> Unit
) :
    DialogInterface.OnClickListener {
    private var currGrouping = 0
    private var config = activity.config
    private val pathToUse = if (path.isEmpty()) SHOW_ALL else path

    @SuppressLint("InflateParams")
    private var view: View =
        activity.layoutInflater.inflate(R.layout.dialog_change_grouping, null).apply {
            cbGroupingDialogUseForThisFolder.isChecked = config.hasCustomGrouping(pathToUse)
            rbGroupingDialogRadioFolder.beVisibleIf(path.isEmpty())
        }

    init {

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view = view, dialog = this, titleId = R.string.group_by)
            }

        currGrouping = config.getFolderGrouping(pathToUse)
        setupGroupRadio()
        setupOrderRadio()
    }

    private fun setupGroupRadio() {
        val groupingRadio = view.rgGroupingDialogRadioGrouping

        val groupBtn = when {
            currGrouping and GROUP_BY_NONE != 0 -> groupingRadio.rbGroupingDialogRadioNone
            currGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 -> groupingRadio.rbGroupingDialogRadioLastModifiedDaily
            currGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0 -> groupingRadio.rbGroupingDialogRadioLastModifiedMonthly
            currGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 -> groupingRadio.rbGroupingDialogRadioDateTakenDaily
            currGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0 -> groupingRadio.rbGroupingDialogRadioDateTakenMonthly
            currGrouping and GROUP_BY_FILE_TYPE != 0 -> groupingRadio.rbGroupingDialogRadioFileType
            currGrouping and GROUP_BY_EXTENSION != 0 -> groupingRadio.rbGroupingDialogRadioExtension
            else -> groupingRadio.rbGroupingDialogRadioFolder
        }
        groupBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.rgGroupingDialogRadioOrder
        var orderBtn = orderRadio.rbGroupingDialogRadioAscending

        if (currGrouping and GROUP_DESCENDING != 0) {
            orderBtn = orderRadio.rbGroupingDialogRadioDescending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val groupingRadio = view.rgGroupingDialogRadioGrouping
        var grouping = when (groupingRadio.checkedRadioButtonId) {
            R.id.rbGroupingDialogRadioNone -> GROUP_BY_NONE
            R.id.rbGroupingDialogRadioLastModifiedDaily -> GROUP_BY_LAST_MODIFIED_DAILY
            R.id.rbGroupingDialogRadioLastModifiedMonthly -> GROUP_BY_LAST_MODIFIED_MONTHLY
            R.id.rbGroupingDialogRadioDateTakenDaily -> GROUP_BY_DATE_TAKEN_DAILY
            R.id.rbGroupingDialogRadioDateTakenMonthly -> GROUP_BY_DATE_TAKEN_MONTHLY
            R.id.rbGroupingDialogRadioFileType -> GROUP_BY_FILE_TYPE
            R.id.rbGroupingDialogRadioExtension -> GROUP_BY_EXTENSION
            else -> GROUP_BY_FOLDER
        }

        if (view.rgGroupingDialogRadioOrder.checkedRadioButtonId == R.id.rbGroupingDialogRadioDescending) {
            grouping = grouping or GROUP_DESCENDING
        }

        if (view.cbGroupingDialogUseForThisFolder.isChecked) {
            config.saveFolderGrouping(pathToUse, grouping)
        } else {
            config.removeFolderGrouping(pathToUse)
            config.groupBy = grouping
        }
        callback()
    }
}
