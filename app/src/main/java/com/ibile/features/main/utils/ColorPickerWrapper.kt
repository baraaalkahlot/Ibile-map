package com.ibile.features.main.utils

import android.app.Activity
import androidx.annotation.ArrayRes
import androidx.appcompat.widget.AppCompatTextView
import com.ibile.R
import com.ibile.core.Extensions.dp
import com.ibile.core.getResColor
import petrov.kristiyan.colorpicker.ColorPicker

class ColorPickerWrapper(private val activity: Activity, private val options: Options) {
    fun create(): ColorPicker {
        val colorPicker = ColorPicker(activity)
            .disableDefaultButtons(options.disableDefaultButtons)
            .setColumns(options.numColumns)
            .setDefaultColorButton(options.defaultColorBtn)
            .setTitle(options.title)
            .setRoundColorButton(options.roundColorBtn)
            .setColors(options.colors)
            .setOnFastChooseColorListener(options.onFastChooseColorListener)
        with(colorPicker.dialogViewLayout) {
            setBackgroundColor(context.getResColor(R.color.dark_gray))
            val titleView =
                findViewById<AppCompatTextView>(petrov.kristiyan.colorpicker.R.id.title)
            titleView.setTextColor(context.getResColor(R.color.white))
            titleView.textSize = 12f.dp
        }
        return colorPicker
    }

    data class Options(
        val defaultColorBtn: Int,
        val title: String,
        val onFastChooseColorListener: ColorPicker.OnFastChooseColorListener,
        val disableDefaultButtons: Boolean = true,
        val numColumns: Int = 5,
        val roundColorBtn: Boolean = true,
        @ArrayRes val colors: Int = R.array.marker_colors
    )
}
