package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_change_folder_thumbnail_style.view.*
import kotlinx.android.synthetic.main.directory_item_grid_square.view.*

class ChangeFolderThumbnailStyleDialog(
    val activity: BaseSimpleActivity,
    val callback: () -> Unit
) : DialogInterface.OnClickListener {
    private var config = activity.config

    @SuppressLint("InflateParams")
    private var view: View =
        activity.layoutInflater.inflate(R.layout.dialog_change_folder_thumbnail_style, null)
            .apply {
                cbDialogFolderLimitTitle.isChecked = config.limitFolderTitle
            }

    init {

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    setupStyle()
                    setupMediaCount()
                    updateSample()
                }
            }
    }

    private fun setupStyle() {
        val styleRadio = view.dialog_radio_folder_style
        styleRadio.setOnCheckedChangeListener { _, _ ->
            updateSample()
        }

        val styleBtn = when (config.folderStyle) {
            FOLDER_STYLE_SQUARE -> styleRadio.rbDialogRadioFolderSquare
            else -> styleRadio.rbDialogRadioFolderRoundedCorners
        }

        styleBtn.isChecked = true
    }

    private fun setupMediaCount() {
        val countRadio = view.rgDialogRadioFolderCount
        countRadio.setOnCheckedChangeListener { _, _ ->
            updateSample()
        }

        val countBtn = when (config.showFolderMediaCount) {
            FOLDER_MEDIA_CNT_LINE -> countRadio.rbDialogRadioFolderCountLine
            FOLDER_MEDIA_CNT_BRACKETS -> countRadio.rbDialogRadioFolderCountBrackets
            else -> countRadio.rbDialogRadioFolderCountNone
        }

        countBtn.isChecked = true
    }

    @SuppressLint("SetTextI18n")
    private fun updateSample() {
        val photoCount = 36
        val folderName = "Camera"
        view.apply {
            val useRoundedCornersLayout =
                dialog_radio_folder_style.checkedRadioButtonId == R.id.rbDialogRadioFolderRoundedCorners
            layoutDialogFolderSample.removeAllViews()

            val layout =
                if (useRoundedCornersLayout) R.layout.directory_item_grid_rounded_corners else R.layout.directory_item_grid_square
            val sampleView = activity.layoutInflater.inflate(layout, null)
            layoutDialogFolderSample.addView(sampleView)

            sampleView.layoutParams.width =
                activity.resources.getDimension(R.dimen.sample_thumbnail_size).toInt()
            (sampleView.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL)

            when (rgDialogRadioFolderCount.checkedRadioButtonId) {
                R.id.rbDialogRadioFolderCountLine -> {
                    dir_name.text = folderName
                    photo_cnt.text = photoCount.toString()
                    photo_cnt.beVisible()
                }
                R.id.rbDialogRadioFolderCountBrackets -> {
                    photo_cnt.beGone()
                    dir_name.text = "$folderName ($photoCount)"
                }
                else -> {
                    dir_name.text = folderName
                    photo_cnt?.beGone()
                }
            }

            val options = RequestOptions().centerCrop()
            var builder = Glide.with(activity)
                .load(R.drawable.sample_logo)
                .apply(options)

            if (useRoundedCornersLayout) {
                val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_big).toInt()
                builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
                dir_name.setTextColor(activity.config.textColor)
                photo_cnt.setTextColor(activity.config.textColor)
            }

            builder.into(dir_thumbnail)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val style = when (view.dialog_radio_folder_style.checkedRadioButtonId) {
            R.id.rbDialogRadioFolderSquare -> FOLDER_STYLE_SQUARE
            else -> FOLDER_STYLE_ROUNDED_CORNERS
        }

        val count = when (view.rgDialogRadioFolderCount.checkedRadioButtonId) {
            R.id.rbDialogRadioFolderCountLine -> FOLDER_MEDIA_CNT_LINE
            R.id.rbDialogRadioFolderCountBrackets -> FOLDER_MEDIA_CNT_BRACKETS
            else -> FOLDER_MEDIA_CNT_NONE
        }

        config.folderStyle = style
        config.showFolderMediaCount = count
        config.limitFolderTitle = view.cbDialogFolderLimitTitle.isChecked
        callback()
    }
}
