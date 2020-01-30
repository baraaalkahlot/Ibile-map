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

    init {
        asyncSubscribe(MarkersViewModelState::markersAsync) {
            withState {
                val latestMarkerId = it.addMarkerAsync()
                if (latestMarkerId != null) setState {
                    copy(activeMarkerId = latestMarkerId, addMarkerAsync = Uninitialized)
                }
            }
        }
    }

    fun init() {
        markersRepository
            .getAllMarkers()
            .toObservable()
            .execute { copy(markersAsync = it) }
    }

    fun addPointMarker(markerCoords: LatLng) {
        val newMarker = Marker.point(markerCoords)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun addPolylineMarker(points: List<LatLng?>) {
        val newMarker = Marker.polyline(points)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun setActiveMarkerId(activeMarkerId: Long?) {
        setState { copy(activeMarkerId = activeMarkerId) }
    }

    fun getActiveMarker(): Marker? {
        return withState(this) { state ->
            val (markersAsync, activeMarkerId) = state
            activeMarkerId?.let { markersAsync()?.find { it.id == activeMarkerId } }
        }
    }

    companion object : MvRxViewModelFactory<MarkersViewModel, MarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: MarkersViewModelState
        ): MarkersViewModel {
            val repo by (viewModelContext as ActivityViewModelContext)
                .activity.inject<MarkersRepository>()
            return MarkersViewModel(state, repo)
        }
    }
}
