package com.ibile.features.createimportedmarker

import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.createimportedmarker.CreateImportedMarkerViewModel.State
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class CreateImportedMarkerViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository
) : BaseViewModel<State>(initialState) {

    fun handleCreateMarkerBtnClick(coords: LatLng) {
        val marker = Marker.createMarker(coords).copy(name = "Imported marker")
        markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(createMarkerAsyncResult = it) }
    }

    data class State(val createMarkerAsyncResult: Async<Long> = Uninitialized) : MvRxState

    companion object : MvRxViewModelFactory<CreateImportedMarkerViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): CreateImportedMarkerViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return CreateImportedMarkerViewModel(
                state,
                fragment.get()
            )
        }
    }
}
