package com.ibile

import android.content.Context
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.*
import com.google.maps.android.SphericalUtil
import com.ibile.utils.bitmapFromVectorDrawable

class AddPolylineMarkerHandler(private val context: Context) {
    private var map: GoogleMap? = null

    private var polylinePath: Polyline? = null
    val points: ObservableArrayList<Marker?> = ObservableArrayList()

    val polylinePathDistance: ObservableField<String> = ObservableField()
    var currentPointCoords: ObservableField<LatLng> = ObservableField()

    val activePointIndexObservable = ObservableInt(-1)
    private var activePointIndex: Int
        get() = activePointIndexObservable.get()
        set(value) = activePointIndexObservable.set(value)

    fun setMap(map: GoogleMap?) {
        this.map = map
    }

    fun onMapMove(coords: LatLng) {
        currentPointCoords.set(coords)
        drawPolylinePath(coords)
    }

    fun addPoint() {
        map?.let {
            val insertionPoint = when {
                activePointIndexIsValid() -> activePointIndex + 1
                activePointIndex == -1 -> 0
                else -> points.size
            }

            val options = createMarkerPoint(it.cameraPosition.target, activePointIndexIsValid())
            points.add(insertionPoint, it.addMarker(options))

            activePointIndex = when {
                activePointIndexIsValid() -> activePointIndex + 1
                activePointIndex == -1 -> if (points.size == 1) points.size else -1
                else -> points.size
            }

            updatePointsMarkersIcons()
            drawPolylinePath(it.cameraPosition.target)
        }
    }

    fun handlePrevBtnClick() {
        if (activePointIndex == -1 || points.size == 0) return
        activePointIndex -= 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolylinePath(it.cameraPosition.target)
        }
        updatePointsMarkersIcons()
    }

    fun handleNextBtnClick() {
        if (activePointIndex == points.size || points.size == 0) return
        activePointIndex += 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolylinePath(it.cameraPosition.target)
        }
        updatePointsMarkersIcons()
    }

    fun deletePoint() {
        if (!activePointIndexIsValid()) return

        points[activePointIndex]?.remove()
        points.removeAt(activePointIndex)

        activePointIndex -= 1
        if (activePointIndex == -1 && points.size > 0) activePointIndex = 0

        updatePointsMarkersIcons()
        map?.let {
            val cameraPositionCoords =
                if (activePointIndexIsValid()) points[activePointIndex]?.position else it.cameraPosition.target
            map?.moveCamera(CameraUpdateFactory.newLatLng(cameraPositionCoords))
            drawPolylinePath(it.cameraPosition.target)
        }
    }

    /**
     * [pointIndex] is used in view databinding layout
     */
    fun activePointIndexIsValid(pointIndex: Int = activePointIndex): Boolean =
        pointIndex > -1 && pointIndex < points.size

    /**
     *
     * Parameters are required because of view databinding
     *
     * @param _points
     * @param pointIndex
     */
    fun distanceIsVisible(
        _points: MutableList<Marker?> = points, pointIndex: Int = activePointIndex
    ) = when {
        _points.size == 0 -> false
        _points.size == 1 && activePointIndexIsValid(pointIndex) -> false
        else -> true
    }

    private fun getPolylinePathDistance(): String {
        if (!distanceIsVisible()) return ""
        val distance = SphericalUtil.computeLength(polylinePath?.points)
        return if (distance < 1000) "${distance.toInt()} Meters" else "%.${2}f Km".format(distance / 1000)
    }

    private fun drawPolylinePath(newPoint: LatLng) {
        map?.let {
            if (activePointIndexIsValid()) points[activePointIndex]?.position = newPoint

            val pathPoints = getUpdatedPathPoints(newPoint)
            if (polylinePath != null) polylinePath?.points = pathPoints else
                polylinePath =
                    it.addPolyline(PolylineOptions().add(*pathPoints.toTypedArray()).width(5f))
            polylinePathDistance.set(getPolylinePathDistance())
        }
    }

    private fun updatePointsMarkersIcons() {
        points.forEachIndexed { index, marker ->
            marker?.setIcon(if (index == activePointIndex) activePointIcon else newPointIcon)
        }
    }

    private fun getUpdatedPathPoints(newPoint: LatLng): MutableList<LatLng?> {
        val pathPoints = points.map { it?.position }.toMutableList()
        return if (activePointIndexIsValid()) pathPoints else pathPoints
            .apply { add(if (activePointIndex == -1) 0 else points.size, newPoint) }
    }


    private fun createMarkerPoint(coords: LatLng, isActivePoint: Boolean): MarkerOptions =
        MarkerOptions()
            .position(coords)
            .anchor(0.49f, 0.48f)
            .icon(if (isActivePoint) activePointIcon else newPointIcon)

    private val newPointIcon: BitmapDescriptor?
        get() = BitmapDescriptorFactory.fromBitmap(
            bitmapFromVectorDrawable(context, R.drawable.ic_new_poly_marker_point)
        )

    private val activePointIcon: BitmapDescriptor?
        get() = BitmapDescriptorFactory.fromBitmap(
            bitmapFromVectorDrawable(context, R.drawable.ic_active_poly_marker_point)
        )

    fun reset() {
        polylinePath = null
        points.forEach { point -> point?.remove() }
        points.clear()
        polylinePathDistance.set("")
        activePointIndex = -1
        setMap(null)
    }
}
