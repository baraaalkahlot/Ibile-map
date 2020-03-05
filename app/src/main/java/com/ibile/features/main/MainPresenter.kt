package com.ibile.features.main

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.location.Location
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.withState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.addTo
import com.ibile.core.toObservable
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.MainFragment.Companion.RC_ACCESS_FINE_LOCATION
import io.reactivex.disposables.CompositeDisposable

typealias RunWithPermission = (
    block: () -> Unit,
    permission: String,
    requestCode: Int,
    showRequestPermissionRationale: () -> Unit
) -> Unit

class MainPresenter(
    private val mainViewModel: MainViewModel,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    private val compositeDisposable by lazy { CompositeDisposable() }
    lateinit var map: GoogleMap

    fun onCameraMove(cameraPosition: LatLng) {
        mainViewModel.updateState { copy(cameraPosition = cameraPosition) }
        val locationBtnIsActive = mainViewModel.locationButtonIsActive
        if (locationBtnIsActive.get()) locationBtnIsActive.set(false)
    }

    fun init() {
        mainViewModel.init()
    }


    fun onClickMyLocationBtn(runWithPermission: RunWithPermission) {
        runWithPermission(::moveToDeviceLocation, ACCESS_FINE_LOCATION, RC_ACCESS_FINE_LOCATION) {}
    }

    fun onMapReady(map: GoogleMap, runWithPermission: RunWithPermission) {
        this.map = map
        this.map.uiSettings.isMyLocationButtonEnabled = false
        runWithPermission({
            this.map.isMyLocationEnabled = true
            if (!moveToLastKnownLocation()) moveToDeviceLocation()
        }, ACCESS_FINE_LOCATION, RC_ACCESS_FINE_LOCATION) {}
    }

    private fun moveToLastKnownLocation(): Boolean {
        if (withState(mainViewModel) { it.activeMarkerId } != null) return true
        if (mainViewModel.state.cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(mainViewModel.state.cameraPosition))
            return true
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun moveToDeviceLocation() {
        fusedLocationClient.lastLocation
            .toObservable()
            .subscribe({ location: Location? ->
                location?.let {
                    val update = CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))
                    map.animateCamera(update, 500, object : GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            mainViewModel.locationButtonIsActive.set(true)
                        }

                        override fun onCancel() {}
                    })
                }
            }, {
                it.printStackTrace()
            })
            .addTo(compositeDisposable)
    }

    fun onDestroyMap() {
        compositeDisposable.clear()
    }

    fun onGrantLocationPermission() {
        map.isMyLocationEnabled = true
        moveToDeviceLocation()
    }

    fun onEditMarkerResult(result: Long?) {
        mainViewModel.setActiveMarkerId(result)
    }

    fun onExternalOverlayResult(result: MainFragment.Companion.ExternalOverlaysResult) {
        when (result) {
            is MainFragment.Companion.ExternalOverlaysResult.BrowseMarkers -> {
                mainViewModel.setActiveMarkerId(result.selectedMarkerId)
            }
            is MainFragment.Companion.ExternalOverlaysResult.LocationsSearch -> {
                mainViewModel.setActiveMarkerId(result.createdMarkerId)
            }
        }
    }

    fun onMapClick() {
        mainViewModel.setActiveMarkerId(null)
    }

    fun onMarkerClick(id: Long) {
        mainViewModel.setActiveMarkerId(id)
    }

    fun buildModels(controller: EpoxyController, onNewMarker: (marker: Marker) -> Unit) {
        withState(mainViewModel) { markersState ->
            markersState.markersAsync()?.map { marker ->
                controller.markerView {
                    id(marker.id)
                    marker(marker)
                    map(map)
                    isActive(markersState.activeMarkerId == marker.id)
                    isVisible(marker.id != markersState.marker_pointsEdit?.id)
                    onMarkerAdded { onNewMarker(it) }
                    this.onUnbind { _, view -> view.removeMarker() }
                }
            }
        }
    }

    fun onNewMarker(marker: Marker) {
        handleAddMarkerSuccess(marker)
    }

    private fun handleAddMarkerSuccess(marker: Marker) {
        mainViewModel.setActiveMarkerId(marker.id)
        mainViewModel.updateState { copy(marker_pointsEdit = null) }
    }

    fun onMarkerPointsUpdateInit(marker: Marker) {
        mainViewModel.updateState { copy(marker_pointsEdit = marker) }
    }

    fun onClickAddMarker() {
        mainViewModel.setActiveMarkerId(null)
    }

    fun onCancelAddOrEditMarkerPoints() {
        mainViewModel.updateState {
            copy(marker_pointsEdit = null, activeMarkerId = mainViewModel.state.marker_pointsEdit?.id)
        }
    }
}
