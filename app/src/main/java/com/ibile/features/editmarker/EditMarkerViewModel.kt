package com.ibile.features.editmarker

import android.net.Uri
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.*
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.ImageRepository
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

data class EditMarkerViewModelState(
    val markerId: Long,
    val getMarkerAsync: Async<Marker> = Uninitialized,
    val marker: Marker? = null,
    val importMarkerImagesAsync: Async<List<Uri>> = Uninitialized,
    val updateMarkerAsync: Async<Unit> = Uninitialized,
    val deleteMarkerAsync: Async<Unit> = Uninitialized
) : MvRxState

class EditMarkerViewModel(
    initialState: EditMarkerViewModelState,
    private val markersRepository: MarkersRepository,
    private val imageRepository: ImageRepository
) : BaseMvRxViewModel<EditMarkerViewModelState>(initialState) {

    val state: EditMarkerViewModelState
        get() = withState(this) { it }

    init {
        markersRepository
            .getMarker(state.markerId)
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

    fun onChooseMarkerImages(uris: List<Uri>) = imageRepository.importImagesToApp(uris)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .execute { copy(importMarkerImagesAsync = it) }

    fun updateMarker(marker: Marker) {
        cleanUpDeletedMarkerImages(marker)
        markersRepository
            .updateMarker(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(updateMarkerAsync = it) }
    }

    fun deleteMarker(marker: Marker) {
        if (marker.imageUris.isNotEmpty()) imageRepository.deleteFiles(marker.imageUris)
        markersRepository
            .deleteMarker(marker)
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
            val activity = (viewModelContext as FragmentViewModelContext).activity
            val repo by activity.inject<MarkersRepository>()
            val imageRepository by activity.inject<ImageRepository>()
            return EditMarkerViewModel(state, repo, imageRepository)
        }

        override fun initialState(viewModelContext: ViewModelContext): EditMarkerViewModelState? {
            val editMarkerDialogFragment = (viewModelContext as FragmentViewModelContext)
                .fragment as EditMarkerDialogFragment
            val args by editMarkerDialogFragment.navArgs<EditMarkerDialogFragmentArgs>()
            val markerId = args.markerId
            return EditMarkerViewModelState(markerId)
        }
    }
}
