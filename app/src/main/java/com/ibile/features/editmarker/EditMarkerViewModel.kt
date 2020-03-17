package com.ibile.features.editmarker

import android.net.Uri
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.ImageRepository
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

data class EditMarkerViewModelState(
    val markerId: Long,
    val getMarkerAsync: Async<Marker> = Uninitialized,
    val marker: Marker? = null,
    val importMarkerImagesAsync: Async<List<Uri>> = Uninitialized,
    val updateMarkerAsync: Async<Unit> = Uninitialized,
    val deleteMarkerAsync: Async<Unit> = Uninitialized,
    val markerImagesInSelectMode: Boolean = false,
    val selectedImages: List<Uri> = arrayListOf(),
    val getFoldersAsyncResult: Async<List<FolderWithMarkersCount>> = Uninitialized
) : MvRxState

class EditMarkerViewModel(
    initialState: EditMarkerViewModelState,
    private val markersRepository: MarkersRepository,
    private val imageRepository: ImageRepository,
    private val foldersRepository: FoldersRepository
) : BaseViewModel<EditMarkerViewModelState>(initialState) {

    fun getMarker(markerId: Long) {
        markersRepository
            .getMarker(markerId)
            .subscribeOn(Schedulers.io())
            .execute { copy(marker = it()?.copy(), getMarkerAsync = it) }

        asyncSubscribe(EditMarkerViewModelState::importMarkerImagesAsync) {
            editMarker { copy(imageUris = imageUris.toMutableList().apply { addAll(it) }) }
        }
    }

    fun editMarker(cb: Marker.() -> Marker) = setState {
        val marker = cb(state.marker!!)
        copy(marker = marker)
    }

    fun getFolders() {
        foldersRepository
            .getAllFoldersWithMarkersCount()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFoldersAsyncResult = it) }
    }

    fun onChooseMarkerImages(uris: List<Uri>) = imageRepository.importImagesToApp(uris)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .execute { copy(importMarkerImagesAsync = it) }

    fun updateMarker(marker: Marker) {
        cleanUpDeletedMarkerImages(marker)
        markersRepository
            .updateMarkers(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(updateMarkerAsync = it) }
    }

    fun deleteMarker(marker: Marker) {
        if (marker.imageUris.isNotEmpty()) imageRepository.deleteFiles(marker.imageUris)
        markersRepository
            .deleteMarkers(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(deleteMarkerAsync = it) }
    }

    private fun cleanUpDeletedMarkerImages(marker: Marker) {
        val originalMarkerUris = state.getMarkerAsync()!!.imageUris
        val deletedUris = originalMarkerUris.filter { !marker.imageUris.contains(it) }
        if (deletedUris.isNotEmpty())
            imageRepository.deleteFiles(deletedUris).subscribeOn(Schedulers.io()).subscribe()
    }

    fun cleanUpUnsavedMarkerImages() {
        val originalMarkerUris = state.getMarkerAsync()!!.imageUris
        val unsavedUris =
            state.marker!!.imageUris.filter { uri -> !originalMarkerUris.contains(uri) }
        if (unsavedUris.isNotEmpty())
            imageRepository.deleteFiles(unsavedUris).subscribeOn(Schedulers.io()).subscribe()
    }

    fun deleteUnsavedMarkerImage(uri: Uri) {
        imageRepository.deleteFiles(listOf(uri)).subscribeOn(Schedulers.io()).subscribe()
    }

    companion object : MvRxViewModelFactory<EditMarkerViewModel, EditMarkerViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: EditMarkerViewModelState
        ): EditMarkerViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return EditMarkerViewModel(state, fragment.get(), fragment.get(), fragment.get())
        }

        override fun initialState(viewModelContext: ViewModelContext): EditMarkerViewModelState? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val markerId = fragment.arguments?.get(ARG_MARKER_ID) as Long
            return EditMarkerViewModelState(markerId)
        }

        const val ARG_MARKER_ID = "ARG_MARKER_ID"
    }
}
