package com.ibile.features.main.addpolylinepoi

import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import com.google.maps.android.SphericalUtil
import com.ibile.features.main.addpolylinepoi.AddPolyLinePoiDatabindingViewData.Data

class DatabindingViewDataProvider {
    private val _data: ObservableField<Data> by lazy { ObservableField<Data>() }

    private lateinit var viewModel: AddPolylinePoiViewModel

    val data: ObservableField<Data>
        get() = _data

    fun init(viewModel: AddPolylinePoiViewModel, lifecycleOwner: LifecycleOwner) {
        this.viewModel = viewModel
        with(this.viewModel) {
            selectSubscribe(lifecycleOwner, AddPolylinePoiViewModel.State::points) {
                updateDatabindingViewData { copy(saveBtnIsEnabled = it.size > 1) }
            }
            selectSubscribe(
                lifecycleOwner,
                AddPolylinePoiViewModel.State::activePointIndex
            ) { activePointIndex ->
                val (points) = state
                updateDatabindingViewData {
                    copy(
                        previousBtnIsEnabled = activePointIndex > -1 && points.isNotEmpty(),
                        nextBtnIsEnabled = activePointIndex < points.size && points.isNotEmpty(),
                        removeBtnIsEnabled = activePointIndex in points.indices,
                        newPointTargetIsVisible = activePointIndex !in points.indices
                    )
                }
            }
            selectSubscribe(lifecycleOwner, AddPolylinePoiViewModel.State::cameraPosition) {
                updateDatabindingViewData {
                    copy(
                        instructionsIsVisible = !showPolylineDistanceText(),
                        distanceIsVisible = showPolylineDistanceText(),
                        currentPointCoords = Data
                            .CurrentPointCoordinates(it.latitude.toFloat(), it.longitude.toFloat()),
                        polylinePathDistance = getPolylinePathDistance()
                    )
                }
            }
        }
    }

    private fun updateDatabindingViewData(
        newUpdate: Data.() -> Data
    ) {
        _data.set(newUpdate(_data.get() ?: Data()))
    }

    private fun getPolylinePathDistance(): String {
        if (!showPolylineDistanceText()) return ""

        val (originalPoints, activePointIndex, cameraPosition) = viewModel.state
        val points = if (activePointIndex in originalPoints.indices) originalPoints
        else originalPoints + cameraPosition

        val distance = SphericalUtil.computeLength(points)
        return if (distance < 1000) "${distance.toInt()} Meters" else "%.${2}f Km"
            .format(distance / 1000)
    }

    private fun showPolylineDistanceText(): Boolean {
        val (points, activePointIndex) = viewModel.state
        return when {
            points.isEmpty() -> false
            points.size == 1 && activePointIndex in points.indices -> false
            else -> true
        }
    }
}
