package com.ibile.features.main.mapfiles

import com.airbnb.mvrx.*
import com.ibile.R
import com.ibile.core.BaseViewModel
import com.ibile.data.SharedPref
import com.ibile.data.repositiories.MapFile
import com.ibile.data.repositiories.MapFilesRepository
import com.ibile.features.main.mapfiles.FileOptionsAndMapFilesArrayAdapter.MapFilesOptionsItem
import com.ibile.utils.extensions.withDefaultScheduler
import org.koin.android.ext.android.get
import java.util.*

class MapFilesViewModel(
    initialState: State,
    private val mapFilesRepository: MapFilesRepository,
    private val sharedPref: SharedPref
) :
    BaseViewModel<MapFilesViewModel.State>(initialState) {

    private val mapFiles: List<MapFile>?
        get() = state.getMapFilesAsyncResult()

    private var currentMapFileId: String
        get() = sharedPref.currentMapFileId!!
        set(value) {
            sharedPref.currentMapFileId = value
        }

    init {
        mapFilesRepository.getMapFiles()
            .withDefaultScheduler()
            .execute { copy(getMapFilesAsyncResult = it) }
        asyncSubscribe(State::createMapFileAsyncResult) {
            currentMapFileId = it.id
            setState { copy(command = Command.MapFileChange) }
        }
    }

    fun getCurrentMapFile(): MapFile? = mapFiles?.find { it.id == currentMapFileId }

    fun onClickCurrentMapFile() {
        val mapFileOptions = buildDialogOptionItems()
        setState { copy(command = Command.ShowMapFilesOptions(mapFileOptions)) }
    }

    private fun buildDialogOptionItems(): List<MapFilesOptionsItem> {
        val mapFilesOptionsItems = mapFiles!!
            .filter { it.id != currentMapFileId }
            .map { MapFilesOptionsItem.Item(R.drawable.ic_baseline_sd_storage_24, it.name) }
        return DEFAULT_MAP_FILE_OPTIONS + if (mapFilesOptionsItems.isNotEmpty())
            listOf(MAP_FILES_LIST_HEADER_ITEM) + mapFilesOptionsItems
        else listOf()
    }

    fun onSelectMapFilesOptionsItem(optionIndex: Int) {
        val options = (state.command as Command.ShowMapFilesOptions).options
        when (options[optionIndex]) {
            DEFAULT_MAP_FILE_OPTIONS.first() -> setState { copy(command = Command.ShowMapFileRename) }
            DEFAULT_MAP_FILE_OPTIONS[1] -> setState { copy(command = Command.ShowCreateNewMapFile) }
            else -> {
                val mapFiles = mapFiles!!.filter { it.id != currentMapFileId }
                val selectedMapFile = mapFiles[optionIndex - NON_MAP_FILES_ITEMS_COUNT]
                setState { copy(command = Command.ShowMapFileOptions(selectedMapFile)) }
            }
        }
    }

    fun onClickRenameMapFileViewPositiveBtn(value: String) {
        val updatedMapFile = getCurrentMapFile()!!.copy(name = value)
        mapFilesRepository
            .updateMapFile(updatedMapFile)
            .withDefaultScheduler()
            .execute { copy() }
    }

    fun onClickCreateNewMapViewPositiveBtn(mapName: String) {
        val mapFile = MapFile(mapName, UUID.randomUUID().toString())
        mapFilesRepository
            .addNewMapFile(mapFile)
            .withDefaultScheduler()
            .toSingleDefault(mapFile)
            .execute { copy(createMapFileAsyncResult = it) }
    }

    fun onSelectMapFileOption(optionIndex: Int) {
        val mapFile = (state.command as Command.ShowMapFileOptions).mapFile
        when (optionIndex) {
            0 -> { // switch map file
                currentMapFileId = mapFile.id
                setState { copy(command = Command.MapFileChange) }
            }
            1 -> { // delete map file
                setState { copy(command = Command.ShowMapFileDeleteConfirmation(mapFile)) }
            }
        }
    }

    fun onClickDeleteMapFileConfirm() {
        val mapFile = (state.command as Command.ShowMapFileDeleteConfirmation).mapFile
        mapFilesRepository
            .deleteMapFile(mapFile)
            .withDefaultScheduler()
            .doOnComplete { setState { copy(command = Command.ShowMapFileDeleteSuccessMsg) } }
            .execute { copy() }
    }

    fun onCancelMapsFilesAction() {
        setState { copy(command = null) }
    }

    data class State(
        val command: Command? = null,
        val getMapFilesAsyncResult: Async<List<MapFile>> = Uninitialized,
        val createMapFileAsyncResult: Async<MapFile> = Uninitialized
    ) :
        MvRxState

    sealed class Command {
        data class ShowMapFilesOptions(val options: List<MapFilesOptionsItem>) : Command()
        object ShowMapFileRename : Command()
        object ShowCreateNewMapFile : Command()
        object MapFileChange : Command()
        object ShowMapFileDeleteSuccessMsg : Command()
        data class ShowMapFileOptions(val mapFile: MapFile) : Command()
        data class ShowMapFileDeleteConfirmation(val mapFile: MapFile) : Command()
    }

    companion object : MvRxViewModelFactory<MapFilesViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): MapFilesViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return MapFilesViewModel(state, fragment.get(), fragment.get())
        }

        private val DEFAULT_MAP_FILE_OPTIONS = listOf(
            MapFilesOptionsItem.Item(
                R.drawable.ic_edit_24,
                "Rename",
                "Rename this map"
            ),
            MapFilesOptionsItem.Item(
                R.drawable.ic_create_new_folder,
                "Create map file",
                "Create a new map file locally"
            )
        )
        private val MAP_FILES_LIST_HEADER_ITEM = MapFilesOptionsItem.Header("Map files")
        private const val NON_MAP_FILES_ITEMS_COUNT = 3
    }
}