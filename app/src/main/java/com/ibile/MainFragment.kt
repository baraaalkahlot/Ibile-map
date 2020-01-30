package com.ibile

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.*
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.ibile.UIStateHandler.Overlay
import com.ibile.core.*
import com.ibile.databinding.FragmentMainBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.koin.androidx.scope.lifecycleScope
import org.koin.core.parameter.parametersOf

class ViewStatePreSelectSearchResult

class MainFragment : BaseFragment(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener {

    private var pendingLocationPermissionAction: () -> Unit = {}

    private lateinit var binding: FragmentMainBinding
    private lateinit var mapView: MapView
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val markersViewModel: MarkersViewModel by activityViewModel()
    private val uiStateHandler: UIStateHandler by lifecycleScope.inject { parametersOf(binding) }
    private val locationSearchHandler: LocationSearchHandler by lazy {
        requireActivity().lifecycleScope.get<LocationSearchHandler>()
    }
    private val polylineMarkerHandler: AddPolylineMarkerHandler by lifecycleScope.inject {
        parametersOf(requireContext())
    }

    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    private var viewStatePreSelectSearchResult: ViewStatePreSelectSearchResult? = null

    private var activeMapPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        markersViewModel.init()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding.mapView, savedInstanceState)

        binding.fragment = this
        binding.uiStateHandler = uiStateHandler
        binding.locationSearchHandler = locationSearchHandler
        binding.addPolylineMarkerHandler = polylineMarkerHandler

        return binding.root
    }

    private fun initializeMapView(mv: MapView, savedInstanceState: Bundle?) {
        mapView = mv
        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_KEY_MAP_VIEW)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        uiStateHandler.repositionLocationCompass(mapView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiStateHandler.updateBinding(binding)
        initializeLocationSearch()
        with(markersViewModel) {
            //            asyncSubscribe(MarkersViewModelState::markersAsync) { addMarkersToMap(it) }
            selectSubscribe(MarkersViewModelState::activeMarkerId) {
                toggleMarkerInfoView()
                restoreViewStatePreSelectSearchResult(it)
            }
        }
    }

    private fun initializeLocationSearch() {
        with(binding.searchLocationsView.rvSearchPlacesResults) {
            adapter =
                LocationSearchResultsAdapter(itemClickListener = handleLocationSearchResultItemClick)
        }

        locationSearchHandler.searchLocationsResponseObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val adapter = binding.searchLocationsView.rvSearchPlacesResults.adapter
                (adapter as LocationSearchResultsAdapter).updateSearchResults(it)
            }, {
                it.printStackTrace()
            })
            .addTo(compositeDisposable)
    }

    private val handleLocationSearchResultItemClick = { clickedItemResult: AutocompletePrediction ->
        viewStatePreSelectSearchResult = ViewStatePreSelectSearchResult()
        val action =
            MainFragmentDirections
                .actionMainFragmentToLocationSearchSelectedResultFragment(clickedItemResult.placeId)
        findNavController().navigate(action)
    }

    private fun addMarkersToMap(markers: List<com.ibile.data.database.entities.Marker>) {
        map.clear()
        val activeMarkerId = withState(markersViewModel) { it.activeMarkerId }
        markers.forEach {
            if (it.isPointMarker) {
                val markerOptions = MarkerOptions().position(it.position)
                val marker = map.addMarker(markerOptions)
                marker.tag = it.id
            }
            if (it.isPolylineMarker) {
                val DEFAULT_COLOR = Color.rgb(204, 54, 43)
                val options = PolylineOptions()
                    .addAll(it.points).color(DEFAULT_COLOR).width(3f).clickable(true)
                val polyline = map.addPolyline(options)
                if (it.id == activeMarkerId) {
                    activeMapPolyline?.width = 3f
                    polyline.width = 5f
                    activeMapPolyline = polyline
                }
                polyline.tag = it.id
            }
        }
    }

    private fun restoreViewStatePreSelectSearchResult(newMarkedId: Long?) {
        if (viewStatePreSelectSearchResult == null) return
        if (newMarkedId != null) {
            uiStateHandler.updateActiveOverlay(Overlay.NONE)
            binding.mapActionBar.etSearchLocation.setText("")
            viewStatePreSelectSearchResult = null
        }
    }

    private fun toggleMarkerInfoView() {
        val activeMarker = markersViewModel.getActiveMarker()?.apply {
            map.animateCamera(this.cameraUpdate, 500, null)
        }

        toggleActiveMarkerIndication(activeMarker)
        uiStateHandler.toggleMarkerInfoView(activeMarker)
    }

    private fun toggleActiveMarkerIndication(marker: com.ibile.data.database.entities.Marker?) {
        if (marker == null) {
            activeMapPolyline?.width = 3f
            activeMapPolyline = null
        } else {
            activeMapPolyline?.width = 6f
        }
    }

    override fun epoxyController(): MvRxEpoxyController =
        simpleController(markersViewModel) { markersViewModelState ->
            markerListPropertyObserverView {
                id(MarkerListPropertyObserverView.id)
                markersViewModelState.markersAsync()?.let { data(it) }
                dataCallback { addMarkersToMap(it) }
            }
        }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        if (uiStateHandler.activeOverlay.get() == Overlay.ADD_POLYLINE_MARKER)
            polylineMarkerHandler.setMap(map)

        runWithLocationPermission { map.isMyLocationEnabled = true }
        if (viewStatePreSelectSearchResult == null) moveToDeviceLocation()
        with(this.map) {
            // using custom location button in layout
            uiSettings.isMyLocationButtonEnabled = false
            setOnCameraMoveStartedListener(this@MainFragment)
            setOnCameraMoveListener(this@MainFragment)
            setOnMarkerClickListener(this@MainFragment)
            setOnMapClickListener(this@MainFragment)
            setOnPolylineClickListener(this@MainFragment)
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToDeviceLocation() {
        runWithLocationPermission {
            fusedLocationClient.lastLocation
                .toObservable()
                .subscribe { location: Location? ->
                    location?.let {
                        val locationLatLng = LatLng(it.latitude, it.longitude)
                        map.moveCamera(CameraUpdateFactory.newLatLng(locationLatLng))
                    }
                }
                .addTo(compositeDisposable)
        }
    }

    override fun onCameraMoveStarted(reason: Int) {
        with(map.cameraPosition.target) {
            uiStateHandler.updateUILatLngCoords(this)
        }
    }

    override fun onCameraMove() {
        with(map.cameraPosition.target) {
            uiStateHandler.updateUILatLngCoords(this)
            if (uiStateHandler.activeOverlay.get() == Overlay.ADD_POLYLINE_MARKER)
                polylineMarkerHandler.onMapMove(this)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (uiStateHandler.activeOverlay.get() != Overlay.NONE) return true

        // remove indication on active polyline marker if it is showing
        activeMapPolyline?.width = 3f
        activeMapPolyline = null

        val activeMarkerId = marker.tag as Long
        markersViewModel.setActiveMarkerId(activeMarkerId)

        return true
    }

    override fun onPolylineClick(polyline: Polyline) {
        if (uiStateHandler.activeOverlay.get() != Overlay.NONE) return

        // remove indication on active polyline marker if it is showing
        activeMapPolyline?.width = 3f
        activeMapPolyline = polyline

        val activeMarkerId = polyline.tag as Long
        markersViewModel.setActiveMarkerId(activeMarkerId)
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
        markersViewModel.addPointMarker(cameraPosition)
        uiStateHandler.updateActiveOverlay(Overlay.NONE)
    }

    fun handleSavePolylineMarkerBtnClick() {
        val points = polylineMarkerHandler.points.map { it?.position }
        markersViewModel.addPolylineMarker(points)
        uiStateHandler.updateActiveOverlay(Overlay.NONE)
        polylineMarkerHandler.reset()
    }

    fun handleAddMarkerBtnClick() {
        markersViewModel.setActiveMarkerId(null)
        uiStateHandler.updateActiveOverlay(Overlay.ADD_MARKER)
    }

    fun handleActionBarSearchBtnClick() {
        if (uiStateHandler.activeOverlay.get() == Overlay.SEARCH_LOCATION) return
        markersViewModel.setActiveMarkerId(null)
        locationSearchHandler.setCurrentSessionToken(AutocompleteSessionToken.newInstance())
        uiStateHandler.updateActiveOverlay(Overlay.SEARCH_LOCATION)
    }

    fun handleAddPolylineMarkerBtnClick() {
        polylineMarkerHandler.setMap(map)
        uiStateHandler.updateActiveOverlay(Overlay.ADD_POLYLINE_MARKER)
    }

    @SuppressLint("MissingPermission")
    fun handleMyLocationBtnClick() {
        runWithLocationPermission {
            fusedLocationClient.lastLocation
                .toObservable()
                .subscribe { location: Location? ->
                    location?.let {
                        val locationLatLng = LatLng(it.latitude, it.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLng(locationLatLng), 500, null)
                    }
                }
                .addTo(compositeDisposable)
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
        compositeDisposable.clear()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()

    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiStateHandler.updateBinding(null)
        polylineMarkerHandler.setMap(null)
    }

    companion object {
        const val BUNDLE_KEY_MAP_VIEW = "MapViewBundleKey"
        const val RC_ACCESS_FINE_LOCATION = 1001
    }
}
