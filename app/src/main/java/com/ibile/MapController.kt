package com.ibile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.withState
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.*
import com.ibile.core.bitmapFromVectorDrawable

/**
 * Wraps over the GoogleMap for some few isolated controls. Tied to the lifecycle of the map.
 *
 * @property markersViewModel
 * @property map
 */
class MapController(
    private var map: GoogleMap,
    private val context: Context,
    private val markersViewModel: MarkersViewModel
) {
    private val mapMarkers = mutableListOf<Marker>()
    private val mapPolylines = mutableListOf<Polyline>()
    private val mapPolygons = mutableListOf<Polygon>()

    private var activeMapPolyline: Polyline? = null
        set(value) {
            if (value != null) {
                activeMapPolygon = null
                activeMapMarker = null
            }
            field?.width = POLYLINE_WIDTH
            field = value
        }
    private var activeMapPolygon: Polygon? = null
        set(value) {
            if (value != null) {
                activeMapPolyline = null
                activeMapMarker = null
            }
            field?.strokeWidth = POLYGON_WIDTH
            field?.fillColor = POLYGON_FILL_COLOR
            field = value
        }
    private var activeMapMarker: Marker? = null
        set(value) {
            if (value != null) {
                activeMapPolyline = null
                activeMapPolygon = null
            }
            field?.setIcon(BitmapDescriptorFactory.defaultMarker())
            field = value
        }

    init {
        addMarkersToMap()
    }

    private val state
        get() = withState(markersViewModel) { it }

    fun buildModels(epoxyController: EpoxyController) {
        withState(markersViewModel) { (markersAsync) ->
            epoxyController.markerListPropertyObserverView {
                id(MarkerListPropertyObserverView.id)
                markersAsync()?.let { data(it) }
                dataCallback {
                    addNewMarkerToMap(it)
                    addNewMarkerFromLocationSearchToMap(it)
                }
            }
        }
    }

    private fun addNewMarkerToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        state.addMarkerAsync()?.let {
            val newMarker = markers.find { marker -> marker.id == it }
            newMarker?.let {
                markersViewModel.setActiveMarkerId(it.id)
                addMarkerToMap(it)
                markersViewModel.resetAddMarkerAsync()
            }
        }
    }

    private fun addNewMarkerFromLocationSearchToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        state.addMarkerFromLocationSearchAsync()?.let {
            val newMarker = markers.find { marker -> marker.id == it }
            newMarker?.let {
                markersViewModel.setActiveMarkerId(it.id)
                addMarkerToMap(it)
                markersViewModel.resetAddMarkerFromLocationSearchAsync()
            }
        }
    }

    private fun addMarkersToMap() {
        state.markersAsync()?.forEach { addMarkerToMap(it) }
    }

    private fun addMarkerToMap(it: com.ibile.data.database.entities.Marker) {
        when {
            it.isMarker -> {
                val markerOptions = MarkerOptions().position(it.position)
                val marker = map.addMarker(markerOptions)
                marker.tag = it.id
                mapMarkers.add(marker)
            }
            it.isPolyline -> {
                val options = PolylineOptions().addAll(it.points)
                    .color(DEFAULT_COLOR)
                    .width(POLYLINE_WIDTH)
                    .clickable(true)
                val polyline = map.addPolyline(options)
                polyline.tag = it.id
                mapPolylines.add(polyline)
            }
            it.isPolygon -> {
                val options = PolygonOptions().addAll(it.points)
                    .strokeColor(Color.BLACK)
                    .fillColor(POLYGON_FILL_COLOR)
                    .strokeWidth(POLYGON_WIDTH)
                    .clickable(true)
                val polygon = map.addPolygon(options)
                polygon.tag = it.id
                mapPolygons.add(polygon)
            }
        }
    }

    fun onPolylineClick(polyline: Polyline) {
        val activeMarkerId = polyline.tag as Long
        markersViewModel.setActiveMarkerId(activeMarkerId)
    }

    fun onMarkerClick(marker: Marker) {
        val activeMarkerId = marker.tag as Long
        markersViewModel.setActiveMarkerId(activeMarkerId)
    }

    fun onPolygonClick(polygon: Polygon) {
        val activeMarkerId = polygon.tag as Long
        markersViewModel.setActiveMarkerId(activeMarkerId)
    }

    fun toggleActiveMarkerIndication(marker: com.ibile.data.database.entities.Marker?) {
        if (marker == null) {
            activeMapMarker = null
            activeMapPolyline = null
            activeMapPolygon = null
        } else {
            map.animateCamera(marker.cameraUpdate, 500, null)
            when {
                marker.isPolyline -> {
                    activeMapPolyline = mapPolylines.find { it.tag as Long == marker.id }
                    activeMapPolyline?.width = ACTIVE_POLYLINE_WIDTH
                }
                marker.isPolygon -> {
                    activeMapPolygon = mapPolygons.find { it.tag as Long == marker.id }
                    activeMapPolygon?.strokeWidth = ACTIVE_POLYGON_WIDTH
                    activeMapPolygon?.fillColor = ACTIVE_POLYGON_FILL_COLOR
                }
                marker.isMarker -> {
                    activeMapMarker = mapMarkers.find { it.tag as Long == marker.id }
                    activeMapMarker?.setIcon(getActiveMarkerIcon(context))
                }
            }
        }
    }

    companion object {
        val DEFAULT_COLOR = Color.rgb(204, 54, 43)
        const val POLYLINE_WIDTH = 3F
        const val ACTIVE_POLYLINE_WIDTH = 6F

        val POLYGON_FILL_COLOR = Color.argb(95, 204, 54, 43)
        const val POLYGON_WIDTH = 3F
        const val ACTIVE_POLYGON_WIDTH = 5F
        val ACTIVE_POLYGON_FILL_COLOR = Color.argb(150, 204, 54, 43)

        fun getActiveMarkerIcon(context: Context): Bitmap? {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.DKGRAY
                this.style = Paint.Style.FILL
            }
            return context.bitmapFromVectorDrawable(
                R.drawable.map_icon_map_pin, Color.rgb(222, 44, 41)
            )?.applyCanvas {
                drawCircle(42f, 24f, 12f, paint)
            }
        }

        fun Marker?.setIcon(bitmap: Bitmap?) {
            this?.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
        }
    }
}