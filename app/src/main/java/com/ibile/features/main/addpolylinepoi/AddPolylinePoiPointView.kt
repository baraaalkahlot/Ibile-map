package com.ibile.features.main.addpolylinepoi

import android.content.Context
import android.view.View
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.ibile.R
import com.ibile.core.bitmapFromVectorDrawable

@ModelView(autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT)
class AddPolylinePoiPointView(context: Context) : View(context) {
    private var marker: Marker? = null
    private var map: GoogleMap? = null
    private lateinit var position: LatLng
    private var isActive: Boolean = false

    @ModelProp
    fun setPosition(newPosition: LatLng) {
        position = newPosition
        marker?.position = newPosition
    }

    @ModelProp(ModelProp.Option.IgnoreRequireHashCode)
    fun setMap(map: GoogleMap) {
        this.map = map
    }

    @ModelProp
    fun isActive(isActive: Boolean) {
        this.isActive = isActive
        marker?.let {
            it.setIcon(if (isActive) activePointIcon else newPointIcon)
            if (isActive) map?.moveCamera(CameraUpdateFactory.newLatLng(it.position))
        }
    }

    @AfterPropsSet
    fun useProps() {
        if (marker != null) return
        val options = MarkerOptions()
            .position(position)
            .anchor(0.49f, 0.48f)
            .icon(if (isActive) activePointIcon else newPointIcon)
        marker = map?.addMarker(options)
    }

    fun remove() {
        marker?.remove()
        marker = null
        map = null
    }

    private val activePointIcon by lazy {
        BitmapDescriptorFactory.fromBitmap(context.bitmapFromVectorDrawable(R.drawable.ic_active_poly_marker_point))
    }
    private val newPointIcon by lazy {
        BitmapDescriptorFactory.fromBitmap(context.bitmapFromVectorDrawable(R.drawable.ic_new_poly_marker_point))
    }
}