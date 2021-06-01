package com.loitp.ui.dialog

import android.annotation.SuppressLint
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.value
import com.loitp.pro.R
import kotlinx.android.synthetic.main.dialog_custom_aspect_ratio.view.*

@SuppressLint("InflateParams")
class CustomAspectRatioDialog(
    val activity: BaseSimpleActivity,
    private val defaultCustomAspectRatio: Pair<Float, Float>?,
    val callback: (aspectRatio: Pair<Float, Float>) -> Unit
) {
    init {
        val view =
            activity.layoutInflater.inflate(R.layout.dialog_custom_aspect_ratio, null).apply {
                etAspectRatioWidth.setText(
                    defaultCustomAspectRatio?.first?.toInt()?.toString() ?: ""
                )
                etAspectRatioHeight.setText(
                    defaultCustomAspectRatio?.second?.toInt()?.toString() ?: ""
                )
            }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    showKeyboard(view.etAspectRatioWidth)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val width = getViewValue(view.etAspectRatioWidth)
                        val height = getViewValue(view.etAspectRatioHeight)
                        callback(Pair(width, height))
                        dismiss()
                    }
                }
            }
    }

    private fun getViewValue(view: EditText): Float {
        val textValue = view.value
        return if (textValue.isEmpty()) 0f else textValue.toFloat()
    }
}
