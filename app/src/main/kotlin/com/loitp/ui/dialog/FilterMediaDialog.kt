package com.loitp.ui.dialog

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.loitp.pro.R
import com.loitp.ext.config
import com.loitp.pro.helpers.*
import kotlinx.android.synthetic.main.dialog_filter_media.view.*

class FilterMediaDialog(
    val activity: BaseSimpleActivity,
    val callback: (result: Int) -> Unit
) {
    @SuppressLint("InflateParams")
    private var view = activity.layoutInflater.inflate(R.layout.dialog_filter_media, null)

    init {
        val filterMedia = activity.config.filterMedia
        view.apply {
            cbFilterMediaImages.isChecked = filterMedia and TYPE_IMAGES != 0
            cbFilterMediaVideos.isChecked = filterMedia and TYPE_VIDEOS != 0
            filter_media_gifs.isChecked = filterMedia and TYPE_GIFS != 0
            cbFilterMediaRaws.isChecked = filterMedia and TYPE_RAWS != 0
            cbFilterMediaSvgs.isChecked = filterMedia and TYPE_SVGS != 0
            cbFilterMediaPortraits.isChecked = filterMedia and TYPE_PORTRAITS != 0
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.filter_media)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        if (view.cbFilterMediaImages.isChecked)
            result += TYPE_IMAGES
        if (view.cbFilterMediaVideos.isChecked)
            result += TYPE_VIDEOS
        if (view.filter_media_gifs.isChecked)
            result += TYPE_GIFS
        if (view.cbFilterMediaRaws.isChecked)
            result += TYPE_RAWS
        if (view.cbFilterMediaSvgs.isChecked)
            result += TYPE_SVGS
        if (view.cbFilterMediaPortraits.isChecked)
            result += TYPE_PORTRAITS

        if (result == 0) {
            result = getDefaultFileFilter()
        }

        activity.config.filterMedia = result
        callback(result)
    }
}
