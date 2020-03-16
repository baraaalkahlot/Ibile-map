package com.ibile.features.locationssearch

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.ibile.core.addTo
import com.ibile.features.main.MainFragment
import com.ibile.features.addmarkerfromlocationssearchresult.LocationSearchSelectedResultFragment
import com.ibile.placesResultItem
import com.ibile.searchPlacesResultsState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

interface LocationsSearchViewEvents {
    fun onClickSearchResultItem(id: String)
}

class LocationsSearchPresenter(
    private val locationsSearchViewModel: LocationsSearchViewModel,
    private val fragmentManager: FragmentManager
) {
    private val selectedResultFragment: LocationSearchSelectedResultFragment
        get() = fragmentManager.findFragmentByTag(LOCATIONS_SEARCH_SELECTED_RESULT_FRAGMENT_TAG)
                as? LocationSearchSelectedResultFragment
            ?: LocationSearchSelectedResultFragment()

    private val searchQuerySubject: PublishRelay<String> = PublishRelay.create()
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    fun init(lifecycleOwner: LifecycleOwner) {
        if (locationsSearchViewModel.currentSearchSessionToken == null) {
            locationsSearchViewModel.currentSearchSessionToken =
                AutocompleteSessionToken.newInstance()
        }
        searchQuerySubject
            .distinctUntilChanged()
            .doOnNext {
                if (it.isNotBlank())
                    locationsSearchViewModel.updateState { copy(searchPlacesResultAsync = Loading()) }
            }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .subscribe { locationsSearchViewModel.updateState { copy(searchQuery = it) } }
            .addTo(compositeDisposable)
        locationsSearchViewModel
            .selectSubscribe(lifecycleOwner, LocationSearchState::searchQuery) { searchPlaces(it) }
    }

    private fun searchPlaces(query: String) {
        if (query.isBlank()) return
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(locationsSearchViewModel.currentSearchSessionToken)
            .build()
        locationsSearchViewModel.searchPlaces(request)
    }

    fun onSearchInputChange(value: String) {
        searchQuerySubject.accept(value)
    }

    fun buildModels(controller: EpoxyController, eventsHandlerSearch: LocationsSearchViewEvents) =
        withState(locationsSearchViewModel) { state ->
            controller.searchPlacesResultsState {
                id("SearchPlacesResult")
                isLoading(state.searchPlacesResultAsync is Loading)
                isSuccess(state.searchPlacesResultAsync is Success)
                isError(state.searchPlacesResultAsync is Error)
            }
            state.searchPlacesResultAsync()?.forEach { result ->
                controller.placesResultItem {
                    id(result.placeId)
                    prediction(result)
                    onClick { model, _, _, _ ->
                        eventsHandlerSearch.onClickSearchResultItem(model.prediction().placeId)
                    }
                }
            }
        }

    fun onClickSearchResultItem(id: String) {
        locationsSearchViewModel.updateState { copy(selectedSearchResultId = id) }
        selectedResultFragment.show(fragmentManager, LOCATIONS_SEARCH_SELECTED_RESULT_FRAGMENT_TAG)
    }

    fun onBackPressed(navController: NavController) {
        setResult(navController, null)
    }

    fun onAddMarkerSuccess(navController: NavController, markerId: Long) {
        locationsSearchViewModel.currentSearchSessionToken = null
        selectedResultFragment.dismiss()
        setResult(navController, markerId)
        navController.popBackStack()
    }

    private fun setResult(navController: NavController, result: Long?) {
        navController
            .previousBackStackEntry?.savedStateHandle?.set(
                MainFragment.RESULT_FRAGMENT_EXTERNAL_OVERLAY,
                MainFragment.Companion.ExternalOverlaysResult.LocationsSearch(result)
            )
    }

    fun onSelectedResultViewBackPressed() {
        selectedResultFragment.dismiss()
    }

    fun dispose() {
        compositeDisposable.clear()
    }

    val selectedSearchResultId: String
        get() = locationsSearchViewModel.state.selectedSearchResultId

    val sessionToken: AutocompleteSessionToken
        get() = locationsSearchViewModel.currentSearchSessionToken!!

    companion object {
        const val LOCATIONS_SEARCH_SELECTED_RESULT_FRAGMENT_TAG =
            "LOCATIONS_SEARCH_SELECTED_RESULT_FRAGMENT"
    }
}