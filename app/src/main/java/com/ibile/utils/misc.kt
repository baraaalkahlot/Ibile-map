package com.ibile.utils

import android.graphics.Color
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.ibile.core.Extensions.dp
import com.ibile.core.setColor
import org.koin.core.KoinComponent
import org.koin.core.get
import java.text.SimpleDateFormat
import java.util.*

object Misc : KoinComponent {
    fun createCircularProgressDrawable(
        color: Int = Color.WHITE,
        strokeWidth: Float = 5f,
        centerRadius: Float = 30f
    ): CircularProgressDrawable {
        return CircularProgressDrawable(get())
            .apply {
                setColor(color)
                this.strokeWidth = strokeWidth.dp
                this.centerRadius = centerRadius.dp
                start()
            }
    }
}

fun timeStampString(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
