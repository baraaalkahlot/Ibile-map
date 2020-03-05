package com.ibile.features.main.addmarkerpoi

import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.ibile.R
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.PartialAddNewMarkerOverlayBinding

class AddMarkerPoiPresenter(private val viewModel: AddMarkerPoiViewModel) {
    val databindingViewData by lazy { viewModel }

    fun onClickOkBtn(map: GoogleMap) {
        when (viewModel.state.mode) {
            is Mode.Add -> viewModel
                .addMarker(Marker.createMarker(map.cameraPosition.target))
            is Mode.Edit -> {
                val marker =
                    (viewModel.state.mode as Mode.Edit).marker
                val updatedMarker = marker.copy(points = listOf(map.cameraPosition.target))
                viewModel.updateMarker(updatedMarker)
            }
        }
    }

    fun onCancel(binding: PartialAddNewMarkerOverlayBinding) {
        reset(binding)
    }

    fun onMapMove(cameraPosition: LatLng) {
        viewModel.markerTargetCoords.set(cameraPosition)
    }

    fun init(map: GoogleMap) {
        viewModel.updateState { copy(mode = Mode.Add) }
        viewModel.markerTargetCoords.set(map.cameraPosition.target)
    }

    fun initEditMarkerPoint(
        marker: Marker,
        binding: PartialAddNewMarkerOverlayBinding,
        map: GoogleMap
    ) {
        viewModel.updateState { copy(mode = Mode.Edit(marker)) }
        binding.ivNewMarkerDummy.setImageBitmap(marker.icon!!.defaultBitmap)
        viewModel.markerTargetCoords.set(map.cameraPosition.target)
    }

    private fun reset(binding: PartialAddNewMarkerOverlayBinding) {
        viewModel.updateState { copy(mode = Mode.Add) }
        binding.ivNewMarkerDummy
            .setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_location_marker_dummy))
    }

    fun onCreateOrUpdateSuccess(marker: Marker, binding: PartialAddNewMarkerOverlayBinding) {
        reset(binding)
    }

    sealed class Mode {
        object Add : Mode()
        data class Edit(val marker: Marker) : Mode()
    }
}