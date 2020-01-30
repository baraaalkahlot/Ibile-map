package com.ibile

import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.marginBottom
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableField
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.animateSlideVertical
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.FragmentMainBinding

class UIStateHandler(private var binding: FragmentMainBinding?) {
    val activeOverlay = ObservableField<Overlay>(Overlay.NONE)
    val activeMarker = ObservableField<Marker>()
    val newMarkerCoordsValues = ObservableArrayMap<String, Float>()
        .apply {
            put("lat", 0f)
            put("lng", 0f)
        }

    fun handleActionBarBtnClick(overlay: Overlay) {
        updateActiveOverlay(overlay)
    }

    fun updateActiveOverlay(overlay: Overlay) {
        activeOverlay.set(overlay)
    }

    fun handleCancelAddMarkerBtnClick() {
        activeOverlay.set(Overlay.NONE)
    }

    fun actionBarIsVisible(activeOverlay: Overlay) =
        !arrayListOf(Overlay.ADD_POLYLINE_MARKER, Overlay.ADD_MARKER).contains(activeOverlay)

    fun updateUILatLngCoords(cameraPositionCoords: LatLng) {
        if (activeOverlay.get() != Overlay.ADD_MARKER) return
        with(newMarkerCoordsValues) {
            replace("lat", cameraPositionCoords.latitude.toFloat())
            replace("lng", cameraPositionCoords.longitude.toFloat())
        }
    }

    fun toggleMarkerInfoView(marker: Marker?) {
        if (marker == null && activeMarker.get() != null) hideActiveMarkerInfoView()
        if (marker != null && activeMarker.get() == null) showActiveMarkerInfo()
        activeMarker.set(marker)
    }

    fun repositionLocationCompass(mapView: MapView) {
        val locationCompass =
            (mapView.findViewById<View>("1".toInt()).parent as View).findViewById<View>("5".toInt())
        val layoutParams = (locationCompass.layoutParams as RelativeLayout.LayoutParams)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 160, 30, 0)
    }

    private fun showActiveMarkerInfo() {
        binding?.let {
            with(it.markerInfoView.root) {
                val addBtnToInfoMargin = 16
                val addMarkerBtn = it.btnAddMarker
                val slideDistance =
                    addMarkerBtn.marginBottom - ((height.toFloat()) + addBtnToInfoMargin)

                this.animateSlideVertical(-height.toFloat(), 150)
                addMarkerBtn.animateSlideVertical(slideDistance, 150)
            }
        }
    }

    private fun hideActiveMarkerInfoView() {
        binding?.let {
            with(it.markerInfoView.root) {
                val markerBtnToInfoMargin = 16
                val addMarkerBtn = it.btnAddMarker
                val slideDistance =
                    height.toFloat() + markerBtnToInfoMargin - addMarkerBtn.marginBottom

                this.animateSlideVertical(height.toFloat(), 150)
                addMarkerBtn.animateSlideVertical(slideDistance, 150)
            }
        }
    }

    fun updateBinding(binding: FragmentMainBinding?) {
        this.binding = binding
    }

    enum class Overlay { NONE, SEARCH_LOCATION, DRAWER, SHARE, BROWSE_MARKERS, ORGANIZE_MARKERS, ADD_MARKER, ADD_POLYLINE_MARKER }
}
