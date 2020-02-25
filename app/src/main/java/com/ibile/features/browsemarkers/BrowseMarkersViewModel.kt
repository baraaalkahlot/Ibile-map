package com.ibile.features.browsemarkers

import com.airbnb.mvrx.*
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import com.jakewharton.rxrelay2.PublishRelay
import org.koin.android.ext.android.inject

data class BrowseMarkersViewModelState(
    val markersAsync: Async<List<Marker>> = Uninitialized,
    val searchQuery: String = ""
) : MvRxState

class BrowseMarkersViewModel(
    initialState: BrowseMarkersViewModelState,
    private val markersRepository: MarkersRepository
) : BaseMvRxViewModel<BrowseMarkersViewModelState>(initialState) {

    private val searchQuerySubject: PublishRelay<String> = PublishRelay.create()

    val state: BrowseMarkersViewModelState
        get() = withState(this) { it }

    fun init() {
        withState { state ->
            if (state.markersAsync is Success) return@withState
            markersRepository
                .getAllMarkers()
                .toObservable()
                .execute { copy(markersAsync = it) }
        }
        searchQuerySubject.execute { it -> copy(searchQuery = it() ?: "") }
    }

    fun setSearchQuery(query: String) {
        searchQuerySubject.accept(query)
    }

    companion object : MvRxViewModelFactory<BrowseMarkersViewModel, BrowseMarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: BrowseMarkersViewModelState
        ): BrowseMarkersViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val repo by fragment.inject<MarkersRepository>()
            return BrowseMarkersViewModel(state, repo)
        }
    }
}
