package com.ibile.features.mainexternaloverlays

import androidx.databinding.ObservableField
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.ibile.features.browsemarkers.BrowseMarkersPresenter
import com.ibile.features.browsemarkers.BrowseMarkersViewEvents
import com.ibile.features.locationssearch.LocationsSearchPresenter
import com.ibile.features.locationssearch.LocationsSearchViewEvents

class MainPresenter(
    private val uiStateViewModel: UIStateViewModel,
    private val browseMarkersPresenter: BrowseMarkersPresenter,
    private val locationsSearchPresenter: LocationsSearchPresenter
) {
    val actionBarViewBindingData = ObservableField<ActionBarViewData>()

    fun init(lifecycleOwner: LifecycleOwner) {
        val currentView = uiStateViewModel.state.currentView
        actionBarViewBindingData.set(ActionBarViewData(currentView))
        when (currentView) {
            is UIStateViewModel.CurrentView.BrowseMarkers -> browseMarkersPresenter.init()
            is UIStateViewModel.CurrentView.LocationsSearch -> locationsSearchPresenter
                .init(lifecycleOwner)
        }
    }

    fun buildModels(
        controller: EpoxyController,
        browseMarkersViewEvents: BrowseMarkersViewEvents,
        locationsSearchViewEvents: LocationsSearchViewEvents
    ) {
        updateActionBarViewBindingData()
        when (uiStateViewModel.state.currentView) {
            is UIStateViewModel.CurrentView.BrowseMarkers -> browseMarkersPresenter
                .buildModels(controller, browseMarkersViewEvents)
            is UIStateViewModel.CurrentView.LocationsSearch -> locationsSearchPresenter
                .buildModels(controller, locationsSearchViewEvents)
        }
    }

    private fun updateActionBarViewBindingData() {
        val currentActionBarViewData = actionBarViewBindingData.get()
        val currentView = uiStateViewModel.state.currentView
        if (currentActionBarViewData?.currentView == currentView) return

        actionBarViewBindingData.set(currentActionBarViewData?.copy(currentView = currentView))
    }

    fun onClickMarkerItem(markerId: Long, navController: NavController) {
        browseMarkersPresenter.onClickMarkerItem(markerId, navController)
    }

    fun onBackPressed(navController: NavController) = when (uiStateViewModel.state.currentView) {
        is UIStateViewModel.CurrentView.BrowseMarkers -> browseMarkersPresenter
            .onBackPressed(navController)
        is UIStateViewModel.CurrentView.LocationsSearch -> locationsSearchPresenter
            .onBackPressed(navController)
    }

    fun onBrowseMarkerSearchInputChange(value: String) {
        browseMarkersPresenter.onSearchInputChange(value)
    }

    fun onLocationsSearchInputChange(value: String) {
        locationsSearchPresenter.onSearchInputChange(value)
    }

    fun onClickLocationsSearchResultItem(id: String) {
        locationsSearchPresenter.onClickSearchResultItem(id)
    }

    fun onLocationsSearchSelectedResultViewAddMarkerSuccess(
        markerId: Long,
        navController: NavController
    ) {
        locationsSearchPresenter.onAddMarkerSuccess(navController, markerId)
    }

    fun onLocationsSearchSelectedResultViewBackPressed() {
        locationsSearchPresenter.onSelectedResultViewBackPressed()
    }

    fun onClickActionBarBrowseMarkersBtn() {
        browseMarkersPresenter.init()
        uiStateViewModel.updateCurrentView(UIStateViewModel.CurrentView.BrowseMarkers())
    }

    fun onClickActionBarLocationsSearchBtn(lifecycleOwner: LifecycleOwner) {
        locationsSearchPresenter.init(lifecycleOwner)
        uiStateViewModel.updateCurrentView(UIStateViewModel.CurrentView.LocationsSearch())
    }

    fun dispose() {
        locationsSearchPresenter.dispose()
        browseMarkersPresenter.dispose()
    }

    val locationsSearchSelectedSearchResultId: String
        get() = locationsSearchPresenter.selectedSearchResultId

    val locationsSearchSessionToken: AutocompleteSessionToken
        get() = locationsSearchPresenter.sessionToken
}