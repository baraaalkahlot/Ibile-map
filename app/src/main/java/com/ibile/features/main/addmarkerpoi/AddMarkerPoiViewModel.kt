package com.ibile.features.main.addmarkerpoi

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.database.entities.Marker.Icon
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddMarkerPoiViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository,
    private val foldersRepository: FoldersRepository
) :
    BaseViewModel<AddMarkerPoiViewModel.State>(initialState) {

    val targetFolderObservable: ObservableField<FolderWithMarkersCount> = ObservableField()
    val markerObservable: ObservableField<Marker> = ObservableField()
    val targetFolderIsVisible = ObservableBoolean()

    init {
        selectSubscribe(State::marker) { markerObservable.set(it) }
        selectSubscribe(State::targetFolder) {
            targetFolderObservable.set(it)
            if (it != null) {
                val marker = state.marker?.copy(
                    icon = Icon(it.iconId, true),
                    color = it.color,
                    folderId = it.id
                )
                updateState { copy(marker = marker) }
            }
        }
        asyncSubscribe(State::getFoldersAsyncResult) {
            val folder = it.find { folder -> folder.id == state.marker?.folderId }!!
            updateState { copy(targetFolder = folder) }
        }
        selectSubscribe(State::mode) {
            targetFolderIsVisible.set(it is AddMarkerPoiPresenter.Mode.Add)
        }
    }

    fun addMarker(marker: Marker) {
        markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy() }
    }

    fun updateMarker(marker: Marker) {
        markersRepository
            .updateMarkers(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy() }
    }

    fun getFolders() {
        foldersRepository
            .getAllFoldersWithMarkersCount()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFoldersAsyncResult = it) }
    }

    data class State(
        val mode: AddMarkerPoiPresenter.Mode = AddMarkerPoiPresenter.Mode.Add,
        val marker: Marker? = null,
        val getFoldersAsyncResult: Async<List<FolderWithMarkersCount>> = Uninitialized,
        val targetFolder: FolderWithMarkersCount? = null
    ) : MvRxState

    companion object : MvRxViewModelFactory<AddMarkerPoiViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): AddMarkerPoiViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddMarkerPoiViewModel(state, fragment.get(), fragment.get())
        }
    }
}
