package com.ibile.features.editmarker

import android.content.Context
import android.net.Uri
import android.util.Log
import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.ibile.USERS_COLLECTION
import com.ibile.USERS_MARKERS
import com.ibile.core.BaseViewModel
import com.ibile.data.SharedPref
import com.ibile.data.database.entities.ConvertedFirebaseMarker
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
    private val foldersRepository: FoldersRepository,
    private val sharedPref: SharedPref,
    private val context: Context
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
        Log.d("wasd", "updateMarker: test2")
        updateOrAddMarker(marker, 0L)
    }

    fun deleteMarker(marker: Marker) {
        if (marker.imageUris.isNotEmpty()) imageRepository.deleteFiles(marker.imageUris)
        markersRepository
            .deleteMarkers(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy(deleteMarkerAsync = it) }


        val db = FirebaseFirestore.getInstance()
        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(marker.folderId.toString())
            .collection(USERS_MARKERS)
            .document(marker.id.toString())
            .delete()

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
            return EditMarkerViewModel(
                state,
                fragment.get(),
                fragment.get(),
                fragment.get(),
                fragment.get(),
                viewModelContext.activity
            )
        }

        override fun initialState(viewModelContext: ViewModelContext): EditMarkerViewModelState? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val markerId = fragment.arguments?.get(ARG_MARKER_ID) as Long
            return EditMarkerViewModelState(markerId)
        }

        const val ARG_MARKER_ID = "ARG_MARKER_ID"
    }

    private fun updateOrAddMarker(marker: Marker, markerId: Long) {
        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        marker.apply {
            val arrayOfGeoPoint: ArrayList<GeoPoint> = arrayListOf()
            for (p: LatLng? in points) {
                arrayOfGeoPoint.add(GeoPoint(p?.latitude!!, p.longitude))
            }

            val arrayOfImagePath: ArrayList<String> = arrayListOf()

            for (i: Uri? in imageUris) {
                arrayOfImagePath.add(i.toString())
            }


            Log.d("wasd", "updateOrAddMarker: hey i'm here!!")
            val convertedFirebaseMarker = ConvertedFirebaseMarker(
                id = id,
                points = arrayOfGeoPoint,
                type = type,
                createdAt = createdAt,
                updatedAt = updatedAt,
                description = description,
                color = color,
                icon = icon?.id,
                phoneNumber = phoneNumber,
                imageUris = arrayOfImagePath,
                folderId = folderId
            )
            val db = FirebaseFirestore.getInstance()

            val schemaId = if (markerId == 0L) id else markerId

            db.collection(USERS_COLLECTION)
                .document(userEmail!!)
                .collection(sharedPref.currentMapFileId.toString())
                .document(folderId.toString())
                .collection(USERS_MARKERS)
                .document(schemaId.toString())
                .set(convertedFirebaseMarker)
                .addOnSuccessListener {
                    Log.d("wasd", "success")
                }
                .addOnFailureListener { e ->
                    Log.w("wasd", "Error adding document", e)
                }
        }
    }
}
