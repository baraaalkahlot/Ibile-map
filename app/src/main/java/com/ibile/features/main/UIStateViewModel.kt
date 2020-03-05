package com.ibile.features.main

import androidx.databinding.ObservableField
import com.airbnb.mvrx.MvRxState
import com.ibile.core.BaseViewModel
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiPresenter
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiPresenter
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel


data class UIStateViewModelState(val activeOverlay: UIStateViewModel.Overlay = UIStateViewModel.Overlay.None) :
    MvRxState

class UIStateViewModel(initialState: UIStateViewModelState) :
    BaseViewModel<UIStateViewModelState>(initialState) {
    val activeOverlayObservable = ObservableField<Overlay>(Overlay.None)

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
        class MarkerInfo(val markerId: Long?) : Overlay()
        object ExternalOverlay : Overlay()
    }
}
