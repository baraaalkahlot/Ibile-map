package com.ibile

import android.net.Uri
import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.ImageRepository
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

data class MarkersViewModelState(
    val markersAsync: Async<List<Marker>> = Uninitialized,
    val activeMarkerId: Long? = null,
    val addMarkerAsync: Async<Long> = Uninitialized,
    val markerForEdit: Marker? = null,
    val markerUpdateAsync: Async<Unit> = Uninitialized,
    val importMarkerImagesAsync: Async<List<Uri>> = Uninitialized
) : MvRxState

class MarkersViewModel(
    initialState: MarkersViewModelState,
    private val markersRepository: MarkersRepository,
    private val imageRepository: ImageRepository
) : BaseMvRxViewModel<MarkersViewModelState>(initialState) {

    val state: MarkersViewModelState
        get() = withState(this) { it }

    fun init() {
        withState { state ->
            if (state.markersAsync is Success) return@withState
            markersRepository
                .getAllMarkers()
                .toObservable()
                .execute { copy(markersAsync = it) }
        }
    }

    init {
        asyncSubscribe(MarkersViewModelState::importMarkerImagesAsync) {
            editMarker { copy(imageUris = imageUris.toMutableList().apply { addAll(it) }) }
        }
    }

    fun addMarker(markerCoords: LatLng) {
        val newMarker = Marker.createMarker(markerCoords)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun addPolyline(points: List<LatLng?>) {
        val newMarker = Marker.createPolyline(points)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun addPolygon(points: List<LatLng?>) {
        val newMarker = Marker.createPolygon(points)
        markersRepository
            .insertMarker(newMarker)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(addMarkerAsync = it) }
    }

    fun resetAddMarkerAsync() = setState { copy(addMarkerAsync = Uninitialized) }

    fun setActiveMarkerId(activeMarkerId: Long?) {
        setState { copy(activeMarkerId = activeMarkerId) }
    }

    fun getActiveMarker(): Marker? {
        return withState(this) { state ->
            val (markersAsync, activeMarkerId) = state
            activeMarkerId?.let { markersAsync()?.find { it.id == activeMarkerId } }
        }
    }

    fun getMarkerById(id: Long?): Marker = state.markersAsync()!!.find { it.id == id }!!

    fun setMarkerForEdit(marker: Marker?) {
        if (marker != null) {
            setState { copy(markerForEdit = marker, activeMarkerId = null, markerUpdateAsync = Uninitialized) }
            return
        }
        cleanUpUnsavedMarkerImages() // called when back is pressed on edit fragment
        setState {
            copy(activeMarkerId = markerForEdit!!.id, markerForEdit = null, markerUpdateAsync = Uninitialized)
        }
    }

    fun editMarker(cb: Marker.() -> Marker) = setState {
        val marker = cb(state.markerForEdit!!)
        copy(markerForEdit = marker)
    }

    fun onChooseMarkerImages(uris: List<Uri>) = imageRepository.importImagesToApp(uris)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .execute { copy(importMarkerImagesAsync = it) }


    fun saveMarkerForEdit() {
        val marker = state.markerForEdit!!
        cleanUpDeletedMarkerImages()
        markersRepository
            .updateMarker(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(markerUpdateAsync = it) }
    }

    private fun cleanUpUnsavedMarkerImages() {
        val marker = state.markerForEdit!!
        val originalMarkerUris = getMarkerById(marker.id).imageUris
        val unsavedUris = marker.imageUris.filter { uri -> !originalMarkerUris.contains(uri) }
        if (unsavedUris.isNotEmpty())
            imageRepository.deleteFiles(unsavedUris).subscribeOn(Schedulers.io()).subscribe()
    }

    private fun cleanUpDeletedMarkerImages() {
        val marker = state.markerForEdit!!
        val originalMarkerUris = getMarkerById(marker.id).imageUris
        val deletedUris = originalMarkerUris.filter { !marker.imageUris.contains(it) }
        if (deletedUris.isNotEmpty())
            imageRepository.deleteFiles(deletedUris).subscribeOn(Schedulers.io()).subscribe()
    }

    fun deleteUnsavedMarkerImage(uri: Uri) {
        imageRepository.deleteFiles(listOf(uri)).subscribeOn(Schedulers.io()).subscribe()
    }

    companion object : MvRxViewModelFactory<MarkersViewModel, MarkersViewModelState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext, state: MarkersViewModelState
        ): MarkersViewModel {
            val activity = (viewModelContext as ActivityViewModelContext).activity
            val repo by activity.inject<MarkersRepository>()
            val imageRepository by activity.inject<ImageRepository>()
            return MarkersViewModel(state, repo, imageRepository)
        }
    }
}
