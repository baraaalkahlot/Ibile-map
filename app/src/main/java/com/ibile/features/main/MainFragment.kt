package com.ibile.features.main

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.currentContext
import com.ibile.core.simpleController
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.FragmentMainBinding
import com.ibile.features.MarkerImagesPreviewFragment
import com.ibile.features.createimportedmarker.CreateImportedMarkerFragment
import com.ibile.features.editmarker.EditMarkerDialogFragment
import com.ibile.features.main.UIStateViewModel.Overlay
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiDatabindingViewData
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiPresenter
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiViewModel
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel
import com.ibile.features.main.addpolylinepoi.AddPolyLinePoiDatabindingViewData
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiPresenter
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiViewModel
import com.ibile.features.main.datasharing.DataSharingHandler
import com.ibile.features.main.datasharing.DataSharingViewModel
import com.ibile.features.main.datasharing.ShareOptionsDialogFragment
import com.ibile.features.main.folderlist.FolderListPresenter
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import com.ibile.features.main.folderlist.FoldersViewModel
import com.ibile.features.main.mapfiles.MapFilesOptionsContainerDialogFragment
import com.ibile.features.main.mapfiles.MapFilesController
import com.ibile.features.main.mapfiles.MapFilesViewModel
import com.ibile.features.main.markerslist.MarkerInfoDatabindingViewData
import com.ibile.features.main.markerslist.MarkersPresenter
import com.ibile.features.main.markerslist.MarkersViewModel
import com.ibile.features.mainexternaloverlays.UIStateViewModel.CurrentView
import com.ibile.features.markeractiontargetfolderselection.MarkerActionTargetFolderSelectionDialogFragment
import com.ibile.features.shared.subscriptionrequired.SubscriptionRequiredFragment
import com.ibile.utils.extensions.navController
import com.ibile.utils.extensions.runWithPermissions
import com.ibile.utils.views.OptionWithIconArrayAdapter
import kotlinx.android.parcel.Parcelize
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main Application Entry Screen.
 *
 * This Fragment consists of many sub-screens and thus employs a Presenter/Controller pattern for
 * delegating features of those sub-screens.
 *
 * While the Fragment looks like a god class, its major functionality is delegating to the Controller
 * classes and this is hardly unavoidable. Additionally, there are a lot of different arch patterns
 * employed by the features due to how complicated this main screen is. This also formed the basis of
 * how features across the app are implemented.
 *
 * An arch pattern that has however seemed to be efficient and easy to implement is based on MVI-pattern
 * and is implemented in later features including [DataSharingHandler] and [MapFilesController]. This
 * leaves a huge technical debt of refactoring the other features (across the app as well) to this
 * pattern.
 */
