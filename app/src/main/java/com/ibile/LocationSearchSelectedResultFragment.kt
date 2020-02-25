package com.ibile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.UniqueOnly
import com.airbnb.mvrx.activityViewModel
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.ibile.core.addTo
import com.ibile.databinding.FragmentLocationSearchSelectedResultBinding
import io.reactivex.disposables.CompositeDisposable

class LocationSearchSelectedResultFragment :
    BaseMvRxFragment(R.layout.fragment_location_search_selected_result), OnMapReadyCallback {

    private val args: LocationSearchSelectedResultFragmentArgs by navArgs()

    private lateinit var binding: FragmentLocationSearchSelectedResultBinding
    private lateinit var mapView: MapView

    private val markersViewModel: MarkersViewModel by activityViewModel()
    private val locationSearchViewModel: LocationSearchViewModel by activityViewModel()

    private var locationCoords: LatLng? = null
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            FragmentLocationSearchSelectedResultBinding.inflate(inflater, container, false)
        binding.btnCreateMarker.setOnClickListener { handleCreateMarkerBtnClick() }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.locationSearchHandler = locationSearchViewModel

        mapView = binding.mapView
        val mapViewBundle = savedInstanceState?.getBundle(MainFragment.BUNDLE_MAP_VIEW_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        markersViewModel.asyncSubscribe(
            MarkersViewModelState::addMarkerAsync, UniqueOnly(mvrxViewId)
        ) {
            findNavController().popBackStack(R.id.mainFragment, false)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        val placeId = args.argSelectedSearchPlaceId
        locationSearchViewModel.fetchPlace(placeId)
            .subscribe({ it ->
                it.data?.let {
                    locationCoords = it.latLng
                    map.moveCamera(CameraUpdateFactory.newLatLng(it.latLng))
                    val marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
                    marker.showInfoWindow()
                }
            }, { it.printStackTrace() })
            .addTo(compositeDisposable)
    }

    private fun handleCreateMarkerBtnClick() {
        locationCoords?.let { markersViewModel.addMarker(it) }
    }

    override fun invalidate() {
        // not required for now
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
}
