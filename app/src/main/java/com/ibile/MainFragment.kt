package com.ibile

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import com.ibile.databinding.FragmentMainBinding
import org.koin.androidx.scope.lifecycleScope
import org.koin.core.parameter.parametersOf
import com.airbnb.mvrx.withState as withViewModelsState

class MainFragment : BaseMvRxFragment(R.layout.fragment_main), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private var pendingLocationPermissionAction: () -> Unit = {}
    lateinit var mapView: MapView
    lateinit var map: GoogleMap
    private lateinit var binding: FragmentMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val uiStateHandler: UIStateHandler by lifecycleScope.inject { parametersOf(binding) }
    private val markersViewModel: MarkersViewModel by fragmentViewModel()
    private val iconGenerator: IconGenerator by lazy { IconGenerator(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding, savedInstanceState)
        binding.fragment = this
        binding.uiStateHandler = uiStateHandler
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    private fun initializeMapView(binding: FragmentMainBinding, savedInstanceState: Bundle?) {
        mapView = binding.mapView
        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_KEY_MAP_VIEW)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        uiStateHandler.repositionLocationCompass(mapView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(markersViewModel) {
            init()
            asyncSubscribe(MarkersViewModelState::markersAsync) {
                addMarkersToMap(it)
                // since after adding a new marker, the markers list is updated and there is a need
                // to show the new marker info
                toggleMarkerInfoView()
            }
            selectSubscribe(MarkersViewModelState::activeMarkerId) {
                toggleMarkerInfoView()
            }
        }
    }

    private fun addMarkersToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        map.clear()
        markers.forEach {
            val markerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
            val marker = map.addMarker(markerOptions)
            marker.tag = it.id
        }
    }

    private fun toggleMarkerInfoView() {
        val activeMarker = withViewModelsState(markersViewModel) { state ->
            val (markersAsync, activeMarkerId) = state
            activeMarkerId?.let { markersAsync()?.find { it.id == activeMarkerId } }
        }
        uiStateHandler.toggleMarkerInfoView(activeMarker)
    }

    override fun invalidate() {
        // not required for now
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        enableLocation()
        with(this.map) {
            // using custom location button in layout
            uiSettings.isMyLocationButtonEnabled = false
            setOnCameraMoveStartedListener(this@MainFragment)
            setOnCameraMoveListener(this@MainFragment)
            setOnMarkerClickListener(this@MainFragment)
            setOnMapClickListener(this@MainFragment)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        runWithLocationPermission {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationLatLng = LatLng(it.latitude, it.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLng(locationLatLng))
                }
            }
        }
    }

    override fun onCameraMoveStarted(reason: Int) {
        uiStateHandler.updateUILatLngCoords(map.cameraPosition.target)
    }

    override fun onCameraMove() {
        uiStateHandler.updateUILatLngCoords(map.cameraPosition.target)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        map.animateCamera(CameraUpdateFactory.newLatLng(marker?.position), 500, null)

        if (!uiStateHandler.addNewMarkerIsActive.get()) {
            val activeMarkerId = marker?.tag as Long
            markersViewModel.setActiveMarkerId(activeMarkerId)
        }
        return true
    }

    override fun onMapClick(position: LatLng?) {
        markersViewModel.setActiveMarkerId(null)
    }

    private fun runWithLocationPermission(block: () -> Unit) {
        val activity = requireActivity()
        val hasGrantedPermission = ContextCompat.checkSelfPermission(
            activity, ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
        if (!hasGrantedPermission) {
            // TODO: if request was previously denied, show dialog explaining why permission is
            //  required and ask permission from there using block below
            // val showRequestRationale =
            // ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
            // if (showRequestRationale) { return }
            ActivityCompat.requestPermissions(
                activity, arrayOf(ACCESS_FINE_LOCATION), RC_ACCESS_FINE_LOCATION
            )
            // Assigned to run in [onRequestPermissionsResult] after user grants location permission.
            pendingLocationPermissionAction = block
            return
        }
        block()
    }

    fun handleConfirmAddMarkerBtnClick() {
        val cameraPosition = map.cameraPosition.target
        markersViewModel.addMarker(cameraPosition)
        uiStateHandler.updateAddMarkerIsActive(false)
    }

    fun handleAddMarkerBtnClick() {
        markersViewModel.setActiveMarkerId(null)
        uiStateHandler.updateAddMarkerIsActive(true)
    }

    @SuppressLint("MissingPermission")
    fun handleMyLocationBtnClick() {
        runWithLocationPermission {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationLatLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLng(locationLatLng), 500, null)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            RC_ACCESS_FINE_LOCATION -> {
                val hasGrantedLocationPermission = permissions.size == 1 &&
                        permissions[0] == ACCESS_FINE_LOCATION &&
                        grantResults[0] == PERMISSION_GRANTED
                if (!hasGrantedLocationPermission) {
                    // TODO: show error because permission is not granted
                    return
                }
                pendingLocationPermissionAction()
            }
        }
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
