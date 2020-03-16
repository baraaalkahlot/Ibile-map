package com.ibile.features.browsemarkers

import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Success
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.MainFragment
import com.ibile.features.main.MainFragment.Companion.ExternalOverlaysResult
import com.ibile.markerFolderTitle
import com.ibile.markerItem

interface BrowseMarkersViewEvents {
    fun onClickMarkerItem(markerId: Long)
}

class BrowseMarkersPresenter(private val browseMarkersViewModel: BrowseMarkersViewModel) {
    fun init() {
        if (browseMarkersViewModel.state.getFoldersAsync !is Success) {
            browseMarkersViewModel.getAllFolders()
        }
    }

    fun buildModels(controller: EpoxyController, eventsHandler: BrowseMarkersViewEvents) {
        with(controller) {
            val searchQuery = browseMarkersViewModel.state.searchQuery
            browseMarkersViewModel.state.getFoldersAsync()
                ?.filter { it.folder.selected && it.markers.isNotEmpty() }
                ?.forEach { (folder, markers) ->
                    val filteredMarkers = markers.filter { it.containsQuery(searchQuery) }
                    if (filteredMarkers.isEmpty()) return@forEach

                    markerFolderTitle {
                        id("BrowseMarkersList_MarkersFolderItem_${folder.id}")
                        text(folder.title)
                    }
                    filteredMarkers
                        .forEach { marker ->
                            markerItem {
                                id("BrowseMarkersList_MarkerItem_${marker.id}")
                                marker(marker)
                                onClick { model, _, _, _ ->
                                    eventsHandler.onClickMarkerItem(model.marker().id)
                                }
                            }
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
        browseMarkersViewModel.updateState { copy(searchQuery = value) }
    }

    fun dispose() {
        // no destroy activity to be done
    }
}
