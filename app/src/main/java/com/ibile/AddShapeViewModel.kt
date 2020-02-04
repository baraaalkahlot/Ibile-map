package com.ibile

import android.content.Context
import android.graphics.Color
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.*
import com.google.maps.android.SphericalUtil
import com.ibile.core.bitmapFromVectorDrawable

open class AddShapeViewModel(private val context: Context) : ViewModel() {
    private var map: GoogleMap? = null

    private var polylinePath: Polyline? = null
    private var polygonPath: Polygon? = null
    // when drawing the polygon, if the points are less than 3, no shape is drawn if the polygon
    // shape is used. This polyline is used in that case to indicate the least polygon path
    private var polygonPathPolyline: Polyline? = null

    val points: ObservableArrayList<Marker?> = ObservableArrayList()

    val polylinePathDistance: ObservableField<String> = ObservableField()
    val currentPointCoords: ObservableField<LatLng> = ObservableField()
    val polygonPathArea = ObservableField<String>()
    val polygonPathPerimeter = ObservableField<String>()

    val polyTypeObservable: ObservableField<PolyType> = ObservableField()
    var polyType
        get() = polyTypeObservable.get()
        set(value) {
            polyTypeObservable.set(value)
        }

    val activePointIndexObservable = ObservableInt(-1)
    private var activePointIndex: Int
        get() = activePointIndexObservable.get()
        set(value) = activePointIndexObservable.set(value)

    fun init(map: GoogleMap?, type: PolyType?) {
        this.polyType = type
        this.map = map

        initShape()
    }

    private fun initShape() {
        map?.let {
            when (polyType) {
                PolyType.POLYLINE -> {
                    polylinePath = it.addPolyline(PolylineOptions().add().width(5f))
                }
                PolyType.POLYGON -> {
                    polygonPath = it.addPolygon(polygonOptions(it.cameraPosition.target))
                    polygonPathPolyline =
                        it.addPolyline(PolylineOptions().width(3f).color(Color.WHITE))
                }
            }
        }
    }

