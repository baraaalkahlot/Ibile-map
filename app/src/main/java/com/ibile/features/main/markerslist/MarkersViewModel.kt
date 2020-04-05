package com.ibile.features.main.markerslist

import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class MarkersViewModel(initialState: State, private val markersRepository: MarkersRepository) :
    BaseViewModel<MarkersViewModel.State>(initialState) {

    val activeMarker: ObservableField<Marker?> = ObservableField()

    init {
        selectSubscribe(State::activeMarkerId) {
            val marker = state.markersListAsync()?.find { element -> element.id == it }
            activeMarker.set(marker)
        }
        asyncSubscribe(State::markersListAsync) {
            val marker = it.find { element -> element.id == this.state.activeMarkerId }
            activeMarker.set(marker)
        }
    }

    fun getMarkers() {
        markersRepository
            .getAllMarkers()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(markersListAsync = it) }
    }

    data class State(
        val activeMarkerId: Long? = null,
        val markersListAsync: Async<List<Marker>> = Uninitialized,
        val editMarkerId: Long? = null
    ) : MvRxState

    companion object : MvRxViewModelFactory<MarkersViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): MarkersViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return MarkersViewModel(state, fragment.get())
        }
    }
}
