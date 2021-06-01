package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.loitp.pro.R
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_delete_with_remember.view.*

class DeleteWithRememberDialog(
    val activity: Activity,
    val message: String,
    val callback: (remember: Boolean) -> Unit
) {
    private var dialog: AlertDialog

    @SuppressLint("InflateParams")
    val view = activity.layoutInflater.inflate(R.layout.dialog_delete_with_remember, null)!!

    init {
        view.tvDeleteRememberTitle.text = message
        val builder = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.yes) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.no, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback(view.cbDeleteRemember.isChecked)
    }
}
