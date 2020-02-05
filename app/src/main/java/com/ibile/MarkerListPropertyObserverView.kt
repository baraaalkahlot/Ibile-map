package com.ibile

import android.content.Context
import android.view.View
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.ibile.data.database.entities.Marker

@ModelView(autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT)
class MarkerListPropertyObserverView(context: Context) : View(context) {

    var onNewMarkerList: ((data: List<Marker>?) -> Unit)? = null
        @CallbackProp set

    private var markerList: List<Marker>? = null

    @ModelProp
    fun markerList(newMarkerList: List<Marker>?) {
        this.markerList = newMarkerList
    }

    @AfterPropsSet
    fun useProps() {
        onNewMarkerList?.invoke(markerList)
    }

    companion object {
        const val id = "MarkerListPropertyObserverView"
    }
}
