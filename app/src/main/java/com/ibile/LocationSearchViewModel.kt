package com.ibile

import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.ibile.core.toObservable
import com.ibile.utils.Resource
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

data class LocationSearchState(
    val searchQuery: String? = "",
    val searchPlacesResultAsync: Async<List<AutocompletePrediction>> = Uninitialized
) : MvRxState

class LocationSearchViewModel(
    initialState: LocationSearchState,
    private val placesClient: PlacesClient
) : BaseMvRxViewModel<LocationSearchState>(initialState) {

    private val searchQuerySubject: PublishRelay<String> = PublishRelay.create()
    var currentSearchSessionToken: AutocompleteSessionToken? = null
    val getSelectedPlaceResource = ObservableField<Resource<Place>>()

    init {
        searchQuerySubject
            .startWith("")
            .distinctUntilChanged()
            .doOnNext { setState { copy(searchPlacesResultAsync = Loading()) } }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .switchMap { query -> searchPlaces(query) }
            .execute { copy(searchPlacesResultAsync = it) }
    }

    private fun searchPlaces(query: String?): Observable<List<AutocompletePrediction>> {
        if (query.isNullOrBlank()) return Observable.just<List<AutocompletePrediction>>(arrayListOf())

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(currentSearchSessionToken)
            .build()
        return placesClient.findAutocompletePredictions(request)
            .toObservable()
            .map { it.autocompletePredictions }
    }

    fun setSearchQuery(query: String) {
        searchQuerySubject.accept(query)
    }

    fun setCurrentSessionToken(newToken: AutocompleteSessionToken) {
        currentSearchSessionToken = newToken
    }


    fun fetchPlace(placeId: String): Observable<Resource<Place>> {
        val fetchPlaceRequest = FetchPlaceRequest
            .builder(
                placeId,
                arrayListOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)
            )
            .setSessionToken(currentSearchSessionToken)
            .build()
        return placesClient
            .fetchPlace(fetchPlaceRequest)
            .toObservable()
            .map { Resource.success(it.place) }
            .startWith(Resource.loading())
            .doOnNext { getSelectedPlaceResource.set(it) }
            .doOnError { getSelectedPlaceResource.set(Resource.error("", null)) }
    }

    companion object : MvRxViewModelFactory<LocationSearchViewModel, LocationSearchState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: LocationSearchState
        ): LocationSearchViewModel {
            val placesClient by (viewModelContext as ActivityViewModelContext)
                .activity.inject<PlacesClient>()
            return LocationSearchViewModel(state, placesClient)
        }
    }
}