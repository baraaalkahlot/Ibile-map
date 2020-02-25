package com.ibile.features.addpolylinepoi

import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.addpolylinepoi.AddPolylinePoiViewModel.State
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddPolylinePoiViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository
) : BaseViewModel<State>(initialState) {

    fun addMarker(marker: Marker) {
        markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    data class State(
        val points: List<LatLng> = listOf(),
        val activePointIndex: Int = -1,
        val cameraPosition: LatLng = LatLng(0.0, 0.0),
        val addMarkerAsync: Async<Long> = Uninitialized
    ) : MvRxState

    companion object : MvRxViewModelFactory<AddPolylinePoiViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): AddPolylinePoiViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddPolylinePoiViewModel(state, fragment.get())
        }
    }
}
