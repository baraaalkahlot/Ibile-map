package com.ibile.features.mainexternaloverlays

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.ibile.features.browsemarkers.BrowseMarkersPresenter
import com.ibile.features.browsemarkers.BrowseMarkersViewEvents
import com.ibile.features.locationssearch.LocationsSearchPresenter
import com.ibile.features.locationssearch.LocationsSearchViewEvents
import com.ibile.features.mainexternaloverlays.UIStateViewModel.CurrentView.*
import com.ibile.features.organizemarkers.OrganizeMarkersPresenter

class MainPresenter(
    private val uiStateViewModel: UIStateViewModel,
    private val browseMarkersPresenter: BrowseMarkersPresenter,
    private val locationsSearchPresenter: LocationsSearchPresenter,
    private val organizeMarkersPresenter: OrganizeMarkersPresenter
) {
    fun init(lifecycleOwner: LifecycleOwner) {
        when (uiStateViewModel.state.currentView) {
            is BrowseMarkers -> browseMarkersPresenter.init()
            is LocationsSearch -> locationsSearchPresenter.init(lifecycleOwner)
            is OrganizeMarkers -> organizeMarkersPresenter.init(lifecycleOwner)
        }
    }

    fun buildModels(
        controller: EpoxyController,
        browseMarkersViewEvents: BrowseMarkersViewEvents,
        locationsSearchViewEvents: LocationsSearchViewEvents
    ) {
        when (uiStateViewModel.state.currentView) {
            is BrowseMarkers -> browseMarkersPresenter
                .buildModels(controller, browseMarkersViewEvents)
            is LocationsSearch -> locationsSearchPresenter
                .buildModels(controller, locationsSearchViewEvents)
            is OrganizeMarkers -> organizeMarkersPresenter.buildModels(controller)
        }
    }

    fun onClickMarkerItem(markerId: Long, navController: NavController) {
        browseMarkersPresenter.onClickMarkerItem(markerId, navController)
    }

    fun onBackPressed(navController: NavController) = when (uiStateViewModel.state.currentView) {
        is BrowseMarkers -> browseMarkersPresenter.onBackPressed(navController)
        is OrganizeMarkers -> organizeMarkersPresenter.onBackPressed(navController)
        is LocationsSearch -> locationsSearchPresenter.onBackPressed(navController)
    }

    fun onBrowseMarkerSearchInputChange(value: String) {
        browseMarkersPresenter.onSearchInputChange(value)
    }

    fun onLocationsSearchInputChange(value: String) {
        locationsSearchPresenter.onSearchInputChange(value)
    }

    fun onOrganizeMarkersSearchInputChange(value: String) {
        organizeMarkersPresenter.onSearchInputChange(value)
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
        uiStateViewModel.updateState { copy(currentView = BrowseMarkers) }
    }

    fun onClickActionBarLocationsSearchBtn(lifecycleOwner: LifecycleOwner) {
        locationsSearchPresenter.init(lifecycleOwner)
        uiStateViewModel.updateState { copy(currentView = LocationsSearch) }
    }

    fun onClickActionBarOrganizeMarkersBtn(lifecycleOwner: LifecycleOwner) {
        organizeMarkersPresenter.init(lifecycleOwner)
        uiStateViewModel.updateState { copy(currentView = OrganizeMarkers) }
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
