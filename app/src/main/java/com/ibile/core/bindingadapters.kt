package com.ibile.core

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("app:isVisible")
fun isVisible(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}
