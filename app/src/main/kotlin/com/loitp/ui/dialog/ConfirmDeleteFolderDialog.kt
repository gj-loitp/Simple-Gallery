package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.loitp.pro.R
import kotlinx.android.synthetic.main.dialog_confirm_delete_folder.view.*

@SuppressLint("InflateParams")
class ConfirmDeleteFolderDialog(
    activity: Activity,
    message: String,
    warningMessage: String,
    val callback: () -> Unit
) {
    var dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_delete_folder, null)
        view.tvMessage.text = message
        view.tvMessageWarning.text = warningMessage

        val builder = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.yes) { _, _ -> dialogConfirmed() }

        builder.setNegativeButton(R.string.no, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}
