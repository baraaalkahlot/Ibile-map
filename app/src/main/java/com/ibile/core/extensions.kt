package com.ibile.core

import android.animation.ObjectAnimator
import android.view.View
import java.util.*

fun View.animateSlideVertical(distance: Float, duration: Long) {
    ObjectAnimator.ofFloat(this, "translationY", distance).apply {
        this.duration = duration
        start()
    }
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}
