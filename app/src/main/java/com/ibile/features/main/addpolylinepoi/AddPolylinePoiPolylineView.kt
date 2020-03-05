package com.ibile.features.main.addpolylinepoi

import android.content.Context
import android.view.View
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Polyline
import com.google.android.libraries.maps.model.PolylineOptions

@ModelView(autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT)
class AddPolylinePoiPolylineView(context: Context) : View(context) {
    private var polyline: Polyline? = null
    private var map: GoogleMap? = null
    private lateinit var points: List<LatLng>

    @ModelProp(ModelProp.Option.IgnoreRequireHashCode)
    fun setMap(map: GoogleMap) {
        this.map = map
    }

    @ModelProp
    fun setPoints(points: List<LatLng>) {
        this.points = points
        polyline?.points = this.points
    }

    fun remove() {
        map = null
        polyline?.remove()
        polyline = null
    }

    @AfterPropsSet
    fun useProps() {
        if (polyline != null) return
        polyline = map?.addPolyline(PolylineOptions().add(*points.toTypedArray()).width(5f))
    }
}