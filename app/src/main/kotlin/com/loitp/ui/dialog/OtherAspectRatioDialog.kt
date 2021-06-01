package com.loitp.ui.dialog

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.loitp.pro.R
import kotlinx.android.synthetic.main.dialog_other_aspect_ratio.view.*

@SuppressLint("InflateParams")
class OtherAspectRatioDialog(
    val activity: BaseSimpleActivity,
    private val lastOtherAspectRatio: Pair<Float, Float>?,
    val callback: (aspectRatio: Pair<Float, Float>) -> Unit
) {
    private val dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_other_aspect_ratio, null).apply {
            rbOtherAspectRatio_2_1.setOnClickListener { ratioPicked(Pair(2f, 1f)) }
            rbOtherAspectRatio_3_2.setOnClickListener { ratioPicked(Pair(3f, 2f)) }
            rbOtherAspectRatio_4_3.setOnClickListener { ratioPicked(Pair(4f, 3f)) }
            rbOtherAspectRatio_5_3.setOnClickListener { ratioPicked(Pair(5f, 3f)) }
            rbOtherAspectRatio_16_9.setOnClickListener { ratioPicked(Pair(16f, 9f)) }
            rbOtherAspectRatio_19_9.setOnClickListener { ratioPicked(Pair(19f, 9f)) }
            rbOtherAspectRatioCustom.setOnClickListener { customRatioPicked() }

            rbOtherAspectRatio_1_2.setOnClickListener { ratioPicked(Pair(1f, 2f)) }
            rbOtherAspectRatio_2_3.setOnClickListener { ratioPicked(Pair(2f, 3f)) }
            rbOtherAspectRatio_3_4.setOnClickListener { ratioPicked(Pair(3f, 4f)) }
            rbOtherAspectRatio_3_5.setOnClickListener { ratioPicked(Pair(3f, 5f)) }
            rbOtherAspectRatio_9_16.setOnClickListener { ratioPicked(Pair(9f, 16f)) }
            rbOtherAspectRatio_9_19.setOnClickListener { ratioPicked(Pair(9f, 19f)) }

            val radio1SelectedItemId = when (lastOtherAspectRatio) {
                Pair(2f, 1f) -> rbOtherAspectRatio_2_1.id
                Pair(3f, 2f) -> rbOtherAspectRatio_3_2.id
                Pair(4f, 3f) -> rbOtherAspectRatio_4_3.id
                Pair(5f, 3f) -> rbOtherAspectRatio_5_3.id
                Pair(16f, 9f) -> rbOtherAspectRatio_16_9.id
                Pair(19f, 9f) -> rbOtherAspectRatio_19_9.id
                else -> 0
            }
            rgOtherAspectRatioDialogRadio1.check(radio1SelectedItemId)

            val radio2SelectedItemId = when (lastOtherAspectRatio) {
                Pair(1f, 2f) -> rbOtherAspectRatio_1_2.id
                Pair(2f, 3f) -> rbOtherAspectRatio_2_3.id
                Pair(3f, 4f) -> rbOtherAspectRatio_3_4.id
                Pair(3f, 5f) -> rbOtherAspectRatio_3_5.id
                Pair(9f, 16f) -> rbOtherAspectRatio_9_16.id
                Pair(9f, 19f) -> rbOtherAspectRatio_9_19.id
                else -> 0
            }
            rgOtherAspectRatioDialogRadio_2.check(radio2SelectedItemId)

            if (radio1SelectedItemId == 0 && radio2SelectedItemId == 0) {
                rgOtherAspectRatioDialogRadio1.check(rbOtherAspectRatioCustom.id)
            }
        }

        dialog = AlertDialog.Builder(activity)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun customRatioPicked() {
        CustomAspectRatioDialog(
            activity = activity,
            defaultCustomAspectRatio = lastOtherAspectRatio
        ) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun ratioPicked(pair: Pair<Float, Float>) {
        callback(pair)
        dialog.dismiss()
    }
}
