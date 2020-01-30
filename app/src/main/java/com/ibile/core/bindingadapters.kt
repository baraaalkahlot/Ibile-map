package com.ibile.core

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import androidx.databinding.BindingAdapter

@BindingAdapter("app:isVisible")
fun isVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("imageButtonEnabled")
fun setImageButtonEnabled(imageButton: ImageButton, enabled: Boolean) {
    with(imageButton) {
        if (enabled == isEnabled) return
        isEnabled = enabled

        if (enabled) return setImageDrawable(tag as Drawable)
        if (tag == null) tag = drawable
        setImageDrawable(drawable.toGrayScale())
    }
}
