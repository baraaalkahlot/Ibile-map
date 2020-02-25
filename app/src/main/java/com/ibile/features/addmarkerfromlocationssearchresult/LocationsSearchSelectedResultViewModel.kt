package com.ibile.features.addmarkerfromlocationssearchresult

import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.ibile.core.toObservable
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

data class LocationsSearchSelectedResultViewModelState(
    val addMarkerAsyncResult: Async<Long> = Uninitialized,
    val fetchPlaceAsyncResult: Async<Place> = Uninitialized
) : MvRxState

class LocationsSearchSelectedResultViewModel(
    initialState: LocationsSearchSelectedResultViewModelState,
    private val placesClient: PlacesClient,
    private val markersRepository: MarkersRepository
) :
    BaseMvRxViewModel<LocationsSearchSelectedResultViewModelState>(initialState) {

    val state: LocationsSearchSelectedResultViewModelState
        get() = withState(this) { it }

    fun fetchPlace(fetchPlaceRequest: FetchPlaceRequest) {
        placesClient
            .fetchPlace(fetchPlaceRequest)
            .toObservable()
            .map { result -> result.place }
            .execute { copy(fetchPlaceAsyncResult = it) }
    }

    fun addMarker(markerCoords: LatLng) {
        val newMarker = Marker.createMarker(markerCoords)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsyncResult = it) }
    }

    companion object :
        MvRxViewModelFactory<LocationsSearchSelectedResultViewModel, LocationsSearchSelectedResultViewModelState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: LocationsSearchSelectedResultViewModelState
        ): LocationsSearchSelectedResultViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return LocationsSearchSelectedResultViewModel(state, fragment.get(), fragment.get())
        }
    }
}