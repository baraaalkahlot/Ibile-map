package com.ibile.features.main

import androidx.databinding.ObservableField
import com.airbnb.mvrx.MvRxState
import com.ibile.core.BaseViewModel


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

    sealed class Overlay {
        object None : Overlay()
        object AddMarkerPoi : Overlay()
        object AddPolygonPoi : Overlay()
        object AddPolylinePoi : Overlay()
        class MarkerInfo(val markerId: Long?) : Overlay()
        object ExternalOverlay : Overlay()
    }
}
