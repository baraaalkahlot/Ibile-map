package com.ibile.features.main

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.addTo
import com.ibile.core.toObservable
import com.ibile.features.main.MainFragment.Companion.RC_ACCESS_FINE_LOCATION
import io.reactivex.disposables.CompositeDisposable

typealias RunWithPermission = (
    block: () -> Unit,
    permission: String,
    requestCode: Int,
    showRequestPermissionRationale: () -> Unit
) -> Unit

class MainPresenter(
    private val uiStateViewModel: UIStateViewModel,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    private val compositeDisposable by lazy { CompositeDisposable() }
    lateinit var map: GoogleMap

    fun onCameraMove(cameraPosition: LatLng) {
        uiStateViewModel.updateState { copy(cameraPosition = cameraPosition) }
        val locationBtnIsActive = uiStateViewModel.locationButtonIsActive
        if (locationBtnIsActive.get()) locationBtnIsActive.set(false)
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
        if (uiStateViewModel.state.cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(uiStateViewModel.state.cameraPosition))
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
                            uiStateViewModel.locationButtonIsActive.set(true)
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
}
