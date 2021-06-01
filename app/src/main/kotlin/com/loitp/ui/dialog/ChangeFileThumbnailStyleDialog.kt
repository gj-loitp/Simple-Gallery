package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.loitp.pro.R
import com.loitp.ext.config
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.dialog_change_file_thumbnail_style.view.*

@SuppressLint("InflateParams")
class ChangeFileThumbnailStyleDialog(
    val activity: BaseSimpleActivity
) : DialogInterface.OnClickListener {
    private var config = activity.config
    private var view: View
    private var thumbnailSpacing = config.thumbnailSpacing

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_file_thumbnail_style, null)
            .apply {
                swDialogFileStyleRoundedCorners.isChecked = config.fileRoundedCorners
                swDialogFileStyleAnimateGifs.isChecked = config.animateGifs
                swDialogFileStyleShowThumbnailVideoDuration.isChecked =
                    config.showThumbnailVideoDuration
                swDialogFileStyleShowThumbnailFileTypes.isChecked =
                    config.showThumbnailFileTypes

                layoutDialogFileStyleRoundedCorners.setOnClickListener { swDialogFileStyleRoundedCorners.toggle() }
                layoutDialogFileStyleAnimateGifs.setOnClickListener { swDialogFileStyleAnimateGifs.toggle() }
                layoutDialogFileStyleShowThumbnailVideoDuration.setOnClickListener { swDialogFileStyleShowThumbnailVideoDuration.toggle() }
                layoutDialogFileStyleShowThumbnailFileTypes.setOnClickListener { swDialogFileStyleShowThumbnailFileTypes.toggle() }

                layoutDialogFileStyleSpacing.setOnClickListener {
                    val items = arrayListOf(
                        RadioItem(0, "0x"),
                        RadioItem(1, "1x"),
                        RadioItem(2, "2x"),
                        RadioItem(4, "4x"),
                        RadioItem(8, "8x"),
                        RadioItem(16, "16x"),
                        RadioItem(32, "32x"),
                        RadioItem(64, "64x")
                    )

                    RadioGroupDialog(activity, items, thumbnailSpacing) {
                        thumbnailSpacing = it as Int
                        updateThumbnailSpacingText()
                    }
                }
            }

        updateThumbnailSpacingText()

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.fileRoundedCorners = view.swDialogFileStyleRoundedCorners.isChecked
        config.animateGifs = view.swDialogFileStyleAnimateGifs.isChecked
        config.showThumbnailVideoDuration =
            view.swDialogFileStyleShowThumbnailVideoDuration.isChecked
        config.showThumbnailFileTypes = view.swDialogFileStyleShowThumbnailFileTypes.isChecked
        config.thumbnailSpacing = thumbnailSpacing
    }

    @SuppressLint("SetTextI18n")
    private fun updateThumbnailSpacingText() {
        view.tvDialogFileStyleSpacing.text = "${thumbnailSpacing}x"
    }
}
