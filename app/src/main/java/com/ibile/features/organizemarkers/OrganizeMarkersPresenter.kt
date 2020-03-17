package com.ibile.features.organizemarkers

import android.app.AlertDialog
import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.UniqueOnly
import com.ibile.R
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.MainFragment
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import com.ibile.features.markeractiontargetfolderselection.MarkerActionTargetFolderSelectionDialogFragment
import com.ibile.features.organizemarkers.OrganizeMarkersPresenter.SelectedMarkersAction.CopyToFolder
import com.ibile.features.organizemarkers.OrganizeMarkersPresenter.SelectedMarkersAction.MoveToFolder
import com.ibile.markerFolderTitle
import com.ibile.organizeMarkersHeaderView
import com.ibile.organizeMarkersListItem

/**
 *
 * TODO: put dialogs in their own fragments to ensure state is saved when configuration change occurs
 *
 * @property viewModel
 * @property context
 */
class OrganizeMarkersPresenter(
    private val viewModel: OrganizeMarkersViewModel,
    private val context: Context,
    private val fragmentManager: FragmentManager
) {
    private var isSubscribedToStateChanges = false

    private val markersActionTargetFolderDialog: MarkerActionTargetFolderSelectionDialogFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_MARKERS_ACTION_TARGET_FOLDER)
                as? MarkerActionTargetFolderSelectionDialogFragment
            ?: MarkerActionTargetFolderSelectionDialogFragment()

    private fun getMarkersFieldsToUpdateOptionsDialog(
        options: Map<String, Boolean>,
        onOptionValueChange: (option: String, value: Boolean) -> Unit,
        onClickPositiveBtn: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialog)
            .setTitle(R.string.text_organize_markers_field_actions_options_msg)
            .setMultiChoiceItems(
                options.keys.toTypedArray(),
                options.values.toBooleanArray()
            ) { _, which, isChecked ->
                onOptionValueChange(options.keys.toList()[which], isChecked)
            }
            .setPositiveButton(R.string.text_ok) { _, _ ->
                onClickPositiveBtn()
            }
            .setNeutralButton(R.string.text_cancel) { _, _ ->

            }
            .create()
    }

    private fun getDeleteMarkersConfirmationDialog(onClickPositiveBtn: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialog)
            .setMessage("Are you sure you want to delete the selected markers?")
            .setPositiveButton(R.string.text_yes) { _, _ ->
                onClickPositiveBtn()
            }
            .setNegativeButton(R.string.text_no) { _, _ -> }
            .create()
    }

    private val displayedMarkers: List<Marker>
        get() {
            val (getFoldersWithMarkersAsyncResult, searchQuery) = viewModel.state
            val folderWithMarkers = getFoldersWithMarkersAsyncResult()!!
            return folderWithMarkers
                .filter { it.folder.selected && it.markers.isNotEmpty() }
                .flatMap { it.markers }.filter { it.containsQuery(searchQuery) }
        }

    fun init(lifecycleOwner: LifecycleOwner) {
        if (viewModel.state.getFoldersWithMarkersAsyncResult !is Success)
            viewModel.getFoldersWithMarkers()

        if (isSubscribedToStateChanges) return

        viewModel.selectSubscribe(
            lifecycleOwner,
            OrganizeMarkersViewModel.State::selectedMarkersAction,
            UniqueOnly("OrganizeMarkersPresenter")
        ) {
            if (it == null) return@selectSubscribe
            markersActionTargetFolderDialog.show(
                fragmentManager,
                FRAGMENT_TAG_MARKERS_ACTION_TARGET_FOLDER
            )
        }

        viewModel.selectSubscribe(
            lifecycleOwner,
            OrganizeMarkersViewModel.State::markersActionTargetFolder,
            UniqueOnly("OrganizeMarkersPresenter")
        ) {
            if (it == null) return@selectSubscribe
            val options = mutableMapOf("Update color" to true, "Update pin icon" to true)
            getMarkersFieldsToUpdateOptionsDialog(
                options,
                { option, value -> options[option] = value }) {
                handleMarkersUpdateAction(options)
            }.show()
        }

        viewModel.asyncSubscribe(
            lifecycleOwner,
            OrganizeMarkersViewModel.State::updateMarkersAsyncResult
        ) {
            viewModel.updateState {
                copy(
                    selectedMarkersIds = listOf(),
                    selectedMarkersAction = null,
                    markersActionTargetFolder = null,
                    updateMarkersAsyncResult = Uninitialized
                )
            }
        }

        isSubscribedToStateChanges = true
    }

    fun onSelectTargetFolder(targetFolderId: Long) {
        val (getFoldersWithMarkersAsyncResult) = viewModel.state
        val foldersWithMarkers = getFoldersWithMarkersAsyncResult()!!
        val targetFolder = foldersWithMarkers.find { it.folder.id == targetFolderId }!!.folder
        viewModel.updateState { copy(markersActionTargetFolder = targetFolder) }
    }

    val markerActionTargetFolderSelectionDialogTitle: String
        get() = viewModel.state.selectedMarkersAction!!.msg

    val markerActionTargetFolderOptionsList: List<FolderWithMarkersCount>
        get() = viewModel.state.getFoldersWithMarkersAsyncResult()!!.map { (folder, markers) ->
            with(folder) {
                FolderWithMarkersCount(title, id, iconId, color, selected, markers.size)
            }
        }

    private fun handleMarkersUpdateAction(fieldsToUpdate: Map<String, Boolean>) {
        val (_, _, selectedMarkersIds, selectedMarkersAction, markersActionTargetFolder) = viewModel.state
        val fields = fieldsToUpdate.filterValues { it }
        val selectedMarkers = displayedMarkers.filter { selectedMarkersIds.contains(it.id) }
        when (selectedMarkersAction) {
            is MoveToFolder -> {
                val markersToMove = selectedMarkers.map {
                    it.copy(
                        folderId = markersActionTargetFolder!!.id,
                        color = if (fields.containsKey("Update color")) markersActionTargetFolder.color else it.color,
                        icon = if (fields.containsKey("Update pin icon"))
                            Marker.Icon(markersActionTargetFolder.iconId) else it.icon
                    )
                }
                viewModel.updateMarkers(markersToMove)
            }
            is CopyToFolder -> {
                val markersToCopy = selectedMarkers.map {
                    it.copy(
                        id = 0,
                        name = it.title,
                        folderId = markersActionTargetFolder!!.id,
                        color = if (fields.containsKey("Update color")) markersActionTargetFolder.color else it.color,
                        icon = if (fields.containsKey("Update pin icon"))
                            Marker.Icon(markersActionTargetFolder.iconId) else it.icon
                    )
                }
                viewModel.insertMarkers(markersToCopy)
            }
        }
    }

    fun buildModels(controller: EpoxyController) {
        with(controller) {
            val (foldersWithMarkersAsyncResult, searchQuery, selectedMarkersIds) = viewModel.state
            foldersWithMarkersAsyncResult()
                ?.filter { it.folder.selected && it.markers.isNotEmpty() }
                ?.let {
                    // TODO: should be sticky
                    organizeMarkersHeaderView {
                        id("OrganizeMarkersView_Action_Btns")
                        actionBtnsAreVisible(selectedMarkersIds.isNotEmpty())
                        onClick { _, _, clickedView, _ ->
                            handleActionBtnsViewClick(clickedView)
                        }
                    }
                    it.forEach { (folder, markers) ->
                        val filteredMarkers =
                            markers.filter { marker -> marker.containsQuery(searchQuery) }
                        if (filteredMarkers.isEmpty()) return@forEach

                        markerFolderTitle {
                            id("OrganizeMarkersView_FolderTitleItem_${folder.id}")
                            text(folder.title)
                        }
                        filteredMarkers
                            .forEach { marker ->
                                organizeMarkersListItem {
                                    id("OrganizeMarkersView_MarkerItem_${marker.id}")
                                    marker(marker)
                                    isSelected(selectedMarkersIds.contains(marker.id))
                                    onClick { model, _, _, _ ->
                                        handleMarkerItemOrCheckboxClick(model.marker().id)
                                    }
                                }
                            }
                    }
                }
        }
    }

    private fun handleActionBtnsViewClick(clickedView: View) {
        when (clickedView.id) {
            R.id.btn_select_all_markers -> handleSelectAllMarkersBtnClick()
            R.id.btn_copy_selected_markers -> viewModel.updateState {
                copy(selectedMarkersAction = CopyToFolder)
            }
            R.id.btn_move_selected_markers -> viewModel.updateState {
                copy(selectedMarkersAction = MoveToFolder)
            }
            R.id.btn_delete_selected_markers -> getDeleteMarkersConfirmationDialog {
                val selectedMarkers =
                    displayedMarkers.filter { viewModel.state.selectedMarkersIds.contains(it.id) }
                viewModel.deleteMarkers(selectedMarkers)
            }.show()
        }
    }

    private fun handleSelectAllMarkersBtnClick() {
        if (!allMarkersSelected) {
            viewModel.updateState { copy(selectedMarkersIds = displayedMarkers.map { it.id }) }
        } else {
            viewModel.updateState { copy(selectedMarkersIds = listOf()) }
        }
    }

    private val allMarkersSelected: Boolean
        get() {
            val allMarkersIds = displayedMarkers.map { it.id }.sorted()
            val selectedMarkersIds = viewModel.state.selectedMarkersIds.distinct().sorted()
            return allMarkersIds == selectedMarkersIds
        }

    private fun handleMarkerItemOrCheckboxClick(markerId: Long) {
        val updatedSelectedMarkerIdsList = viewModel.state.selectedMarkersIds.toMutableList()
        if (updatedSelectedMarkerIdsList.contains(markerId))
            updatedSelectedMarkerIdsList.remove(markerId)
        else
            updatedSelectedMarkerIdsList.add(markerId)
        viewModel.updateState { copy(selectedMarkersIds = updatedSelectedMarkerIdsList) }
    }

    fun onBackPressed(navController: NavController) {
        navController.previousBackStackEntry?.savedStateHandle?.set(
            MainFragment.RESULT_FRAGMENT_EXTERNAL_OVERLAY,
            MainFragment.Companion.ExternalOverlaysResult.OrganizeMarkers
        )
    }

    fun onSearchInputChange(value: String) {
        viewModel.updateState { copy(searchQuery = value, selectedMarkersIds = listOf()) }
    }

    sealed class SelectedMarkersAction(val msg: String) {
        object MoveToFolder : SelectedMarkersAction("Move markers to folder")

        object CopyToFolder : SelectedMarkersAction("Copy markers to folder")
    }

    companion object {
        private fun Marker.containsQuery(query: String): Boolean = title.contains(query, true)
                || (description ?: "").contains(query, true)

        const val FRAGMENT_TAG_MARKERS_ACTION_TARGET_FOLDER =
            "FRAGMENT_TAG_MARKERS_ACTION_TARGET_FOLDER"
    }
}
