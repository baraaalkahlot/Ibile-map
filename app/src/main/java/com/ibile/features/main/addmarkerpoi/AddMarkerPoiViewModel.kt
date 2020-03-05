package com.ibile.features.main.addmarkerpoi

import androidx.databinding.ObservableField
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddMarkerPoiViewModel(initialState: State, private val markersRepository: MarkersRepository) :
    BaseViewModel<AddMarkerPoiViewModel.State>(initialState) {
    val markerTargetCoords: ObservableField<LatLng> = ObservableField()

    fun addMarker(marker: Marker) {
        markersRepository.insertMarker(marker).subscribeOn(Schedulers.io()).execute { copy() }
    }

    fun updateMarker(marker: Marker) {
        markersRepository.updateMarker(marker).subscribeOn(Schedulers.io()).execute { copy() }
    }

    data class State(val mode: AddMarkerPoiPresenter.Mode = AddMarkerPoiPresenter.Mode.Add) : MvRxState

    companion object : MvRxViewModelFactory<AddMarkerPoiViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): AddMarkerPoiViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddMarkerPoiViewModel(state, fragment.get())
        }
    }
}
