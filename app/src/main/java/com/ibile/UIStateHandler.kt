package com.ibile

import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.marginBottom
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.animateSlideVertical
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.FragmentMainBinding

class UIStateHandler(private val binding: FragmentMainBinding) {
    val activeActionBarBtn =
        ObservableField<MapActionBarBtns>(MapActionBarBtns.NONE)
    val addNewMarkerIsActive = ObservableBoolean(false)
    val newMarkerCoordsValues = ObservableArrayMap<String, Float>()
        .apply {
            put("lat", 0f)
            put("lng", 0f)
        }
    val activeMarker = ObservableField<Marker>()

    fun handleActionBarBtnClick(identifier: MapActionBarBtns) {
        activeActionBarBtn.set(identifier)
    }

    fun handleCancelAddMarkerBtnClick() {
        addNewMarkerIsActive.set(false)
    }


    fun updateAddMarkerIsActive(isActive: Boolean) {
        addNewMarkerIsActive.set(isActive)
    }

    fun updateUILatLngCoords(cameraPositionCoords: LatLng) {
        if (!addNewMarkerIsActive.get()) return
        with(newMarkerCoordsValues) {
            replace("lat", cameraPositionCoords.latitude.toFloat())
            replace("lng", cameraPositionCoords.longitude.toFloat())
        }
    }

    fun toggleMarkerInfoView(marker: Marker?) {
        activeMarker.set(marker)
        if (marker == null) return hideActiveMarkerInfoView()
        showActiveMarkerInfo()
    }

    fun repositionLocationCompass(mapView: MapView) {
        val locationCompass =
            (mapView.findViewById<View>("1".toInt()).parent as View).findViewById<View>("5".toInt())
        val layoutParams = (locationCompass.layoutParams as RelativeLayout.LayoutParams)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 160, 30, 0)
    }

    private fun showActiveMarkerInfo() {
        with(binding.markerInfoView.root) {
            val markerBtnToInfoMargin = 16
            val addMarkerBtn = binding.btnAddMarker
            val slideDistance =
                addMarkerBtn.marginBottom - (height.toFloat() + markerBtnToInfoMargin)

            this.animateSlideVertical(-height.toFloat(), 150)
            addMarkerBtn.animateSlideVertical(slideDistance, 150)
        }
    }

    private fun hideActiveMarkerInfoView() {
        with(binding.markerInfoView.root) {
            val markerBtnToInfoMargin = 16
            val addMarkerBtn = binding.btnAddMarker
            val slideDistance = height.toFloat() + markerBtnToInfoMargin - addMarkerBtn.marginBottom

            this.animateSlideVertical(height.toFloat(), 150)
            addMarkerBtn.animateSlideVertical(slideDistance, 150)
        }
    }

    enum class MapActionBarBtns { NONE, SEARCH_LOCATION, DRAWER, SHARE, BROWSE_MARKERS, ORGANIZE_MARKERS }
}
