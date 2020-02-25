package com.ibile.features.main

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.databinding.ObservableField
import androidx.lifecycle.observe
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Polygon
import com.google.android.libraries.maps.model.Polyline
import com.ibile.core.BaseFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.FragmentMainBinding
import com.ibile.features.MarkerImagesPreviewFragment
import com.ibile.features.addpolylinepoi.AddPolyLinePoiDatabindingViewData
import com.ibile.features.addpolylinepoi.AddPolylinePoiPresenter
import com.ibile.features.addpolylinepoi.AddPolylinePoiViewModel
import com.ibile.features.main.UIStateViewModel.Overlay
import com.ibile.features.mainexternaloverlays.UIStateViewModel.CurrentView
import com.ibile.utils.extensions.navController
import com.ibile.utils.extensions.runWithPermissions
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

interface AddMarkerPoiDatabindingViewData {
    fun handleOkBtnClick()
    fun handleCancelBtnClick()
    fun handleAddPolylineBtnClick()
    fun handleAddPolyMarkerBtnClick()

    data class Data(val cameraPosition: LatLng = LatLng(0.0, 0.0))

    val data: ObservableField<Data>
}

interface MarkerInfoDatabindingViewData {
    fun handleEditBtnClick()
    fun handleCopyBtnClick()
    fun handleNavigationBtnClick()
    fun handleCallBtnClick()
    fun handleImageClick(index: Int)

    val data: ObservableField<Marker>
}

interface ActionBarDatabindingViewData {
    fun handleDrawerBtnClick()
    fun handleBrowseMarkersBtnClick()
    fun handleOrganizeMarkersBtnClick()
    fun handleSearchBtnClick()
    fun handleShareBtnClick()
}

interface MainDataBindingViewData {
    fun handleMyLocationBtnClick()
    fun handleAddMarkerBtnClick()

    val data: MainViewModel
}

