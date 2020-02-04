package com.ibile

import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxState
import com.google.android.libraries.maps.MapView
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.animateSlideVertical
import com.ibile.data.database.entities.Marker


data class UIStateViewModelState(val activeListView: UIStateViewModel.ListView = UIStateViewModel.ListView.NONE) :
    MvRxState

class UIStateViewModel(initialState: UIStateViewModelState) :
    BaseMvRxViewModel<UIStateViewModelState>(initialState) {

    val activeMarker = ObservableField<Marker>()
    val locationButtonIsActive = ObservableBoolean()

    val activeOverlayObservable = ObservableField<Overlay>(Overlay.NONE)
    var activeOverlay
        get() = activeOverlayObservable.get()
        set(value) {
            val activeListView = when (value) {
                Overlay.BROWSE_MARKERS -> ListView.BROWSE_MARKERS
                Overlay.SEARCH_LOCATION -> ListView.SEARCH_LOCATION
                Overlay.ORGANIZE_MARKERS -> ListView.ORGANIZE_MARKERS
                else -> ListView.NONE
            }
            setState { copy(activeListView = activeListView) }
            activeOverlayObservable.set(value)
        }

    val cameraPositionObservable: ObservableField<LatLng> = ObservableField()
    var cameraPosition
        get() = cameraPositionObservable.get()
        set(value) {
            cameraPositionObservable.set(value)
        }


    fun handleActionBarBtnClick(overlay: Overlay) {
        updateActiveOverlay(overlay)
    }

    fun updateActiveOverlay(overlay: Overlay) {
        activeOverlay = overlay
    }

    fun handleCancelAddMarkerBtnClick() {
        activeOverlay = Overlay.NONE
    }

    fun actionBarIsVisible(activeOverlay: Overlay) =
        !arrayListOf(Overlay.ADD_POLY_SHAPE, Overlay.ADD_MARKER).contains(activeOverlay)

    fun onCameraMove(cameraPositionCoords: LatLng) {
        cameraPosition = cameraPositionCoords
        if (locationButtonIsActive.get()) {
            locationButtonIsActive.set(false)
        }
    }

    fun setActiveMarker(marker: Marker?) {
        activeMarker.set(marker)
    }

    fun repositionLocationCompass(mapView: MapView) {
        val locationCompass =
            (mapView.findViewById<View>("1".toInt()).parent as View).findViewById<View>("5".toInt())
        val layoutParams = (locationCompass.layoutParams as RelativeLayout.LayoutParams)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 200, 30, 0)
    }

    fun epoxyRvIsVisible(_activeOverlay: Overlay): Boolean =
        arrayListOf(Overlay.SEARCH_LOCATION, Overlay.BROWSE_MARKERS).contains(_activeOverlay)

    enum class Overlay { NONE, SEARCH_LOCATION, DRAWER, SHARE, BROWSE_MARKERS, ORGANIZE_MARKERS, ADD_MARKER, ADD_POLY_SHAPE }
    enum class ListView { NONE, SEARCH_LOCATION, BROWSE_MARKERS, ORGANIZE_MARKERS }

    companion object {
        private fun showActiveMarkerInfo(view: View, addMarkerBtn: ImageButton) {
            with(view) {
                val addBtnToInfoMargin = 16
                val slideDistance =
                    addMarkerBtn.marginBottom - ((height.toFloat()) + addBtnToInfoMargin)

                this.animateSlideVertical(-height.toFloat(), 150)
                addMarkerBtn.animateSlideVertical(slideDistance, 150)
            }
        }

        private fun hideActiveMarkerInfoView(view: View, addMarkerBtn: ImageButton) {

            with(view) {
                val markerBtnToInfoMargin = 16
                val slideDistance =
                    height.toFloat() + markerBtnToInfoMargin - addMarkerBtn.marginBottom

                this.animateSlideVertical(height.toFloat(), 150)
                addMarkerBtn.animateSlideVertical(slideDistance, 150)
            }

        }

        @BindingAdapter("app:marker")
        @JvmStatic
        fun setMarker(view: ConstraintLayout, oldMarker: Marker?, marker: Marker?) {
            if (oldMarker == marker) return

            val markerInfoWindow = view.findViewById<ConstraintLayout>(R.id.marker_info_view)
            val addMarkerBtn = view.findViewById<ImageButton>(R.id.btn_add_marker)


            if (marker == null) {
                hideActiveMarkerInfoView(markerInfoWindow, addMarkerBtn)
            } else {
                showActiveMarkerInfo(markerInfoWindow, addMarkerBtn)
            }
        }

        @BindingAdapter("app:overlayViewIsVisible")
        @JvmStatic
        fun setOverlayViewIsVisible(view: EpoxyRecyclerView, isVisible: Boolean) {
            val parent = (view.parent as ConstraintLayout)
            val constraintSet = ConstraintSet()
            constraintSet.clone(parent)
            if (isVisible) {
                constraintSet.constrainHeight(view.id, ConstraintSet.MATCH_CONSTRAINT)
            } else {
                constraintSet.constrainHeight(view.id, 1)
            }
            constraintSet.applyTo(parent)
        }
    }
}
