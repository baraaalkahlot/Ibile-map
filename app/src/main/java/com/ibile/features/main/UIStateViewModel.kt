package com.ibile.features.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.airbnb.mvrx.MvRxState
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiPresenter
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiPresenter


data class UIStateViewModelState(
    val activeOverlay: UIStateViewModel.Overlay = UIStateViewModel.Overlay.None,
    val cameraPosition: LatLng? = null
) :
    MvRxState

class UIStateViewModel(
    initialState: UIStateViewModelState
) :
    BaseViewModel<UIStateViewModelState>(initialState) {

    val activeOverlayObservable = ObservableField<Overlay>(Overlay.None)
    val locationButtonIsActive = ObservableBoolean()

    init {
        selectSubscribe(UIStateViewModelState::activeOverlay) { activeOverlayObservable.set(it) }
    }

    fun updateActiveOverlay(overlay: Overlay) {
        setState { copy(activeOverlay = overlay) }
    }

    fun addPolyShapeBtnIsVisible(activeOverlay: Overlay): Boolean {
        return activeOverlay is Overlay.AddMarkerPoi && activeOverlay.mode is AddMarkerPoiPresenter.Mode.Add
    }




    sealed class Overlay {
        object None : Overlay()
        class AddMarkerPoi(val mode: AddMarkerPoiPresenter.Mode) : Overlay()
        class AddPolygonPoi(val mode: AddPolygonPoiViewModel.Mode) : Overlay()
        class AddPolylinePoi(val mode: AddPolylinePoiPresenter.Mode) : Overlay()
        object ExternalOverlay : Overlay()
    }
}
