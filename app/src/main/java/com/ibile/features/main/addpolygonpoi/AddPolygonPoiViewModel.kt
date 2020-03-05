package com.ibile.features.main.addpolygonpoi

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
import com.ibile.R
import com.ibile.core.addTo
import com.ibile.core.bitmapFromVectorDrawable
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

open class AddPolygonPoiViewModel(
    private val context: Context,
    private val markersRepository: MarkersRepository
) : ViewModel() {
    private var map: GoogleMap? = null

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var mode: Mode =
        Mode.Add

    // when drawing the polygon, if the points are less than 3, no shape is drawn if the polygon
    // shape is used. This polyline is used in that case to indicate the least polygon path
    private var polygonPathPolyline: Polyline? = null
    private var polygonPath: Polygon? = null

    val points: ObservableArrayList<Marker?> = ObservableArrayList()
    val currentPointCoords: ObservableField<LatLng> = ObservableField()
    val polygonPathArea = ObservableField<String>()
    val polygonPathPerimeter = ObservableField<String>()

    val activePointIndexObservable = ObservableInt(-1)
    private var activePointIndex: Int
        get() = activePointIndexObservable.get()
        set(value) = activePointIndexObservable.set(value)

    fun init(map: GoogleMap?) {
        this.map = map
        mode =
            Mode.Add
        initShape()
    }

    private fun initShape() {
        map?.let {
            polygonPath = it.addPolygon(polygonOptions(it.cameraPosition.target))
            polygonPathPolyline = it.addPolyline(PolylineOptions().width(3f).color(Color.WHITE))
        }
    }

    fun onMapMove(coords: LatLng) {
        currentPointCoords.set(coords)
        drawPolygonPath(coords)
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
            drawPolygonPath(it.cameraPosition.target)
        }
    }

    fun handlePrevBtnClick() {
        if (activePointIndex == -1 || points.size == 0) return
        activePointIndex -= 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolygonPath(it.cameraPosition.target)
        }
        updateActivePointIcon()
    }

    fun handleNextBtnClick() {
        if (activePointIndex == points.size || points.size == 0) return
        activePointIndex += 1
        if (activePointIndexIsValid()) map?.let {
            it.moveCamera(CameraUpdateFactory.newLatLng(points[activePointIndex]?.position))
            drawPolygonPath(it.cameraPosition.target)
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
            drawPolygonPath(it.cameraPosition.target)
        }
    }

    fun saveBtnIsEnabled(_points: MutableList<Marker?>): Boolean = _points.size > 2

    /**
     * [pointIndex] is used in view databinding layout
     */
    fun activePointIndexIsValid(pointIndex: Int = activePointIndex): Boolean =
        pointIndex in points.indices

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
    ): Boolean = when {
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
        drawPolygonPath(map.cameraPosition.target)
    }

    fun handleSaveBtnClick() {
        val points = points.map { it?.position }
        when (mode) {
            is Mode.Add -> {
                val newMarker = com.ibile.data.database.entities.Marker.createPolygon(points)
                markersRepository
                    .insertMarker(newMarker)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
                    .addTo(compositeDisposable)
            }
            is Mode.Edit -> {
                val updatedMarker = (mode as Mode.Edit).marker.copy(points = points)
                markersRepository
                    .updateMarker(updatedMarker)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
                    .addTo(compositeDisposable)
            }
        }
    }

    fun onCreateOrUpdateSuccess(marker: com.ibile.data.database.entities.Marker) {
        reset()
    }

    private fun reset() {
        polygonPath?.remove()
        polygonPath = null
        polygonPathPolyline?.remove()
        polygonPathPolyline = null
        polygonPathPerimeter.set("")
        polygonPathArea.set("")

        points.forEach { point -> point?.remove() }
        points.clear()

        activePointIndex = -1
        init(null)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun initEditPoints(marker: com.ibile.data.database.entities.Marker, map: GoogleMap) {
        this.map = map
        mode =
            Mode.Edit(
                marker
            )
        currentPointCoords.set(map.cameraPosition.target)

        initShape()

        activePointIndex = marker.points.size
        val markerPoints = marker.points.mapIndexed { _, point ->
            map.addMarker(createMarkerPoint(point, false))
        }
        this.points.addAll(markerPoints)
        drawPolygonPath(marker.points.last()!!)
    }

    fun onCancel() {
        reset()
    }

    sealed class Mode {
        object Add : Mode()
        data class Edit(val marker: com.ibile.data.database.entities.Marker) : Mode()
    }
}
