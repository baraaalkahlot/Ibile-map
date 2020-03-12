package com.ibile.features.main.folderlist

import androidx.fragment.app.FragmentManager
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Success
import com.ibile.*
import com.ibile.data.database.entities.Folder
import com.ibile.features.main.addfolder.AddFolderDialog
import com.ibile.features.main.folderlist.FolderWithMarkersCount.Companion.toFolder

class FolderListPresenter(
    private val fragmentManager: FragmentManager,
    private val viewModel: FoldersViewModel
) {
    fun init() {
        if (viewModel.state.getFoldersAsync !is Success) {
            viewModel.getFolders()
        }
    }

    fun buildModels(controller: EpoxyController) {
        with(controller) {
            markerFolderTitle {
                id("DrawerView_Title_Folders")
                text("Folders")
            }
            viewModel.state.getFoldersAsync()?.let {
                val totalMarkers = it
                    .map { folder -> folder.totalMarkers }
                    .reduce { acc, total -> acc + total }
                markerFolderListItemAll {
                    id("DrawerView_MarkerFoldersListItemAll")
                    count("$totalMarkers markers in ${it.size} folder")
                    selected(it.all { folder -> folder.selected })
                    onClick { model, _, _, _ ->
                        handleAllFoldersViewClick(model.selected())
                    }
                }
                it.forEach { folder ->
                    markerFolderListItem {
                        id("DrawerView_MarkerFoldersListItem ${folder.id}")
                        folder(folder)
                        count("${folder.totalMarkers} markers")
                        onClick { model, _, _, _ ->
                            handleFolderItemClick(model.folder())
                        }
                    }
                }
            }
            horizontalDivider { id("Divider_FolderList_Add_Folder") }
            addFolderButton {
                id("DrawerView_AddFolder_Btn")
                onClick { _ -> handleAddFolderBtnClick() }
            }
        }
    }

    private fun handleAddFolderBtnClick() {
        AddFolderDialog.newInstance().show(fragmentManager, FRAGMENT_TAG_ADD_MARKER_FOLDER_DIALOG)
    }

    private fun handleFolderItemClick(folderWithMarkersCount: FolderWithMarkersCount) {
        val folder = folderWithMarkersCount.toFolder()
        val updatedFolder = folder.copy(selected = !folder.selected)
        viewModel.updateFolders(updatedFolder)
    }

    private fun handleAllFoldersViewClick(selected: Boolean) {
        val folders =
            viewModel.state.getFoldersAsync()!!.map { it.copy(selected = !selected).toFolder() }
        viewModel.updateFolders(*folders.toTypedArray())
    }

    fun onAddFolderViewOkBtnClick(folder: Folder) {
        if (folder.title.isBlank()) return
        viewModel.addFolder(folder)
    }

    companion object {
        const val FRAGMENT_TAG_ADD_MARKER_FOLDER_DIALOG = "FRAGMENT_TAG_ADD_MARKER_FOLDER_DIALOG"
    }
}
