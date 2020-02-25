package com.ibile.features.main

import androidx.databinding.ObservableBoolean
import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

data class MarkersViewModelState(
    val markersAsync: Async<List<Marker>> = Uninitialized,
    val activeMarkerId: Long? = null,
    val addMarkerAsync: Async<Long> = Uninitialized,
    val cameraPosition: LatLng? = null
) : MvRxState

class MainViewModel(
    initialState: MarkersViewModelState,
    private val markersRepository: MarkersRepository
) : BaseViewModel<MarkersViewModelState>(initialState) {
    val locationButtonIsActive = ObservableBoolean()

    fun init() {
        withState { state ->
            if (state.markersAsync is Success) return@withState
            markersRepository
                .getAllMarkers()
                .toObservable()
                .execute { copy(markersAsync = it) }
        }
    }

    fun addMarker(markerCoords: LatLng) {
        val newMarker = Marker.createMarker(markerCoords)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun resetAddMarkerAsync() = setState { copy(addMarkerAsync = Uninitialized) }

    fun setActiveMarkerId(activeMarkerId: Long?) {
        setState { copy(activeMarkerId = activeMarkerId) }
    }

    fun getMarkerById(id: Long): Marker {
        return state.markersAsync()!!.find { it.id == id }!!
    }

    companion object : MvRxViewModelFactory<MainViewModel, MarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: MarkersViewModelState
        ): MainViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val repo by fragment.inject<MarkersRepository>()
            return MainViewModel(state, repo)
        }
    }
}
