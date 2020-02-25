package com.ibile.features.browsemarkers

import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.MainFragment
import com.ibile.features.main.MainFragment.Companion.ExternalOverlaysResult
import com.ibile.markerItem

interface BrowseMarkersViewEvents {
    fun onClickMarkerItem(markerId: Long)
}

class BrowseMarkersPresenter(private val browseMarkersViewModel: BrowseMarkersViewModel) {
    fun init() {
        browseMarkersViewModel.init()
    }

    fun buildModels(controller: EpoxyController, eventsHandler: BrowseMarkersViewEvents) {
        val markers = browseMarkersViewModel.state.markersAsync()
        val searchQuery = browseMarkersViewModel.state.searchQuery
        markers
            ?.filter { marker -> marker.containsQuery(searchQuery) }
            ?.forEach { marker ->
                controller.markerItem {
                    id(marker.id)
                    marker(marker)
                    onClick { model, _, _, _ ->
                        eventsHandler.onClickMarkerItem(model.marker().id)
                    }
                }
            }
    }

    private fun Marker.containsQuery(query: String): Boolean = title.contains(query, true)
            || (description ?: "").contains(query, true)

    fun onClickMarkerItem(markerId: Long, navController: NavController) {
        setResult(navController, markerId)
        navController.popBackStack()
    }

    fun onBackPressed(navController: NavController) {
        setResult(navController, null)
    }

    private fun setResult(navController: NavController, result: Long?) {
        navController.previousBackStackEntry?.savedStateHandle
            ?.set(
                MainFragment.RESULT_FRAGMENT_EXTERNAL_OVERLAY,
                ExternalOverlaysResult.BrowseMarkers(result)
            )
    }

    fun onSearchInputChange(value: String) {
        browseMarkersViewModel.setSearchQuery(value)
    }

    fun dispose() {
        // no destroy activity to be done
    }
}