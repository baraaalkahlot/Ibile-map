package com.ibile.features.main.addmarkerpoi

import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Success
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.LatLng
import com.ibile.data.database.entities.Marker
import com.ibile.data.database.entities.Marker.Icon
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import com.ibile.features.main.mapfiles.MapFilesViewModel
import com.ibile.features.markeractiontargetfolderselection.MarkerActionTargetFolderSelectionDialogFragment

class AddMarkerPoiPresenter(
    private val viewModel: AddMarkerPoiViewModel,
    private val fragmentManager: FragmentManager,
    private val mapFilesViewModel: MapFilesViewModel
) {

    private val chooseTargetFolderDialog: MarkerActionTargetFolderSelectionDialogFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_CHOOSE_TARGET_FOLDER)
                as? MarkerActionTargetFolderSelectionDialogFragment
            ?: MarkerActionTargetFolderSelectionDialogFragment()

    fun init(map: GoogleMap) {
        if (viewModel.state.getFoldersAsyncResult !is Success)
            viewModel.getFolders()
        val targetFolder = viewModel.state.targetFolder
        val marker = Marker.createMarker(map.cameraPosition.target)
            .run {
                targetFolder
                    ?.let { copy(folderId = it.id, icon = Icon(it.iconId, true), color = it.color) }
                    ?: this
            }
        viewModel.updateState { copy(mode = Mode.Add, marker = marker) }
    }

    fun initEditMarkerPoint(marker: Marker, map: GoogleMap) {
        map.animateCamera(marker.cameraUpdate, 500, null)
        val markerToUpdate = marker.copy(points = listOf(map.cameraPosition.target))
        viewModel.updateState { copy(mode = Mode.Edit, marker = markerToUpdate) }
    }

    fun onMapMove(cameraPosition: LatLng) {
        viewModel.updateState { copy(marker = marker?.copy(points = listOf(cameraPosition))) }
    }

    fun onClickMarkerTargetFolder() {
        chooseTargetFolderDialog.show(fragmentManager, FRAGMENT_TAG_CHOOSE_TARGET_FOLDER)
    }

    fun onChooseMarkerTargetFolder(folderId: Long) {
        val newTargetFolder = viewModel.state.getFoldersAsyncResult()?.find { it.id == folderId }
        viewModel.updateState { copy(targetFolder = newTargetFolder) }
    }

    val targetFolderOptionsList: List<FolderWithMarkersCount>
        get() = viewModel.state.getFoldersAsyncResult() ?: listOf()

    fun onClickOkBtn() {
        when (viewModel.state.mode) {
            is Mode.Add -> viewModel.addMarker(viewModel.state.marker!! , viewModel.state.targetFolder ,mapFilesViewModel.getCurrentMapFile())
            is Mode.Edit -> viewModel.updateMarker(viewModel.state.marker!!)
        }
    }

    fun onCreateOrUpdateSuccess() {
        reset()
    }

    fun onCancel() {
        reset()
    }

    private fun reset() {
        viewModel.updateState { copy(mode = Mode.Add, marker = null) }
    }

    sealed class Mode {
        object Add : Mode()
        object Edit : Mode()
    }

    companion object {
        const val FRAGMENT_TAG_CHOOSE_TARGET_FOLDER = "FRAGMENT_TAG_CHOOSE_TARGET_FOLDER"
    }
}
