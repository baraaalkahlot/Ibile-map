package com.ibile.features.main

import android.content.Context
import android.graphics.Color
import android.view.View
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.*
import com.ibile.data.database.entities.Marker
import com.ibile.data.database.entities.Marker.Companion.alpha

@ModelView(autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT)
class MarkerView(context: Context) : View(context) {

    private lateinit var mapMarker: com.google.android.libraries.maps.model.Marker
    private lateinit var mapPolygon: Polygon
    private lateinit var mapPolyline: Polyline

    private lateinit var _marker: Marker
    private lateinit var _map: GoogleMap

    private var _isActive: Boolean = false
    private var _isVisible: Boolean = true

    private var markerAdded: Boolean = false

    var onMarkerAdded: ((marker: Marker) -> Unit)? = null
        @CallbackProp set


    @ModelProp
    fun setMarker(marker: Marker) {
        markerAdded = false
        this._marker = marker
    }

    @ModelProp(ModelProp.Option.IgnoreRequireHashCode)
    fun setMap(map: GoogleMap) {
        markerAdded = false
        this._map = map
    }

    @ModelProp
    fun setIsActive(isActive: Boolean) {
        _isActive = isActive
        if (markerAdded) if (isActive) showActiveIndication() else removeActiveIndication()
    }

    @ModelProp
    fun isVisible(isVisible: Boolean) {
        _isVisible = isVisible
        if (!markerAdded) return
        setIsVisible()
    }

    private fun setIsVisible() {
        when {
            _marker.isMarker -> mapMarker.isVisible = _isVisible
            _marker.isPolyline -> mapPolyline.isVisible = _isVisible
            _marker.isPolygon -> mapPolygon.isVisible = _isVisible
        }
    }

    @AfterPropsSet
    fun useProps() {
        if (!markerAdded) addMarkerToMap()
    }

    fun removeMarker() {
        if (::mapMarker.isInitialized) mapMarker.remove()
        if (::mapPolygon.isInitialized) mapPolygon.remove()
        if (::mapPolyline.isInitialized) mapPolyline.remove()
    }

    private fun addMarkerToMap() {
        removeMarker()
        with(_marker) {
            when {
                isMarker -> {
                    val options = MarkerOptions()
                        .position(position)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.fromBitmap(icon!!.defaultBitmap))
                    mapMarker = _map.addMarker(options).apply { tag = this@with.id }
                }
                isPolyline -> {
                    val options = PolylineOptions().addAll(points)
                        .color(color)
                        .width(Marker.POLYLINE_DEFAULT_WIDTH)
                        .clickable(true)
                    mapPolyline = _map.addPolyline(options).apply { tag = this@with.id }
                }
                isPolygon -> {
                    val options = PolygonOptions().addAll(points)
                        .strokeColor(Color.BLACK)
                        .fillColor(color.alpha(Marker.POLYGON_DEFAULT_COLOR_ALPHA))
                        .strokeWidth(Marker.POLYGON_DEFAULT_WIDTH)
                        .clickable(true)
                    mapPolygon = _map.addPolygon(options).apply { tag = this@with.id }
                }
            }
        }
        markerAdded = true
        if (_isActive) showActiveIndication() else removeActiveIndication()
        setIsVisible()
        onMarkerAdded?.invoke(_marker)
    }

    private fun showActiveIndication() {
        with(_marker) {
            _map.animateCamera(cameraUpdate, 500, null)
            when {
                isPolyline -> mapPolyline.width = Marker.ACTIVE_POLYLINE_WIDTH
                isPolygon -> {
                    mapPolygon.strokeWidth = Marker.ACTIVE_POLYGON_WIDTH
                    mapPolygon.fillColor = color.alpha(Marker.POLYGON_ACTIVE_COLOR_ALPHA)
                }
                isMarker -> mapMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon!!.activeBitmap))
            }
        }
    }

    private fun removeActiveIndication() {
        with(_marker) {
            when {
                isPolyline -> mapPolyline.width = Marker.POLYLINE_DEFAULT_WIDTH
                isPolygon -> {
                    mapPolygon.strokeWidth = Marker.POLYGON_DEFAULT_WIDTH
                    mapPolygon.fillColor = color.alpha(Marker.POLYGON_DEFAULT_COLOR_ALPHA)
                }
                isMarker -> mapMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon!!.defaultBitmap))
            }
        }
    }
}
