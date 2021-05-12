package com.ibile.features.main.datasharing

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.util.Xml
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.core.context
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.features.main.datasharing.DataSharingViewModel.Companion.Command.ShowEntityDataExportOptionsDialog.Entity
import de.siegmar.fastcsv.writer.CsvWriter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.core.KoinComponent
import org.koin.ext.getScopeName

class DataSharingViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val exporter: Exporter
) :
    BaseViewModel<DataSharingViewModel.State>(initialState) {

    init {
        asyncSubscribe(State::getAllFoldersAsyncResult) { executeActionAwaitingData(it) }
        asyncSubscribe(State::exportAsyncResult) { shareExportedEntityData(it) }
    }

    private fun executeActionAwaitingData(it: List<FolderWithMarkers>) {
        state.actionAwaitingData?.invoke(it)
    }

    private fun shareExportedEntityData(exportIntent: Intent) {
        setState { copy(viewCommand = Command.ShareData(exportIntent)) }
    }

    fun init(markerId: Long?) {

        val command = if (markerId == null) Command.ShowInitialShareOptionsDialog
        else Command.ShowInitialShareOptionsWithMarkerOptionsDialog

        setState { copy(markerId = markerId, viewCommand = command) }
    }

    fun initOrganize(markerId: Long?){
        val command = if (markerId == null) Command.ShowInitialShareOptionsDialog
        else Command.ShowEntityDataExportOptionsDialog(Entity.Marker)

        setState { copy(markerId = markerId, viewCommand = command) }
    }

    fun onCancelShareOperation() {
        reset()
    }

    fun onCompleteOrCancelShareOperation() {
        reset()
    }

    private fun reset() {
        exporter.onCompleteOrCancelExport()
        /**
         * not resetting [State.getAllFoldersAsyncResult] so changes made from other views
         * are propagated and waiting to be acted on if needed
         */
        setState {
            copy(
                markerId = null,
                viewCommand = null,
                exportAsyncResult = Uninitialized,
                actionAwaitingData = null
            )
        }
    }

    /**
     * Handles selected option from initial share options.
     *
     * @param selectedOptionIndex
     */
    fun onSelectInitialShareOptionItem(selectedOptionIndex: Int) {
        /**
         * Indices used here correspond to indices of elements in
         * [DataSharingHandler.initialShareOptionsItems]
         * TODO: use constants
         */
        when (selectedOptionIndex) {
            0 -> setState { copy(viewCommand = Command.TakeMapSnapshot) }
            1 -> setState { copy(viewCommand = Command.ShowEntityDataExportOptionsDialog(Entity.AllFilesAndFolder)) }
            2 -> usingAllFoldersWithMarkers(::showSelectFolderToExport)
        }
    }

    /**
     * Handles selected option from initial share options (with marker options).
     *
     * @param selectedOptionIndex
     */
    fun onSelectInitialShareOptionWithMarkerOptionsItem(selectedOptionIndex: Int) {
        /**
         * Indices used here correspond to indices of elements in
         * [DataSharingHandler.initialShareOptionsWithMarkerOptionsItems]
         *
         * options 2, 3, 4 correspond to options 0, 1, 2 in [onSelectInitialShareOptionItem], hence
         * the delegation to same
         * TODO: use constants
         */
        when (selectedOptionIndex) {
            0 -> usingAllFoldersWithMarkers(::shareMarkerAsPlainText)
            1 -> setState { copy(viewCommand = Command.ShowEntityDataExportOptionsDialog(Entity.Marker)) }
            2,
            3,
            4 -> onSelectInitialShareOptionItem(selectedOptionIndex - 2)
        }
    }

    /**
     * Handles selected option from export entity data action.
     *
     * @param selectedOptionIndex
     */
    fun onSelectEntityDataExportFormat(selectedOptionIndex: Int) {
        /**
         * Indices used here correspond to indices of elements in
         * [DataSharingHandler.exportEntityDataFileOptions]
         * TODO: use constants
         */
        when (selectedOptionIndex) {
            0 -> exportEntityData(DataExportFormat.KML)
            1 -> exportEntityData(DataExportFormat.KMZ)
            2 -> exportEntityData(DataExportFormat.CSV)
        }
    }

    fun onSelectFolderToExportData(selectedFolderIndex: Int) {
        val command = Command.ShowEntityDataExportOptionsDialog(Entity.Folder(selectedFolderIndex))
        setState { copy(viewCommand = command) }
    }

    fun onCompleteTakeMapShot(bitmap: Bitmap) {
        exporter.exportSnapshotBitmap(bitmap).executeExport()
    }

    private fun showSelectFolderToExport(foldersWithMarkers: List<FolderWithMarkers>) {
        setState { copy(viewCommand = Command.ShowSelectFolderToExportDialog(foldersWithMarkers)) }
    }

    private fun shareMarkerAsPlainText(data: List<FolderWithMarkers>) {
        val markerId = state.markerId
        val marker = data.flatMap { it.markers }.find { it.id == markerId }!!

        val text = """
            ${marker.title}
            
            ${marker.formattedCreatedAt}
            lat/lng: (${marker.position!!.latitude}, ${marker.position!!.longitude})
            http://maps.google.com/?q=${marker.position!!.latitude},${marker.position!!.longitude}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val intent = Intent.createChooser(shareIntent, "")
        setState { copy(viewCommand = Command.ShareData(intent)) }
    }

    private fun exportEntityData(format: DataExportFormat) = usingAllFoldersWithMarkers {
        val entity = (state.viewCommand as Command.ShowEntityDataExportOptionsDialog).entity
        val (name, data) = entity.processExportData(it)

        when (format) {
            is DataExportFormat.KML -> exporter.exportKml(data, name).executeExport()
            is DataExportFormat.KMZ -> exporter.exportKmz(data, name).executeExport()
            is DataExportFormat.CSV -> {
                val finalData = data.map { folder ->
                    folder.copy(markers = folder.markers.filter { marker -> marker.isMarker })
                }
                exporter.exportCsv(finalData).executeExport()
            }
        }
    }

    private fun Entity.processExportData(initialData: List<FolderWithMarkers>) = when (this) {
        is Entity.AllFilesAndFolder -> "All markers and folders" to initialData
        is Entity.Marker -> {
            val markerId = state.markerId
            val folder =
                initialData.find { it.markers.find { marker -> marker.id == markerId } != null }!!
            val folderWithMarker =
                folder.copy(markers = folder.markers.filter { it.id == markerId })
            folderWithMarker.markers[0].title to listOf(folderWithMarker)
        }
        is Entity.Folder -> {
            val folderIndex = folderIndex
            val folder = state.getAllFoldersAsyncResult()!![folderIndex]
            folder.folder.title to listOf(folder)
        }

    }


    private fun Single<Intent>.executeExport() {
        this
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(exportAsyncResult = it) }
    }

    private fun usingAllFoldersWithMarkers(action: (folders: List<FolderWithMarkers>) -> Unit) {
        state.getAllFoldersAsyncResult()
            ?.let { action(it) }
            ?: {
                setState { copy(actionAwaitingData = action) }
                getAllFoldersWithMarkers()
            }()
    }

    private fun getAllFoldersWithMarkers() {
        foldersRepository
            .getAllFoldersWithMarkers()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getAllFoldersAsyncResult = it) }
    }

    data class State(
        val markerId: Long? = null,
        val markerIds: List<Long>? = null,
        val getAllFoldersAsyncResult: Async<List<FolderWithMarkers>> = Uninitialized,
        val viewCommand: Command? = null,
        val exportAsyncResult: Async<Intent> = Uninitialized,
        val actionAwaitingData: ((list: List<FolderWithMarkers>) -> Unit)? = null
    ) : MvRxState

    companion object : MvRxViewModelFactory<DataSharingViewModel, State>, KoinComponent {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): DataSharingViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val exporter = Exporter(DataSerializer(Xml.newSerializer()), CsvWriter(), context)
            return DataSharingViewModel(state, fragment.get(), exporter)
        }

        sealed class Command {
            object ShowInitialShareOptionsDialog : Command()
            object ShowInitialShareOptionsWithMarkerOptionsDialog : Command()
            data class ShowEntityDataExportOptionsDialog(val entity: Entity) : Command() {
                sealed class Entity {
                    object Marker : Entity()
                    object AllFilesAndFolder : Entity()
                    data class Folder(val folderIndex: Int) : Entity()

                }
            }

            data class ShowSelectFolderToExportDialog(val foldersWithMarkers: List<FolderWithMarkers>) :
                Command()

            data class ShareData(val data: Intent) : Command()
            object TakeMapSnapshot : Command()
        }

        sealed class DataExportFormat {
            object KML : DataExportFormat()
            object KMZ : DataExportFormat()
            object CSV : DataExportFormat()
        }
    }
}
