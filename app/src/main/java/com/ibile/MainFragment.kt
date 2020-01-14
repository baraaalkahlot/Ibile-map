package com.ibile

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.LatLng
import com.ibile.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main), OnMapReadyCallback {
    lateinit var mapView: MapView
    lateinit var map: GoogleMap
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            requireActivity()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentMainBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_main, container, false
        )
        initializeMapView(binding, savedInstanceState)
        return binding.root
    }

    private fun initializeMapView(binding: FragmentMainBinding, savedInstanceState: Bundle?) {
        mapView = binding.mapView
        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_KEY_MAP_VIEW)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
        binding.btnMyLocation.setOnClickListener(handleMyLocationBtnClick)
    }

    private fun enableLocation() {
        val hasGrantedPermission = context?.let {
            ContextCompat.checkSelfPermission(it, ACCESS_FINE_LOCATION)
        } == PackageManager.PERMISSION_GRANTED
        if (hasGrantedPermission) {
            map.isMyLocationEnabled = true
            return
        }
        val activity = requireActivity()
        val showRequestRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
        if (showRequestRationale) {
            // TODO : show dialog explaining why permission is required and ask permission from there
            return
        }

        ActivityCompat.requestPermissions(
            activity, arrayOf(ACCESS_FINE_LOCATION), RC_ACCESS_FINE_LOCATION
        )
    }

    private fun moveMapToDeviceLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val locationLatLng = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLng(locationLatLng))
            }
        }
    }

    private val handleMyLocationBtnClick: (View) -> Unit = {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val locationLatLng = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLng(locationLatLng), 500, null)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        enableLocation()
        moveMapToDeviceLocation()
        // using custom location button in layout
        this.map.uiSettings.isMyLocationButtonEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode != RC_ACCESS_FINE_LOCATION) return
        val hasGrantedLocationPermission = permissions.size == 1 &&
                permissions[0] == ACCESS_FINE_LOCATION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (!hasGrantedLocationPermission) {
            // TODO: show error because permission is not granted
            return
        }
        map.isMyLocationEnabled = true
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(BUNDLE_KEY_MAP_VIEW)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(BUNDLE_KEY_MAP_VIEW, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()

    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        const val BUNDLE_KEY_MAP_VIEW = "MapViewBundleKey"
        const val RC_ACCESS_FINE_LOCATION = 1001
    }
}
