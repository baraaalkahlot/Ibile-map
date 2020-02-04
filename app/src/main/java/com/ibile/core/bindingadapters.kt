package com.ibile.core

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.databinding.BindingAdapter
import com.airbnb.epoxy.EpoxyRecyclerView
import com.ibile.R
import com.ibile.data.database.entities.Marker

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
