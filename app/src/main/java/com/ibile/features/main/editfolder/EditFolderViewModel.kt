package com.ibile.features.main.editfolder

import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class EditFolderViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val markersRepository: MarkersRepository
) :
    BaseViewModel<EditFolderViewModel.State>(initialState) {

    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) {
            folder.set(it)
        }
    }

    fun getFolder(folderId: Long) {
        foldersRepository.getFolder(folderId)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFolderAsync = it) }
    }

    fun updateFolder(folder: Folder) {
        foldersRepository
            .updateFolders(folder)
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }
    }

    fun updateFolderWithMarkers(folder: Folder, updateMarkers: (List<Marker>) -> List<Marker>) {
        markersRepository
            .getMarkersByFolderId(folder.id)
            .flatMapCompletable { markersRepository.updateMarkers(*updateMarkers(it).toTypedArray()) }
            .concatWith(foldersRepository.updateFolders(folder))
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }
    }

    fun deleteFolderWithMarkers(folder: Folder) {
        markersRepository
            .getMarkersByFolderId(folder.id)
            .flatMapCompletable { markersRepository.deleteMarkers(*it.toTypedArray()) }
            .concatWith(foldersRepository.deleteFolders(folder))
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }
    }

    data class State(
        val folderId: Long,
        val folder: Folder = Folder(""),
        val getFolderAsync: Async<Folder> = Uninitialized,
        val updateFolderAsync: Async<Unit> = Uninitialized
    ) :
        MvRxState

    companion object : MvRxViewModelFactory<EditFolderViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): EditFolderViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return EditFolderViewModel(state, fragment.get(), fragment.get())
        }

        override fun initialState(viewModelContext: ViewModelContext): State? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val folderId = fragment.arguments?.getLong(EditFolderDialogFragment.ARG_FOLDER_ID)!!
            return State(folderId)
        }
    }
}
