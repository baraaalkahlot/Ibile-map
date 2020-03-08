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
import com.ibile.features.editmarker.EditMarkerDialogFragment
import com.ibile.features.main.UIStateViewModel.Overlay
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiDatabindingViewData
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiPresenter
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiViewModel
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel
import com.ibile.features.main.addpolylinepoi.AddPolyLinePoiDatabindingViewData
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiPresenter
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiViewModel
import com.ibile.features.main.markerslist.MarkerInfoDatabindingViewData
import com.ibile.features.main.markerslist.MarkersPresenter
import com.ibile.features.main.markerslist.MarkersViewModel
import com.ibile.features.mainexternaloverlays.UIStateViewModel.CurrentView
import com.ibile.utils.extensions.navController
import com.ibile.utils.extensions.runWithPermissions
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BaseFragment(), MarkerImagesPreviewFragment.Callback,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
    GoogleMap.OnCameraMoveListener, EditMarkerDialogFragment.Callback {

    private lateinit var binding: FragmentMainBinding
    private lateinit var mapView: MapView
    private val fusedLocationClient: FusedLocationProviderClient by inject()

    private val uiStateViewModel: UIStateViewModel by fragmentViewModel()
    private val mainPresenter: MainPresenter by lazy {
        MainPresenter(uiStateViewModel, fusedLocationClient)
    }

    private val addPolygonPoiViewModel: AddPolygonPoiViewModel by viewModel()

    private val addPolylinePoiViewModel: AddPolylinePoiViewModel by fragmentViewModel()
    private val addPolylinePoiPresenter: AddPolylinePoiPresenter by lazy {
        AddPolylinePoiPresenter(addPolylinePoiViewModel)
    }

    private val addMarkerPoiViewModel: AddMarkerPoiViewModel by fragmentViewModel()
    private val addMarkerPoiPresenter: AddMarkerPoiPresenter by lazy {
        AddMarkerPoiPresenter(addMarkerPoiViewModel)
    }

    private val markersViewModel: MarkersViewModel by fragmentViewModel()
    private val markersPresenter: MarkersPresenter by lazy {
        MarkersPresenter(markersViewModel, childFragmentManager)
    }

    private lateinit var map: GoogleMap

    override val mode: MarkerImagesPreviewFragment.Callback.Mode
        get() = markersPresenter.mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        markersPresenter.init()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding.mapView, savedInstanceState)

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
            it.setOnMarkerDragListener(markerDragListener)

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
            addMarkerPoiPresenter.onMapMove(cameraPosition)
    }

    private val markerDragListener = object : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragEnd(marker: com.google.android.libraries.maps.model.Marker) {

        }

        override fun onMarkerDragStart(marker: com.google.android.libraries.maps.model.Marker) {
            updateActiveMarkerPoints()
        }

        override fun onMarkerDrag(marker: com.google.android.libraries.maps.model.Marker) {

        }

    }

    private val mainDataBindingViewData = object :
        MainDataBindingViewData {
        override fun handleMyLocationBtnClick() {
            mainPresenter.onClickMyLocationBtn(this@MainFragment::runWithPermissions)
        }

        override fun handleAddMarkerBtnClick() {
            addMarkerPoiPresenter.init(map)
            markersPresenter.onClickAddMarker()
            uiStateViewModel.updateActiveOverlay(Overlay.AddMarkerPoi(AddMarkerPoiPresenter.Mode.Add))
        }

        override fun handleAddPolylineBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.AddPolylinePoi(AddPolylinePoiPresenter.Mode.Add))
            addPolylinePoiPresenter.init(this@MainFragment, map)
        }

        override fun handleAddPolygonBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.AddPolygonPoi(AddPolygonPoiViewModel.Mode.Add))
            addPolygonPoiViewModel.init(map)
        }

        override val data: UIStateViewModel by lazy { uiStateViewModel }
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

    private val actionBarDatabindingViewData = object :
        ActionBarDatabindingViewData {
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

    private val addMarkerPoiDatabindingViewData = object :
        AddMarkerPoiDatabindingViewData {
        override fun handleOkBtnClick() {
            addMarkerPoiPresenter.onClickOkBtn(map)
        }

        // TODO: should only be for addMarkerPoiPresenter, but onBackPressed will use the same
        //  functionality
        override fun handleCancelBtnClick() {
            addMarkerPoiPresenter.onCancel(binding.addMarkerView)
            addPolylinePoiPresenter.onCancel()
            addPolygonPoiViewModel.onCancel()

            markersPresenter.onCancelAddOrEditMarkerPoints()
        }

        override val data by lazy { addMarkerPoiViewModel }
    }

    private val markerInfoDatabindingViewData = object :
        MarkerInfoDatabindingViewData {
        override val data: MarkersViewModel by lazy { markersViewModel }

        override fun handleEditBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            markersPresenter.onClickEditMarkerBtn()
        }

        override fun handleCopyBtnClick() {
            markersPresenter.onClickMarkerInfoCopyBtn(currentContext)
        }

        override fun handleNavigationBtnClick() {
            markersPresenter.onClickMarkerInfoNavigationBtn(currentContext)
        }

        override fun handleCallBtnClick() {
            markersPresenter.onClickMarkerInfoCallBtn(navController, currentContext)
        }

        override fun handleImageClick(index: Int) {
            markersPresenter.onClickMarkerInfoImage(index)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<ExternalOverlaysResult>(RESULT_FRAGMENT_EXTERNAL_OVERLAY)
            ?.observe(viewLifecycleOwner) { result ->
                markersPresenter.onExternalOverlayResult(result)
                uiStateViewModel.updateActiveOverlay(Overlay.None)
            }
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        markersPresenter.buildModels(map, this, this@MainFragment::handleOnMarkerCreatedOrUpdated)
        if (uiStateViewModel.state.activeOverlay is Overlay.AddPolylinePoi)
            addPolylinePoiPresenter.buildModels(map, this)
    }

    private fun handleOnMarkerCreatedOrUpdated(marker: Marker) {
        if (uiStateViewModel.state.activeOverlay is Overlay.None) return

        uiStateViewModel.updateActiveOverlay(Overlay.None)

        addPolylinePoiPresenter.onCreateOrUpdateSuccess(marker)
        addPolygonPoiViewModel.onCreateOrUpdateSuccess(marker)
        addMarkerPoiPresenter.onCreateOrUpdateSuccess(marker, binding.addMarkerView)
        markersPresenter.onMarkerCreatedOrUpdated(marker)
    }

    override fun onComplete(markerId: Long?) {
        uiStateViewModel.updateActiveOverlay(Overlay.None)
        markersPresenter.onEditMarkerComplete(markerId)
    }

    override fun onEditMarkerDialogEditCoordinatesBtnClick() {
        updateActiveMarkerPoints()
    }

    private fun updateActiveMarkerPoints() {
        val marker = markersPresenter.onMarkerPointsUpdateInit()
        when {
            marker.isMarker -> {
                addMarkerPoiPresenter.initEditMarkerPoint(marker, binding.addMarkerView, map)
                uiStateViewModel
                    .updateActiveOverlay(Overlay.AddMarkerPoi(AddMarkerPoiPresenter.Mode.Edit(marker)))
            }
            marker.isPolyline -> {
                addPolylinePoiPresenter.initEditPoints(marker, map, this)
                uiStateViewModel.updateActiveOverlay(
                    Overlay.AddPolylinePoi(AddPolylinePoiPresenter.Mode.Edit(marker))
                )
            }
            marker.isPolygon -> {
                addPolygonPoiViewModel.initEditPoints(marker, map)
                uiStateViewModel.updateActiveOverlay(
                    Overlay.AddPolygonPoi(AddPolygonPoiViewModel.Mode.Edit(marker))
                )
            }
        }
    }

    override fun onMapClick(position: LatLng?) {
        markersPresenter.onMapClick()
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
        val activeOverlay = uiStateViewModel.state.activeOverlay
        if (activeOverlay !is Overlay.None) return
        markersPresenter.onClickMarker(id)
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
    }

    companion object {
        const val BUNDLE_MAP_VIEW_KEY = "BUNDLE_MAP_VIEW_KEY"
        const val RC_ACCESS_FINE_LOCATION = 1001
        const val RESULT_FRAGMENT_EXTERNAL_OVERLAY = "RESULT_SELECTED_MARKER_ID"

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
