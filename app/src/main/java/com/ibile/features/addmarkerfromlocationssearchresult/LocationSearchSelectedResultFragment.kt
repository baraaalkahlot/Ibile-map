package com.ibile.features.addmarkerfromlocationssearchresult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.databinding.FragmentLocationSearchSelectedResultBinding
import com.ibile.features.main.MainFragment
import com.ibile.features.mainexternaloverlays.MainExternalOverlaysDialogFragment

interface ViewBindingEvents {
    fun onClickCreateMarkerBtn()
    fun onClickBackBtn()

    val data: ObservableField<ViewData>
}

data class ViewData(val placeAsyncResult: Async<Place>)

class LocationSearchSelectedResultFragment : BaseDialogFragment(),
    ViewBindingEvents {
    private val parentView: ParentViewCallback
        get() = (parentFragment as MainExternalOverlaysDialogFragment)

    private val viewModel: LocationsSearchSelectedResultViewModel by fragmentViewModel()

    private lateinit var mapView: MapView
    private lateinit var map: GoogleMap

    override val data = ObservableField<ViewData>(ViewData(Uninitialized))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentLocationSearchSelectedResultBinding
            .inflate(inflater)
            .apply {
                eventHandlers = this@LocationSearchSelectedResultFragment
                this@LocationSearchSelectedResultFragment.mapView = this.mapView
                initMapView(savedInstanceState)
            }
            .root
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        val mapViewBundle = savedInstanceState?.getBundle(MainFragment.BUNDLE_MAP_VIEW_KEY)
        this.mapView.onCreate(mapViewBundle)
        this.mapView.getMapAsync { onMapReady(it) }
    }

    private fun onMapReady(map: GoogleMap) {
        this.map = map
        val fetchPlaceRequest = FetchPlaceRequest
            .builder(
                parentView.selectedResultId,
                arrayListOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            )
            .setSessionToken(parentView.sessionToken)
            .build()
        viewModel.fetchPlace(fetchPlaceRequest)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.asyncSubscribe(
            LocationsSearchSelectedResultViewModelState::fetchPlaceAsyncResult,
            onSuccess = this::handleFetchPlaceSuccess
        )
        viewModel.asyncSubscribe(
            LocationsSearchSelectedResultViewModelState::addMarkerAsyncResult,
            onSuccess = this::handleAddMarkerSuccess
        )
    }

    private fun handleAddMarkerSuccess(markerId: Long) {
        parentView.onAddMarkerSuccess(markerId)
    }

    private fun handleFetchPlaceSuccess(place: Place) {
        map.moveCamera(CameraUpdateFactory.newLatLng(place.latLng))
        map.addMarker(MarkerOptions().position(place.latLng).title(place.name)).showInfoWindow()
    }

    override fun onClickBackBtn() {
        parentView.onBackBtnClicked()
    }

    override fun onClickCreateMarkerBtn() {
        val place = viewModel.state.fetchPlaceAsyncResult()!!
        viewModel.addMarker(place.latLng!!)
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        setViewData()
    }

    private fun setViewData() {
        val newResult = viewModel.state.fetchPlaceAsyncResult
        val viewData = data.get()
        if (viewData?.placeAsyncResult != newResult)
            data.set(viewData?.copy(placeAsyncResult = newResult))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MainFragment.BUNDLE_MAP_VIEW_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MainFragment.BUNDLE_MAP_VIEW_KEY, mapViewBundle)
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

    interface ParentViewCallback {
        fun onAddMarkerSuccess(markerId: Long)
        fun onBackBtnClicked()
        val selectedResultId: String
        val sessionToken: AutocompleteSessionToken
    }
}
