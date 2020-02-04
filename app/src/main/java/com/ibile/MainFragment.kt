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
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.Polygon
import com.google.android.libraries.maps.model.Polyline
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.ibile.UIStateViewModel.Overlay
import com.ibile.core.*
import com.ibile.databinding.FragmentMainBinding
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BaseFragment(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private var pendingLocationPermissionAction: () -> Unit = {}

    private lateinit var binding: FragmentMainBinding
    private lateinit var mapView: MapView
    private lateinit var map: GoogleMap
    private val fusedLocationClient: FusedLocationProviderClient by inject()

    private val markersViewModel: MarkersViewModel by activityViewModel()
    private val locationSearchViewModel: LocationSearchViewModel by activityViewModel()
    private val addShapeViewModel: AddShapeViewModel by viewModel()
    private val uiStateViewModel: UIStateViewModel by fragmentViewModel()

    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }

    private var mapController: MapController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) markersViewModel.init()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding.mapView, savedInstanceState)

        binding.fragment = this
        binding.uiStateHandler = uiStateViewModel
        binding.locationSearchHandler = locationSearchViewModel
        binding.addPolylineMarkerHandler = addShapeViewModel

        return binding.root
    }

    private fun initializeMapView(mv: MapView, savedInstanceState: Bundle?) {
        mapView = mv
        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_MAP_VIEW_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        uiStateViewModel.repositionLocationCompass(mapView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(markersViewModel) {
            selectSubscribe(MarkersViewModelState::activeMarkerId) { toggleMarkerInfoView() }
            asyncSubscribe(MarkersViewModelState::addMarkerFromLocationSearchAsync) {
                if (uiStateViewModel.activeOverlay == Overlay.SEARCH_LOCATION) {
                    uiStateViewModel.updateActiveOverlay(Overlay.NONE)
                    locationSearchViewModel.setSearchQuery("")
                }
            }
        }
    }

    private fun toggleMarkerInfoView() {
        val activeMarker = markersViewModel.getActiveMarker()
        mapController?.toggleActiveMarkerIndication(activeMarker)
        uiStateViewModel.setActiveMarker(activeMarker)
    }

    override fun epoxyController(): MvRxEpoxyController =
        simpleController(
            locationSearchViewModel,
            markersViewModel,
            uiStateViewModel
        ) { locationSearchState, markersState, uiState ->
            mapController?.buildModels(epoxyController)
            when (uiState.activeListView) {
                UIStateViewModel.ListView.SEARCH_LOCATION -> {
                    val (_, searchPlacesResultAsync) = locationSearchState
                    searchPlacesResultsState {
                        id("PlacesResultView")
                        isLoading(searchPlacesResultAsync is Loading)
                        isSuccess(searchPlacesResultAsync is Success && searchPlacesResultAsync().isNotEmpty())
                        searchPlacesResultAsync(searchPlacesResultAsync)
                    }
                    searchPlacesResultAsync()?.forEach {
                        placesResultItem {
                            id(it.placeId)
                            prediction(it)
                            onClick { _ ->
                                val action = MainFragmentDirections
                                    .actionMainFragmentToLocationSearchSelectedResultFragment(it.placeId)
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
                UIStateViewModel.ListView.BROWSE_MARKERS -> {
                    if (uiStateViewModel.activeOverlay == Overlay.BROWSE_MARKERS) {
                        val (markersAsync) = markersState
                        markerFolderTitle {
                            id("Default Folder")
                            text("Default folder")
                        }
                        markersAsync()?.forEach { marker ->
                            markerItem {
                                id(marker.id)
                                marker(marker)
                                onClick { _ ->
                                    markersViewModel.setActiveMarkerId(marker.id)
                                    uiStateViewModel.activeOverlay = Overlay.NONE
                                }
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        mapController = MapController(map, requireContext(), markersViewModel)
        if (uiStateViewModel.activeOverlay == Overlay.ADD_POLY_SHAPE) {
            addShapeViewModel.setMap(map)
        }

        runWithLocationPermission { map.isMyLocationEnabled = true }
        moveToLastKnownLocation()
        with(this.map) {
            // using custom location button in layout
            uiSettings.isMyLocationButtonEnabled = false
            setOnCameraMoveStartedListener(this@MainFragment)
            setOnCameraMoveListener(this@MainFragment)
            setOnMarkerClickListener(this@MainFragment)
            setOnMapClickListener(this@MainFragment)
            setOnPolylineClickListener(this@MainFragment)
            setOnPolygonClickListener(this@MainFragment)
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToLastKnownLocation() {
        if (withState(markersViewModel) { it.activeMarkerId } != null) return
        if (uiStateViewModel.cameraPosition != null)
            return map.moveCamera(CameraUpdateFactory.newLatLng(uiStateViewModel.cameraPosition))

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
            uiStateViewModel.onCameraMove(this)
        }
    }

    override fun onCameraMove() {
        with(map.cameraPosition.target) {
            uiStateViewModel.onCameraMove(this)
            if (uiStateViewModel.activeOverlay == Overlay.ADD_POLY_SHAPE)
                addShapeViewModel.onMapMove(this)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (uiStateViewModel.activeOverlay != Overlay.NONE) return true
        mapController?.onMarkerClick(marker)
        return true
    }

    override fun onPolylineClick(polyline: Polyline) {
        if (uiStateViewModel.activeOverlay != Overlay.NONE) return
        mapController?.onPolylineClick(polyline)
    }

    override fun onPolygonClick(polygon: Polygon) {
        if (uiStateViewModel.activeOverlay != Overlay.NONE) return
        mapController?.onPolygonClick(polygon)
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
        uiStateViewModel.updateActiveOverlay(Overlay.NONE)
    }

    fun handleSaveShapeBtnClick() {
        val points = addShapeViewModel.points.map { it?.position }
        when (addShapeViewModel.polyType) {
            AddShapeViewModel.PolyType.POLYLINE -> markersViewModel.addPolyline(points)
            AddShapeViewModel.PolyType.POLYGON -> markersViewModel.addPolygon(points)
        }
        uiStateViewModel.updateActiveOverlay(Overlay.NONE)
        addShapeViewModel.reset()
    }

    fun handleAddMarkerBtnClick() {
        markersViewModel.setActiveMarkerId(null)
        uiStateViewModel.updateActiveOverlay(Overlay.ADD_MARKER)
    }

    fun handleActionBarSearchBtnClick() {
        if (uiStateViewModel.activeOverlay == Overlay.SEARCH_LOCATION) return
        markersViewModel.setActiveMarkerId(null)
        locationSearchViewModel.setCurrentSessionToken(AutocompleteSessionToken.newInstance())
        uiStateViewModel.updateActiveOverlay(Overlay.SEARCH_LOCATION)
    }

    fun handleActionBarBrowseBtnClick() {
        if (uiStateViewModel.activeOverlay == Overlay.BROWSE_MARKERS) return
        markersViewModel.setActiveMarkerId(null)
        uiStateViewModel.updateActiveOverlay(Overlay.BROWSE_MARKERS)
    }

    fun handleAddPolylineMarkerBtnClick() {
        addShapeViewModel.init(map, AddShapeViewModel.PolyType.POLYLINE)
        uiStateViewModel.updateActiveOverlay(Overlay.ADD_POLY_SHAPE)
    }

    fun handleAddPolygonMarkerBtnClick() {
        addShapeViewModel.init(map, AddShapeViewModel.PolyType.POLYGON)
        uiStateViewModel.updateActiveOverlay(Overlay.ADD_POLY_SHAPE)
    }

    @SuppressLint("MissingPermission")
    fun handleMyLocationBtnClick() {
        runWithLocationPermission {
            fusedLocationClient.lastLocation
                .toObservable()
                .subscribe { location: Location? ->
                    location?.let {
                        val update =
                            CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))
                        map.animateCamera(update, 500, object : GoogleMap.CancelableCallback {
                            override fun onFinish() {
                                uiStateViewModel.locationButtonIsActive.set(true)
                            }

                            override fun onCancel() {}
                        })
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
        var mapViewBundle = outState.getBundle(BUNDLE_MAP_VIEW_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(BUNDLE_MAP_VIEW_KEY, mapViewBundle)
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
        compositeDisposable.clear()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapController = null
        addShapeViewModel.setMap(null)
    }

    companion object {
        const val BUNDLE_MAP_VIEW_KEY = "BUNDLE_MAP_VIEW_KEY"
        const val RC_ACCESS_FINE_LOCATION = 1001
    }
}