class MainFragment : BaseFragment(), MarkerImagesPreviewFragment.Callback,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
    GoogleMap.OnCameraMoveListener {

    private lateinit var binding: FragmentMainBinding
    private lateinit var mapView: MapView
    private val fusedLocationClient: FusedLocationProviderClient by inject()

    private val mainViewModel: MainViewModel by fragmentViewModel()
    private val uiStateViewModel: UIStateViewModel by fragmentViewModel()

    private val addPolygonPoiViewModel: AddPolygonPoiViewModel by viewModel()

    private val addPolylinePoiViewModel: AddPolylinePoiViewModel by fragmentViewModel()
    private val addPolylinePoiPresenter: AddPolylinePoiPresenter by lazy {
        AddPolylinePoiPresenter(addPolylinePoiViewModel)
    }

    private val markerInfoPresenter: MarkerInfoPresenter by lazy {
        MarkerInfoPresenter(childFragmentManager)
    }
    private val mainPresenter: MainPresenter by lazy {
        MainPresenter(mainViewModel, fusedLocationClient)
    }

    private lateinit var map: GoogleMap

    override val mode: MarkerImagesPreviewFragment.Callback.Mode
        get() = markerInfoPresenter.mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainPresenter.init()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding.mapView, savedInstanceState)

        binding.uiStateViewModel = uiStateViewModel

        binding.handler = mainDataBindingViewData
        binding.actionBar.handler = actionBarDatabindingViewData
        binding.addMarkerView.handler = addMarkerPoiDatabindingViewData
        binding.partialAddPolylinePoi.handler = addPolyLinePoiDatabindingViewData
        binding.partialAddPolygonPoi.addShapeViewModel = addPolygonPoiViewModel
        binding.markerInfoView.handler = markerInfoDatabindingViewData

        return binding.root
    }

    private fun initializeMapView(mapView: MapView, savedInstanceState: Bundle?) {
        this.mapView = mapView

        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_MAP_VIEW_KEY)
        this.mapView.onCreate(mapViewBundle)
        this.mapView.getMapAsync {
            this.map = it

            it.setOnCameraMoveListener(this)
            it.setOnMarkerClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnPolylineClickListener(this)
            it.setOnPolygonClickListener(this)

            repositionLocationCompass(this.mapView)

            mainPresenter.onMapReady(it, this::runWithPermissions)
            addPolygonPoiViewModel.setMap(it)
        }
    }

    private fun repositionLocationCompass(mapView: MapView) {
        val locationCompass =
            (mapView.findViewById<View>("1".toInt()).parent as View).findViewById<View>("5".toInt())
        val layoutParams = (locationCompass.layoutParams as RelativeLayout.LayoutParams)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 200, 30, 0)
    }

    override fun onCameraMove() {
        val cameraPosition = map.cameraPosition.target
        mainPresenter.onCameraMove(cameraPosition)
        if (uiStateViewModel.state.activeOverlay is Overlay.AddPolylinePoi)
            addPolylinePoiPresenter.onMapMove(cameraPosition)
        if (uiStateViewModel.state.activeOverlay is Overlay.AddPolygonPoi)
            addPolygonPoiViewModel.onMapMove(cameraPosition)
        if (uiStateViewModel.state.activeOverlay is Overlay.AddMarkerPoi)
            addMarkerPoiDatabindingViewData.onMapMove(cameraPosition)
    }

    private val mainDataBindingViewData = object : MainDataBindingViewData {
        override fun handleMyLocationBtnClick() {
            mainPresenter.onClickMyLocationBtn(this@MainFragment::runWithPermissions)
        }

        override fun handleAddMarkerBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.AddMarkerPoi)
        }

        override val data: MainViewModel by lazy { mainViewModel }
    }

    private val addPolyLinePoiDatabindingViewData: AddPolyLinePoiDatabindingViewData =
        object : AddPolyLinePoiDatabindingViewData {
            override fun onClickAddBtn() {
                addPolylinePoiPresenter.onClickAddBtn(map)
            }

            override fun onClickPrevBtn() {
                addPolylinePoiPresenter.onClickPrevBtn(map)
            }

            override fun onClickNextBtn() {
                addPolylinePoiPresenter.onClickNextBtn(map)
            }

            override fun onClickRemoveBtn() {
                addPolylinePoiPresenter.onClickRemoveBtn(map)
            }

            override fun onClickSaveBtn() {
                addPolylinePoiPresenter.onClickSaveBtn()
            }

            override val data: ObservableField<AddPolyLinePoiDatabindingViewData.Data>
                get() = addPolylinePoiPresenter.databindingViewData
        }

    private val actionBarDatabindingViewData = object : ActionBarDatabindingViewData {
        override fun handleDrawerBtnClick() {

        }

        override fun handleBrowseMarkersBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            val action = MainFragmentDirections
                .actionMainFragmentToMainExternalOverlaysDialogFragment(CurrentView.BrowseMarkers())
            navController.navigate(action)
        }

        override fun handleOrganizeMarkersBtnClick() {

        }

        override fun handleSearchBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            val action = MainFragmentDirections
                .actionMainFragmentToMainExternalOverlaysDialogFragment(CurrentView.LocationsSearch())
            navController.navigate(action)
        }

        override fun handleShareBtnClick() {

        }
    }


    private val addMarkerPoiDatabindingViewData = object : AddMarkerPoiDatabindingViewData {
        override fun handleOkBtnClick() {
            mainViewModel.addMarker(map.cameraPosition.target)
        }

        override fun handleCancelBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.None)
        }

        // TODO: this button belongs to the main view
        override fun handleAddPolylineBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.AddPolylinePoi)
            addPolylinePoiPresenter.init(this@MainFragment, map)
        }

        // TODO: this button belongs to the main view
        override fun handleAddPolyMarkerBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.AddPolygonPoi)
            addPolygonPoiViewModel.init(map, AddPolygonPoiViewModel.PolyType.POLYGON)
        }

        override val data: ObservableField<AddMarkerPoiDatabindingViewData.Data> = ObservableField()

        private fun updateData(newUpdate: AddMarkerPoiDatabindingViewData.Data.() -> AddMarkerPoiDatabindingViewData.Data) {
            data.set(newUpdate(data.get() ?: AddMarkerPoiDatabindingViewData.Data()))
        }

        fun onMapMove(cameraPosition: LatLng) {
            updateData { copy(cameraPosition = cameraPosition) }
        }
    }


    private val markerInfoDatabindingViewData = object : MarkerInfoDatabindingViewData {
        override val data: ObservableField<Marker> by lazy { markerInfoPresenter.data }

        override fun handleEditBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            markerInfoPresenter.handleEditBtnClick(navController)
        }

        override fun handleCopyBtnClick() {
            markerInfoPresenter.handleCopyBtnClick()
        }

        override fun handleNavigationBtnClick() {
            markerInfoPresenter.handleNavigationBtnClick()
        }

        override fun handleCallBtnClick() {
            markerInfoPresenter.handleCallBtnClick(navController, currentContext)
        }

        override fun handleImageClick(index: Int) {
            markerInfoPresenter.handleImageClick(index)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiStateViewModel.selectSubscribe(UIStateViewModelState::activeOverlay) {
            when (it) {
                is Overlay.None -> return@selectSubscribe
                is Overlay.MarkerInfo -> markerInfoPresenter.marker = it.markerId
                    ?.let { markerId -> mainViewModel.getMarkerById(markerId) }
                else -> markerInfoPresenter.marker = null
            }
        }

        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<ExternalOverlaysResult>(RESULT_FRAGMENT_EXTERNAL_OVERLAY)
            ?.observe(viewLifecycleOwner) { result ->
                onExternalOverlayResult(result)
                mainPresenter.onExternalOverlayResult(result)
            }

        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Long?>(RESULT_FRAGMENT_EDIT_MARKER)
            ?.observe(viewLifecycleOwner) {
                mainPresenter.onEditMarkerResult(it)
                handleExternalOverlayResult(it)
            }
    }

    private fun onExternalOverlayResult(result: ExternalOverlaysResult) {
        mainPresenter.onExternalOverlayResult(result)
        when (result) {
            is ExternalOverlaysResult.BrowseMarkers -> {
                handleExternalOverlayResult(result.selectedMarkerId)
            }
            is ExternalOverlaysResult.LocationsSearch -> {
                handleExternalOverlayResult(result.createdMarkerId)
            }
        }
    }

    private fun handleExternalOverlayResult(markerId: Long?) {
        val overlay = if (markerId == null) Overlay.None else Overlay.MarkerInfo(markerId)
        uiStateViewModel.updateActiveOverlay(overlay)
    }

    override fun onMapClick(position: LatLng?) {
        markerInfoPresenter.onMapClick()
        mainPresenter.onMapClick()
    }

    override fun onMarkerClick(marker: com.google.android.libraries.maps.model.Marker): Boolean {
        val markerId = marker.tag as Long
        onMarkerClick(markerId)
        return true
    }

    override fun onPolylineClick(polyline: Polyline) {
        val id = polyline.tag as Long
        onMarkerClick(id)
    }

    override fun onPolygonClick(polygon: Polygon) {
        val id = polygon.tag as Long
        onMarkerClick(id)
    }

    private fun onMarkerClick(id: Long) {
        if (listOf(Overlay.AddPolygonPoi, Overlay.AddPolylinePoi, Overlay.AddMarkerPoi)
                .contains(uiStateViewModel.state.activeOverlay)
        ) return
        uiStateViewModel.updateActiveOverlay(Overlay.MarkerInfo(id))
        mainPresenter.onMarkerClick(id)
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        mainPresenter.buildModels(this, this@MainFragment::handleOnMarkerAdded)
        if (uiStateViewModel.state.activeOverlay == Overlay.AddPolylinePoi)
            addPolylinePoiPresenter.buildModels(map, this)
    }

    private fun handleOnMarkerAdded(marker: Marker) {
        if (uiStateViewModel.state.activeOverlay is Overlay.None
            || uiStateViewModel.state.activeOverlay is Overlay.MarkerInfo
        ) return
        when (uiStateViewModel.state.activeOverlay) {
            is Overlay.AddPolylinePoi -> addPolylinePoiPresenter.onCreateMarkerSuccess(marker)
            is Overlay.AddPolygonPoi -> addPolygonPoiViewModel.onCreateMarkerSuccess(marker)
        }
        mainPresenter.onNewMarker(marker)
        uiStateViewModel.updateActiveOverlay(Overlay.MarkerInfo(marker.id))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RC_ACCESS_FINE_LOCATION -> {
                val hasGrantedLocationPermission = permissions.size == 1 &&
                        permissions[0] == ACCESS_FINE_LOCATION &&
                        grantResults[0] == PERMISSION_GRANTED
                if (!hasGrantedLocationPermission) {
                    // TODO: show error because permission is not granted
                    return
                }
                mainPresenter.onGrantLocationPermission()
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
        mainPresenter.onDestroyMap()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addPolygonPoiViewModel.setMap(null)
        navController.currentBackStackEntry?.savedStateHandle
            ?.remove<ExternalOverlaysResult>(RESULT_FRAGMENT_EXTERNAL_OVERLAY)
        navController.currentBackStackEntry?.savedStateHandle
            ?.remove<Long?>(RESULT_FRAGMENT_EDIT_MARKER)
    }

    companion object {
        const val BUNDLE_MAP_VIEW_KEY = "BUNDLE_MAP_VIEW_KEY"
        const val RC_ACCESS_FINE_LOCATION = 1001
        const val RESULT_FRAGMENT_EXTERNAL_OVERLAY = "RESULT_SELECTED_MARKER_ID"
        const val RESULT_FRAGMENT_EDIT_MARKER = "RESULT_FRAGMENT_EDIT_MARKER"

        sealed class ExternalOverlaysResult : Parcelable {
            class BrowseMarkers(val selectedMarkerId: Long?) : ExternalOverlaysResult() {
                constructor(parcel: Parcel) : this(parcel.readSerializable() as? Long)

                override fun writeToParcel(parcel: Parcel, flags: Int) {
                    parcel.writeSerializable(selectedMarkerId)
                }

                override fun describeContents(): Int {
                    return 0
                }

                companion object CREATOR : Parcelable.Creator<BrowseMarkers> {
                    override fun createFromParcel(parcel: Parcel): BrowseMarkers {
                        return BrowseMarkers(parcel)
                    }

                    override fun newArray(size: Int): Array<BrowseMarkers?> {
                        return arrayOfNulls(size)
                    }
                }
            }

            class LocationsSearch(val createdMarkerId: Long?) : ExternalOverlaysResult() {
                constructor(parcel: Parcel) : this(parcel.readSerializable() as? Long)

                override fun writeToParcel(parcel: Parcel, flags: Int) {
                    parcel.writeSerializable(createdMarkerId)
                }

                override fun describeContents(): Int {
                    return 0
                }

                companion object CREATOR : Parcelable.Creator<LocationsSearch> {
                    override fun createFromParcel(parcel: Parcel): LocationsSearch {
                        return LocationsSearch(parcel)
                    }

                    override fun newArray(size: Int): Array<LocationsSearch?> {
                        return arrayOfNulls(size)
                    }
                }
            }
        }
    }
}