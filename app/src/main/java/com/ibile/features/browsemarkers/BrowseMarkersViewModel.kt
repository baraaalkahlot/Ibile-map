package com.ibile.features.browsemarkers

import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.FoldersRepository
import com.jakewharton.rxrelay2.PublishRelay
import org.koin.android.ext.android.get

data class BrowseMarkersViewModelState(
    val markersAsync: Async<List<Marker>> = Uninitialized,
    val searchQuery: String = "",
    val getFoldersAsync: Async<List<FolderWithMarkers>> = Uninitialized
) : MvRxState

class BrowseMarkersViewModel(
    initialState: BrowseMarkersViewModelState,
    private val foldersRepository: FoldersRepository
) : BaseViewModel<BrowseMarkersViewModelState>(initialState) {

    fun getAllFolders() {
        foldersRepository
            .getAllSelectedFoldersWithMarkers()
            .toObservable()
            .execute { copy(getFoldersAsync = it) }
    }

    companion object : MvRxViewModelFactory<BrowseMarkersViewModel, BrowseMarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: BrowseMarkersViewModelState
        ): BrowseMarkersViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return BrowseMarkersViewModel(state, fragment.get())
        }
    }
}
