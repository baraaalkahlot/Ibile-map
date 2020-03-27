package com.ibile.utils.views

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class List(context: Context) : Carousel(context) {
    @ModelProp
    fun setGridCount(count: Int) {
        layoutManager = GridLayoutManager(context, count, LinearLayoutManager.VERTICAL, false)
    }
}