class MainFragment : SubscriptionRequiredFragment(), MarkerImagesPreviewFragment.Callback,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
    GoogleMap.OnCameraMoveListener, EditMarkerDialogFragment.Callback,
    MarkerActionTargetFolderSelectionDialogFragment.Callback, ShareOptionsDialogFragment.Callback,
    MapFilesOptionsContainerDialogFragment.Callback {

    private lateinit var binding: FragmentMainBinding
    private lateinit var mapView: MapView
    private val fusedLocationClient: FusedLocationProviderClient by inject()
    internal lateinit var map: GoogleMap

    private val drawerLayoutViewEpoxyController: MvRxEpoxyController by lazy {
        drawerLayoutViewEpoxyController()
    }



    private val addPolygonPoiViewModel: AddPolygonPoiViewModel by viewModel()

    private val addPolylinePoiViewModel: AddPolylinePoiViewModel by fragmentViewModel()
    private val addPolylinePoiPresenter: AddPolylinePoiPresenter by lazy {
        AddPolylinePoiPresenter(addPolylinePoiViewModel)
    }

    private val addMarkerPoiViewModel: AddMarkerPoiViewModel by fragmentViewModel()
    private val addMarkerPoiPresenter: AddMarkerPoiPresenter by lazy {
        AddMarkerPoiPresenter(addMarkerPoiViewModel, childFragmentManager)
    }

    private val markersViewModel: MarkersViewModel by fragmentViewModel()
    private val markersPresenter: MarkersPresenter by lazy {
        MarkersPresenter(markersViewModel, childFragmentManager)
    }

    private val foldersViewModel: FoldersViewModel by fragmentViewModel()
    private val folderListPresenter: FolderListPresenter by lazy {
        FolderListPresenter(childFragmentManager, foldersViewModel)
    }

    // TODO: use koin lifecycle to inject this
    // viewmodel should be creatable from handler
    private val dataSharingViewModel: DataSharingViewModel by fragmentViewModel()
    private val dataSharingHandler: DataSharingHandler by lazy {
        DataSharingHandler(this, dataSharingViewModel)
    }


    private val uiStateViewModel: UIStateViewModel by fragmentViewModel()
    private val mainPresenter: MainPresenter by lazy {
        MainPresenter(uiStateViewModel, fusedLocationClient)
    }

    private val mapFilesViewModel: MapFilesViewModel by fragmentViewModel()
    private val mapFilesController: MapFilesController by lazy {
        MapFilesController(this, mapFilesViewModel)
    }

    override val mode: MarkerImagesPreviewFragment.Callback.Mode
        get() = markersPresenter.mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: feed this in through a VM/repo
        if (FirebaseAuth.getInstance().currentUser == null) {
            MainFragmentDirections.actionMainFragmentToAuthGraph().navigate()
            return
        }
        drawerLayoutViewEpoxyController.onRestoreInstanceState(savedInstanceState)
        markersPresenter.init()
        folderListPresenter.init()
        Log.d("CHECKING OTHERS", "TO know what calls")
    }

     fun getInstance(): MainFragment {
         return this
      }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        initializeMapView(binding.mapView, savedInstanceState)

        binding.handler = mainDataBindingViewData
        binding.actionBar.handler = actionBarDatabindingViewData

        // TODO: move this to its own interface and set in layout
        binding.drawerView.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.drawerView, true)
        }

        binding.markerInfoView.handler = markerInfoDatabindingViewData
        binding.drawerRecyclerview.setController(drawerLayoutViewEpoxyController)

        binding.addMarkerView.handler = addMarkerPoiDatabindingViewData
        binding.partialAddPolylinePoi.handler = addPolyLinePoiDatabindingViewData
        binding.partialAddPolygonPoi.addShapeViewModel = addPolygonPoiViewModel



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
        when (uiStateViewModel.state.activeOverlay) {
            is Overlay.AddMarkerPoi -> addMarkerPoiPresenter.onMapMove(cameraPosition)
            is Overlay.AddPolylinePoi -> addPolylinePoiPresenter.onMapMove(cameraPosition)
            is Overlay.AddPolygonPoi -> addPolygonPoiViewModel.onMapMove(cameraPosition)
        }
    }

    private val markerDragListener = object : GoogleMap.OnMarkerDragListener {
        private var marker: Marker? = null

        override fun onMarkerDragEnd(mapMarker: com.google.android.libraries.maps.model.Marker) {
            mapMarker.position = marker?.position
        }

        // long clicking a marker calls this method, and the map marker instance gets hidden
        // through markersPresenter since markers position won't be changed through dragging it but
        // through editMarkerPresenter (addMarkerPresenter). This causes onMarkerDragEnd to be called
        // after marker is hidden.
        // The map marker instance's position shifts a bit from the original position, however
        // (GoogleMap's behaviour). Position, therefore, has to be set back to the apps [Marker]'s
        // position in onMarkerDragEnd
        override fun onMarkerDragStart(mapMarker: com.google.android.libraries.maps.model.Marker) {
            markersPresenter.onMarkerPointsUpdateInit(mapMarker.tag as Long).apply {
                marker = this
                initEditMarkerPointsUpdate(this)
            }
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
            binding.drawerLayout.openDrawer(binding.drawerView)
        }

        override fun handleBrowseMarkersBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            val action = MainFragmentDirections
                .actionMainFragmentToMainExternalOverlaysDialogFragment(CurrentView.BrowseMarkers)
            navController.navigate(action)
        }

        override fun handleOrganizeMarkersBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            val action = MainFragmentDirections
                .actionMainFragmentToMainExternalOverlaysDialogFragment(CurrentView.OrganizeMarkers)
            navController.navigate(action)
        }

        override fun handleSearchBtnClick() {
            uiStateViewModel.updateActiveOverlay(Overlay.ExternalOverlay)
            val action = MainFragmentDirections
                .actionMainFragmentToMainExternalOverlaysDialogFragment(CurrentView.LocationsSearch)
            navController.navigate(action)
        }

        override fun handleShareBtnClick() {


            val activeMarkerId = markersPresenter.onClickActionBarShareBtn()
            dataSharingHandler.init(activeMarkerId)

        }
    }


    private val addMarkerPoiDatabindingViewData = object :
        AddMarkerPoiDatabindingViewData {
        override fun handleOkBtnClick() {
            addMarkerPoiPresenter.onClickOkBtn()
        }

        // TODO: should only be for addMarkerPoiPresenter, but onBackPressed will use the same
        //  functionality
        override fun handleCancelBtnClick() {
            addMarkerPoiPresenter.onCancel()
            addPolylinePoiPresenter.onCancel()
            addPolygonPoiViewModel.onCancel()

            markersPresenter.onCancelAddOrEditMarkerPoints()
            uiStateViewModel.updateActiveOverlay(Overlay.None)
        }

        override fun handleTargetFolderBtnClick() {
            addMarkerPoiPresenter.onClickMarkerTargetFolder()
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
            markersPresenter.onClickMarkerInfoNavigationBtn()
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

        with(navController.currentBackStackEntry!!.savedStateHandle) {
            getLiveData<ExternalOverlaysResult>(RESULT_FRAGMENT_EXTERNAL_OVERLAY)
                .observe(viewLifecycleOwner) {
                    uiStateViewModel.updateActiveOverlay(Overlay.None)
                    markersPresenter.onExternalOverlayResult(it)
                }

            getLiveData<Long>(CreateImportedMarkerFragment.RESULT_KEY_CREATED_MARKER_ID)
                .observe(viewLifecycleOwner) {
                    markersPresenter.onMarkerCreatedOrUpdated(it)
                }
        }
    }

    override fun invalidate() {
        super.invalidate()
        binding.drawerRecyclerview.requestModelBuild()
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        markersPresenter.buildModels(map, this, this@MainFragment::handleOnMarkerCreatedOrUpdated)
        if (uiStateViewModel.state.activeOverlay is Overlay.AddPolylinePoi)
            addPolylinePoiPresenter.buildModels(map, this)
    }

    private fun handleOnMarkerCreatedOrUpdated(marker: Marker) {
        val activeOverlay = uiStateViewModel.state.activeOverlay
        if (activeOverlay is Overlay.None || activeOverlay is Overlay.ExternalOverlay) return

        uiStateViewModel.updateActiveOverlay(Overlay.None)

        addPolylinePoiPresenter.onCreateOrUpdateSuccess()
        addPolygonPoiViewModel.onCreateOrUpdateSuccess()
        addMarkerPoiPresenter.onCreateOrUpdateSuccess()
        markersPresenter.onMarkerCreatedOrUpdated(marker.id)
    }

    private fun drawerLayoutViewEpoxyController() = simpleController {
        mapFilesController.buildModels(this)
        folderListPresenter.buildModels(this)
    }

    override fun onComplete(markerId: Long?) {
        uiStateViewModel.updateActiveOverlay(Overlay.None)
        markersPresenter.onEditMarkerComplete(markerId)
    }

    override fun onEditMarkerDialogEditCoordinatesBtnClick() {
        val marker = markersPresenter.onMarkerPointsUpdateInit()
        initEditMarkerPointsUpdate(marker)
    }

    private fun initEditMarkerPointsUpdate(marker: Marker) {
        val overlay = when {
            marker.isMarker -> {
                addMarkerPoiPresenter.initEditMarkerPoint(marker, map)
                Overlay.AddMarkerPoi(AddMarkerPoiPresenter.Mode.Edit)
            }
            marker.isPolyline -> {
                addPolylinePoiPresenter.initEditPoints(marker, map, this)
                Overlay.AddPolylinePoi(AddPolylinePoiPresenter.Mode.Edit(marker))
            }
            marker.isPolygon -> {
                addPolygonPoiViewModel.initEditPoints(marker, map)
                Overlay.AddPolygonPoi(AddPolygonPoiViewModel.Mode.Edit(marker))
            }
            else -> Overlay.None
        }
        uiStateViewModel.updateActiveOverlay(overlay)
    }

    override fun onSelectTargetFolder(folderId: Long) {
        addMarkerPoiPresenter.onChooseMarkerTargetFolder(folderId)
    }

    override val markerActionTargetFolderSelectionDialogTitle: String
        get() = "Change folder to..."

    override val markerActionTargetFolderOptionsList: List<FolderWithMarkersCount>
        get() = addMarkerPoiPresenter.targetFolderOptionsList

    override val optionItems_ShareDataOptionsDialogFragment: List<OptionWithIconArrayAdapter.ItemOptionWithIcon>
        get() = dataSharingHandler.optionItems_ShareDataOptionsDialogFragment

    override val title_ShareDataOptionsDialogFragment: String
        get() = dataSharingHandler.title_ShareDataOptionsDialogFragment

    override fun onCancel_ShareDataOptionsDialogFragment() {
        dataSharingHandler.onCancel_ShareDataOptionsDialogFragment()
    }

    override fun onSelectOption_ShareDataOptionsDialogFragment(optionIndex: Int) {
        dataSharingHandler.onSelectOption_ShareDataOptionsDialogFragment(optionIndex)
    }

    override fun getDialog_FileOptionsDialogFragment(): Dialog {
        return mapFilesController.getDialog_FileOptionsDialogFragment()
    }

    override fun onCancelDialog_FileOptionsDialogFragment() {
        mapFilesController.onCancelDialog_FileOptionsDialogFragment()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dataSharingHandler.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        drawerLayoutViewEpoxyController.onSaveInstanceState(outState)
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
        drawerLayoutViewEpoxyController.cancelPendingModelBuild()
        super.onDestroyView()
        addPolygonPoiViewModel.setMap(null)
        with(navController.currentBackStackEntry!!.savedStateHandle) {
            remove<ExternalOverlaysResult>(RESULT_FRAGMENT_EXTERNAL_OVERLAY)
            remove<Long>(CreateImportedMarkerFragment.RESULT_KEY_CREATED_MARKER_ID)
        }
    }

    companion object {
        const val BUNDLE_MAP_VIEW_KEY = "BUNDLE_MAP_VIEW_KEY"
        const val RC_ACCESS_FINE_LOCATION = 1001
        const val RESULT_FRAGMENT_EXTERNAL_OVERLAY = "RESULT_SELECTED_MARKER_ID"

        sealed class ExternalOverlaysResult : Parcelable {
            @Parcelize
            class BrowseMarkers(val selectedMarkerId: Long?) : ExternalOverlaysResult()

            @Parcelize
            class LocationsSearch(val createdMarkerId: Long?) : ExternalOverlaysResult()

            @Parcelize
            object OrganizeMarkers : ExternalOverlaysResult()
        }
    }
}
