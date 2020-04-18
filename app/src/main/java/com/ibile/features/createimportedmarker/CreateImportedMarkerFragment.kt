package com.ibile.features.createimportedmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.MarkerOptions
import com.ibile.databinding.FragmentCreateImportedMarkerBinding
import com.ibile.utils.extensions.navController
import com.ibile.utils.extensions.popBackStackWithResult

class CreateImportedMarkerFragment : BaseMvRxFragment() {
    private lateinit var mapView: MapView
    private val args by navArgs<CreateImportedMarkerFragmentArgs>()
    private val viewModel: CreateImportedMarkerViewModel by fragmentViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentCreateImportedMarkerBinding.inflate(inflater, container, false)
            .apply {
                this@CreateImportedMarkerFragment.mapView = mapView
                btnBack.setOnClickListener { navController.navigateUp() }
                btnCreateMarker.setOnClickListener {
                    viewModel.handleCreateMarkerBtnClick(args.importedMarkerCoords)
                }
            }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initMapView(savedInstanceState)
        viewModel.asyncSubscribe(CreateImportedMarkerViewModel.State::createMarkerAsyncResult) {
            navController.popBackStackWithResult(RESULT_KEY_CREATED_MARKER_ID, it)
        }
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        val mapViewBundle = savedInstanceState?.getBundle(BUNDLE_MAP_VIEW_KEY)
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync { onMapReady(it) }
    }

    private fun onMapReady(map: GoogleMap) {
        map.addMarker(MarkerOptions().position(args.importedMarkerCoords).title("Imported marker"))
            .apply {
                showInfoWindow()
                map.moveCamera(CameraUpdateFactory.newLatLng(this.position))
            }
    }

    override fun invalidate() {
        /* no-op */
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val BUNDLE_MAP_VIEW_KEY = "BUNDLE_MAP_VIEW_KEY"
        const val RESULT_KEY_CREATED_MARKER_ID = "RESULT_KEY_CREATED_MARKER_ID"
    }
}
