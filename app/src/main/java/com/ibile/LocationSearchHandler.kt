package com.ibile

import androidx.databinding.ObservableField
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
import java.util.concurrent.TimeUnit

class LocationSearchHandler(private val placesClient: PlacesClient) {

    private val searchQuerySubject: PublishRelay<String> = PublishRelay.create()
    var currentSearchSessionToken: AutocompleteSessionToken? = null
    val searchLocationsResponseObservable: Observable<List<AutocompletePrediction>>
    val locationsSearchApiResource = ObservableField<Resource<List<AutocompletePrediction>>>()
    val getSelectedPlaceResource = ObservableField<Resource<Place>>()

    // not doing anything for now, called in [MainActivity] so koin does not wait till dependency
    // is required in fragment before creating it
    fun init() {

    }

    init {
        searchLocationsResponseObservable = searchQuerySubject
            .distinctUntilChanged()
            .doOnNext { locationsSearchApiResource.set(Resource.loading()) }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .switchMap { query: String -> searchPlaces(query) }
    }

    private fun searchPlaces(query: String): Observable<List<AutocompletePrediction>> {
        if (query.isBlank()) return Observable.just<List<AutocompletePrediction>>(arrayListOf())
            .doOnNext { locationsSearchApiResource.set(Resource.success(it)) }

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(currentSearchSessionToken)
            .build()
        return placesClient.findAutocompletePredictions(request)
            .toObservable()
            .map { it.autocompletePredictions }
            .doOnNext { locationsSearchApiResource.set(Resource.success(it)) }
            .doOnError { locationsSearchApiResource.set(Resource.error("", null)) }
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
}