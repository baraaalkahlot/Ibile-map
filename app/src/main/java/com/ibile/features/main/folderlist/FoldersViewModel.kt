package com.ibile.features.main.folderlist

import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Folder
import com.ibile.data.repositiories.FoldersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class FoldersViewModel(initialState: State, private val foldersRepository: FoldersRepository) :
    BaseViewModel<FoldersViewModel.State>(initialState) {

    fun getFolders() {
        foldersRepository
            .getAllFoldersWithMarkersCount()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFoldersAsync = it) }
    }

    fun updateFolders(vararg folder: Folder) {
        foldersRepository.updateFolders(*folder)
            .subscribeOn(Schedulers.io())
            .execute { copy() }
    }

    data class State(
        val getFoldersAsync: Async<List<FolderWithMarkersCount>> = Uninitialized,
        val addFolderAsync: Async<Long> = Uninitialized
    ) :
        MvRxState

    companion object : MvRxViewModelFactory<FoldersViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): FoldersViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return FoldersViewModel(state, fragment.get())
        }
    }
}
