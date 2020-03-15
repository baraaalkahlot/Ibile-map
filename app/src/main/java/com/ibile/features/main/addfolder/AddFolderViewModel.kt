package com.ibile.features.main.addfolder

import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Folder
import com.ibile.data.repositiories.FoldersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddFolderViewModel(initialState: State, private val foldersRepository: FoldersRepository) :
    BaseViewModel<AddFolderViewModel.State>(initialState) {

    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) { folder.set(it) }
    }

    fun addFolder(folder: Folder) {
        foldersRepository.addFolder(folder)
            .subscribeOn(Schedulers.io())
            .execute { copy(addFolderAsync = it) }
    }

    data class State(
        val folder: Folder = Folder(""),
        val addFolderAsync: Async<Long> = Uninitialized
    ) : MvRxState

    companion object : MvRxViewModelFactory<AddFolderViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): AddFolderViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddFolderViewModel(state, fragment.get())
        }
    }
}
