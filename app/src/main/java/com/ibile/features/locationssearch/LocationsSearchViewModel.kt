package com.ibile.features.locationssearch

import com.airbnb.mvrx.*
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.ibile.core.toObservable
import org.koin.android.ext.android.inject

data class LocationSearchState(
    val searchQuery: String = "",
    val searchPlacesResultAsync: Async<List<AutocompletePrediction>> = Uninitialized,
    val selectedSearchResultId: String = ""
) : MvRxState

class LocationsSearchViewModel(
    initialState: LocationSearchState,
    private val placesClient: PlacesClient
) : BaseMvRxViewModel<LocationSearchState>(initialState) {
    val state
        get() = withState(this) { it }

    private var _currentSearchSessionToken: AutocompleteSessionToken? = null

    var currentSearchSessionToken: AutocompleteSessionToken?
        get() = _currentSearchSessionToken
        set(value) {
            _currentSearchSessionToken = value
        }

    fun searchPlaces(request: FindAutocompletePredictionsRequest) {
        placesClient.findAutocompletePredictions(request)
            .toObservable()
            .map { response -> response.autocompletePredictions }
            .execute { copy(searchPlacesResultAsync = it) }
    }

    fun updateState(newState: LocationSearchState.() -> LocationSearchState) {
        setState { newState() }
    }

    companion object : MvRxViewModelFactory<LocationsSearchViewModel, LocationSearchState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: LocationSearchState
        ): LocationsSearchViewModel {
            val placesClient by (viewModelContext as FragmentViewModelContext)
                .fragment.inject<PlacesClient>()
            return LocationsSearchViewModel(state, placesClient)
        }
    }
}
