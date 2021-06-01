package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isVisible
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.SHOW_ALL
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

@SuppressLint("InflateParams")
class ChangeSortingDialog(
    val activity: BaseSimpleActivity,
    val isDirectorySorting: Boolean,
    val showFolderCheckbox: Boolean,
    val path: String = "",
    val callback: () -> Unit
) :
    DialogInterface.OnClickListener {
    private var currSorting = 0
    private var config = activity.config
    private var pathToUse = if (!isDirectorySorting && path.isEmpty()) SHOW_ALL else path
    private var view: View

    init {
        currSorting =
            if (isDirectorySorting) config.directorySorting else config.getFolderSorting(pathToUse)
        view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null).apply {
            use_for_this_folder_divider.beVisibleIf(showFolderCheckbox || (currSorting and SORT_BY_NAME != 0 || currSorting and SORT_BY_PATH != 0))

            cbSortingDialogNumericSorting.beVisibleIf(showFolderCheckbox && (currSorting and SORT_BY_NAME != 0 || currSorting and SORT_BY_PATH != 0))
            cbSortingDialogNumericSorting.isChecked = currSorting and SORT_USE_NUMERIC_VALUE != 0

            cbSortingDialogUseForThisFolder.beVisibleIf(showFolderCheckbox)
            cbSortingDialogUseForThisFolder.isChecked = config.hasCustomSorting(pathToUse)
            tvSortingDialogBottomNote.beVisibleIf(!isDirectorySorting)
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view = view, dialog = this, titleId = R.string.sort_by)
            }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.rgSortingDialogRadioSorting
        sortingRadio.setOnCheckedChangeListener { group, checkedId ->
            val isSortingByNameOrPath =
                checkedId == sortingRadio.rbSortingDialogRadioName.id || checkedId == sortingRadio.rbSortingDialogRadioPath.id
            view.cbSortingDialogNumericSorting.beVisibleIf(isSortingByNameOrPath)
            view.use_for_this_folder_divider.beVisibleIf(view.cbSortingDialogNumericSorting.isVisible() || view.cbSortingDialogUseForThisFolder.isVisible())
        }

        val sortBtn = when {
            currSorting and SORT_BY_PATH != 0 -> sortingRadio.rbSortingDialogRadioPath
            currSorting and SORT_BY_SIZE != 0 -> sortingRadio.rbSortingDialogRadioSize
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.rbSortingDialogRadioLastModified
            currSorting and SORT_BY_DATE_TAKEN != 0 -> sortingRadio.rbSortingDialogRadioDateTaken
            currSorting and SORT_BY_RANDOM != 0 -> sortingRadio.rbSortingDialogRadioRandom
            else -> sortingRadio.rbSortingDialogRadioName
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.rgSortingDialogRadioOrder
        var orderBtn = orderRadio.rbSortingDialogRadioAscending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.rbSortingDialogRadioDescending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val sortingRadio = view.rgSortingDialogRadioSorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.rbSortingDialogRadioName -> SORT_BY_NAME
            R.id.rbSortingDialogRadioPath -> SORT_BY_PATH
            R.id.rbSortingDialogRadioSize -> SORT_BY_SIZE
            R.id.rbSortingDialogRadioLastModified -> SORT_BY_DATE_MODIFIED
            R.id.rbSortingDialogRadioRandom -> SORT_BY_RANDOM
            else -> SORT_BY_DATE_TAKEN
        }

        if (view.rgSortingDialogRadioOrder.checkedRadioButtonId == R.id.rbSortingDialogRadioDescending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (view.cbSortingDialogNumericSorting.isChecked) {
            sorting = sorting or SORT_USE_NUMERIC_VALUE
        }

        if (isDirectorySorting) {
            config.directorySorting = sorting
        } else {
            if (view.cbSortingDialogUseForThisFolder.isChecked) {
                config.saveCustomSorting(pathToUse, sorting)
            } else {
                config.removeCustomSorting(pathToUse)
                config.sorting = sorting
            }
        }
        callback()
    }
}
