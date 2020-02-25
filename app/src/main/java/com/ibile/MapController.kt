package com.ibile

import android.graphics.Color
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.*

/**
 * Wraps over the GoogleMap for some few isolated controls. Tied to the lifecycle of the map.
 *
 * @property markersViewModel
 * @property map
 */
class MapController(private var map: GoogleMap, private val markersViewModel: MarkersViewModel) {
    private var markersAdded: Boolean = false

    private val mapMarkers = mutableListOf<Marker>()
    private val mapPolylines = mutableListOf<Polyline>()
    private val mapPolygons = mutableListOf<Polygon>()

    private var activeMapPolyline: Polyline? = null
        set(value) {
            if (value != null) {
                activeMapPolygon = null
                activeMapMarker = null
            }
            field?.width = POLYLINE_DEFAULT_WIDTH
            field = value
        }
    private var activeMapPolygon: Polygon? = null
        set(value) {
            if (value != null) {
                activeMapPolyline = null
                activeMapMarker = null
            }
            field?.let {
                it.strokeWidth = POLYGON_DEFAULT_WIDTH
                it.fillColor = findMarkerForShape(it.tag).color.alpha(POLYGON_DEFAULT_COLOR_ALPHA)
            }
            field = value
        }
    private var activeMapMarker: Marker? = null
        set(value) {
            if (value != null) {
                activeMapPolyline = null
                activeMapPolygon = null
            }
            field?.let {
                val defaultBitmap = findMarkerForShape(it.tag).icon!!.defaultBitmap
                it.setIcon(BitmapDescriptorFactory.fromBitmap(defaultBitmap))
            }
            field = value
        }

    init {
        markersViewModel.state.markersAsync()?.let { addMarkersToMap(it) }
    }

    private fun findMarkerForShape(obj: Any): com.ibile.data.database.entities.Marker =
        markersViewModel.getMarkerById(obj as Long)

    fun onMarkersListUpdate(markers: List<com.ibile.data.database.entities.Marker>) {
        addMarkersToMap(markers)
        addNewMarkerToMap(markers)
        updateEditedMarkerOnMap(markers)
    }

    private fun addMarkersToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        if (markersAdded) return
        markers.forEach { marker -> addMarkerToMap(marker) }
        markersAdded = true
    }

    private fun addNewMarkerToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        markersViewModel.state.addMarkerAsync()?.let {
            val newMarker = markers.find { marker -> marker.id == it }
            newMarker?.let {
                if (!markerAdded(it)) addMarkerToMap(it)
                markersViewModel.setActiveMarkerId(it.id)
                markersViewModel.resetAddMarkerAsync()
            }
        }
    }

    private fun markerAdded(marker: com.ibile.data.database.entities.Marker): Boolean = when {
        marker.isMarker -> mapMarkers.any { it.tag as Long == marker.id }
        marker.isPolygon -> mapPolygons.any { it.tag as Long == marker.id }
        marker.isPolyline -> mapPolylines.any { it.tag as Long == marker.id }
        else -> false
    }

    private fun updateEditedMarkerOnMap(markers: List<com.ibile.data.database.entities.Marker>) {
        markersViewModel.state.markerForEdit?.let { markerForEdit ->
            val marker = markers.find { it.id == markerForEdit.id }!!
            when {
                marker.isMarker -> mapMarkers.find { it.tag as Long == marker.id }?.let {
                    it.remove()
                    mapMarkers.removeAt(mapMarkers.indexOf(it))
                }
                marker.isPolygon -> mapPolygons.find { it.tag as Long == marker.id }?.let {
                    it.remove()
                    mapPolygons.removeAt(mapPolygons.indexOf(it))
                }
                marker.isPolyline -> mapPolylines.find { it.tag as Long == marker.id }?.let {
                    it.remove()
                    mapPolylines.removeAt(mapPolylines.indexOf(it))
                }
            }
            addMarkerToMap(marker)
            markersViewModel.setMarkerForEdit(null)
        }
    }

    private fun addMarkerToMap(it: com.ibile.data.database.entities.Marker) {
        when {
            it.isMarker -> {
                val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(it.icon!!.defaultBitmap)
                val markerOptions = MarkerOptions().position(it.position).icon(bitmapDescriptor)
                val marker = map.addMarker(markerOptions)
                marker.tag = it.id
                mapMarkers.add(marker)
            }
            it.isPolyline -> {
                val options = PolylineOptions().addAll(it.points)
                    .color(it.color)
                    .width(POLYLINE_DEFAULT_WIDTH)
                    .clickable(true)
                val polyline = map.addPolyline(options)
                polyline.tag = it.id
                mapPolylines.add(polyline)
            }
            it.isPolygon -> {
                val options = PolygonOptions().addAll(it.points)
                    .strokeColor(Color.BLACK)
                    .fillColor(it.color.alpha(POLYGON_DEFAULT_COLOR_ALPHA))
                    .strokeWidth(POLYGON_DEFAULT_WIDTH)
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
                    activeMapPolyline!!.width = ACTIVE_POLYLINE_WIDTH
                }
                marker.isPolygon -> {
                    activeMapPolygon = mapPolygons.find { it.tag as Long == marker.id }
                    activeMapPolygon!!.strokeWidth = ACTIVE_POLYGON_WIDTH
                    activeMapPolygon!!.fillColor = marker.color.alpha(POLYGON_ACTIVE_COLOR_ALPHA)
                }
                marker.isMarker -> {
                    activeMapMarker = mapMarkers.find { it.tag as Long == marker.id }
                    val activeBitmap =
                        BitmapDescriptorFactory.fromBitmap(marker.icon!!.activeBitmap)
                    activeMapMarker!!.setIcon(activeBitmap)
                }
            }
        }
    }

    companion object {
        const val POLYLINE_DEFAULT_WIDTH = 3F
        const val ACTIVE_POLYLINE_WIDTH = 6F

        const val POLYGON_DEFAULT_WIDTH = 3F
        const val ACTIVE_POLYGON_WIDTH = 5F
        const val POLYGON_DEFAULT_COLOR_ALPHA = 95
        const val POLYGON_ACTIVE_COLOR_ALPHA = 150

        private fun Int.alpha(value: Int): Int =
            Color.argb(value, Color.red(this), Color.green(this), Color.blue(this))
    }
}