    fun onMapMove(coords: LatLng) {
        currentPointCoords.set(coords)
        drawPolyPath(coords)
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

            updateActivePointIcon()
            drawPolyPath(it.cameraPosition.target)
        }
    }

    fun handlePrevBtnClick() {
        if (activePointIndex == -1 || points.size == 0) return
        activePointIndex -= 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolyPath(it.cameraPosition.target)
        }
        updateActivePointIcon()
    }

    fun handleNextBtnClick() {
        if (activePointIndex == points.size || points.size == 0) return
        activePointIndex += 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolyPath(it.cameraPosition.target)
        }
        updateActivePointIcon()
    }

    fun deletePoint() {
        if (!activePointIndexIsValid()) return

        points[activePointIndex]?.remove()
        points.removeAt(activePointIndex)

        activePointIndex -= 1
        if (activePointIndex == -1 && points.size > 0) activePointIndex = 0

        updateActivePointIcon()
        map?.let {
            val cameraPositionCoords =
                if (activePointIndexIsValid()) points[activePointIndex]?.position else it.cameraPosition.target
            map?.moveCamera(CameraUpdateFactory.newLatLng(cameraPositionCoords))
            drawPolyPath(it.cameraPosition.target)
        }
    }

    fun saveBtnIsEnabled(_points: MutableList<Marker?>, type: PolyType?): Boolean = when (type) {
        PolyType.POLYGON -> _points.size > 2
        PolyType.POLYLINE -> _points.map { it?.position }.distinct().size > 1
        else -> false
    }

    /**
     * [pointIndex] is used in view databinding layout
     */
    fun activePointIndexIsValid(pointIndex: Int = activePointIndex): Boolean =
        pointIndex > -1 && pointIndex < points.size

    private fun drawPolyPath(newPoint: LatLng) {
        when (polyType) {
            PolyType.POLYLINE -> drawPolylinePath(newPoint)
            PolyType.POLYGON -> drawPolygonPath(newPoint)
        }
    }

    private fun drawPolylinePath(newPoint: LatLng) {
        map?.let {
            if (activePointIndexIsValid()) points[activePointIndex]?.position = newPoint

            val pathPoints = getUpdatedPathPoints(newPoint)
            polylinePath?.points = pathPoints
            polylinePathDistance.set(getPolylinePathDistance())
        }
    }

    private fun getPolylinePathDistance(): String {
        if (!showPolylineDistanceText()) return ""
        val distance = SphericalUtil.computeLength(polylinePath?.points)
        return if (distance < 1000) "${distance.toInt()} Meters" else "%.${2}f Km".format(distance / 1000)
    }

    /**
     *
     * Parameters are required because of view databinding
     *
     * @param _points
     * @param pointIndex
     */
    fun showPolylineDistanceText(
        _points: MutableList<Marker?> = points, pointIndex: Int = activePointIndex
    ) = polyType == PolyType.POLYLINE && when {
        _points.size == 0 -> false
        _points.size == 1 && activePointIndexIsValid(pointIndex) -> false
        else -> true
    }

    private fun getUpdatedPathPoints(newPoint: LatLng): MutableList<LatLng?> {
        val pathPoints = points.map { it?.position }.toMutableList()
        return if (activePointIndexIsValid()) pathPoints else pathPoints
            .apply {
                val insertionPoint = if (activePointIndex == -1) 0 else points.size
                add(insertionPoint, newPoint)
            }
    }

    private fun drawPolygonPath(newPoint: LatLng) {
        map?.let {
            if (activePointIndexIsValid()) points[activePointIndex]?.position = newPoint

            val pathPoints = getUpdatedPathPoints(newPoint).distinct()
            polygonPath?.remove()
            if (pathPoints.size < 3) {
                polygonPathPolyline?.points = pathPoints
            } else {
                polygonPathPolyline?.points = listOf()
                try {
                    // raises exception sometimes when multiple points have same values
                    polygonPath = map?.addPolygon(polygonOptions(*pathPoints.toTypedArray()))
                } catch (exception: ArrayIndexOutOfBoundsException) {

                }
            }
            polygonPathPerimeter.set(getPolygonPathPerimeter())
            polygonPathArea.set(getPolygonPathArea())
        }
    }

    private fun polygonOptions(vararg coords: LatLng?): PolygonOptions? = PolygonOptions()
        .add(*coords)
        .strokeWidth(3f).strokeColor(Color.WHITE).fillColor(Color.argb(175, 0, 0, 0))

    fun showPolygonPathDistanceText(
        _points: MutableList<Marker?> = points, pointIndex: Int = activePointIndex
    ): Boolean = polyType == PolyType.POLYGON && when {
        _points.size < 2 -> false
        _points.size == 2 && activePointIndexIsValid(pointIndex) -> false
        else -> true
    }

    private fun getPolygonPathPerimeter() =
        "%.${3}f".format(SphericalUtil.computeLength(polygonPath?.points) / 1000)

    private fun getPolygonPathArea(): String {
        val area = SphericalUtil.computeArea(polygonPath?.points)
        return when {
            area < 4047 -> "$area m²"
            area < 10000 -> "${area / 4047} a"
            else -> "${area / 10000} ha"
        }
    }

    private fun updateActivePointIcon() {
        points.forEachIndexed { index, marker ->
            marker?.setIcon(if (index == activePointIndex) activePointIcon else newPointIcon)
        }
    }

    private fun createMarkerPoint(coords: LatLng?, isActivePoint: Boolean): MarkerOptions =
        MarkerOptions()
            .position(coords)
            .anchor(0.49f, 0.48f)
            .icon(if (isActivePoint) activePointIcon else newPointIcon)

    private val newPointIcon: BitmapDescriptor?
        get() = BitmapDescriptorFactory.fromBitmap(
            context.bitmapFromVectorDrawable(R.drawable.ic_new_poly_marker_point)
        )

    private val activePointIcon: BitmapDescriptor?
        get() = BitmapDescriptorFactory.fromBitmap(
            context.bitmapFromVectorDrawable(R.drawable.ic_active_poly_marker_point)
        )

    fun setMap(map: GoogleMap?) {
        if (map == null) {
            this.map = null
            return
        }
        points.forEachIndexed { index, point ->
            points[index] =
                map.addMarker(createMarkerPoint(point?.position, index == activePointIndex))
        }
        initShape()
        drawPolyPath(map.cameraPosition.target)
    }

    fun reset() {
        polylinePath?.remove()
        polylinePath = null
        polylinePathDistance.set("")

        polygonPath?.remove()
        polygonPath = null
        polygonPathPolyline?.remove()
        polygonPathPolyline = null
        polygonPathPerimeter.set("")
        polygonPathArea.set("")

        points.forEach { point -> point?.remove() }
        points.clear()

        activePointIndex = -1
        init(null, null)
    }

    enum class PolyType { POLYLINE, POLYGON }
}
