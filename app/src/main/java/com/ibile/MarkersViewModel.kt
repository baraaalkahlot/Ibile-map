package com.ibile

import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

data class MarkersViewModelState(
    val markersAsync: Async<List<Marker>> = Uninitialized,
    val activeMarkerId: Long? = null,
    val addMarkerAsync: Async<Long> = Uninitialized
) : MvRxState

class MarkersViewModel(
    initialState: MarkersViewModelState,
    private val markersRepository: MarkersRepository
) : BaseMvRxViewModel<MarkersViewModelState>(initialState) {

    fun init() {
        markersRepository
            .getAllMarkers()
            .toObservable()
            .execute { copy(markersAsync = it) }
    }

    fun addMarker(markerCoords: LatLng) {
        val newMarker = Marker(latitude = markerCoords.latitude, longitude = markerCoords.longitude)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(activeMarkerId = it()) }
    }

    fun setActiveMarkerId(activeMarkerId: Long?) {
        setState { copy(activeMarkerId = activeMarkerId) }
    }

    companion object : MvRxViewModelFactory<MarkersViewModel, MarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: MarkersViewModelState
        ): MarkersViewModel {
            val repo by (viewModelContext as FragmentViewModelContext)
                .fragment.inject<MarkersRepository>()
            return MarkersViewModel(state, repo)
        }
    }
}
