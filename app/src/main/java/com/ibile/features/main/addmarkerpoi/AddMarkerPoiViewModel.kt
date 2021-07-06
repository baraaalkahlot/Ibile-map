package com.ibile.features.main.addmarkerpoi

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.google.android.libraries.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.ibile.DEFAULT_DB_NAME
import com.ibile.USERS_COLLECTION
import com.ibile.USERS_MARKERS
import com.ibile.core.BaseViewModel
import com.ibile.data.SharedPref
import com.ibile.data.database.entities.ConvertedFirebaseMarker
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker
import com.ibile.data.database.entities.Marker.Icon
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MapFile
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get


class AddMarkerPoiViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository,
    private val foldersRepository: FoldersRepository,
    private val sharedPref: SharedPref,
    private val context: Context
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
            val folder = it.find { folder -> folder.id == state.marker?.folderId }
            updateState { copy(targetFolder = folder) }
        }
        selectSubscribe(State::mode) {
            targetFolderIsVisible.set(it is AddMarkerPoiPresenter.Mode.Add)
        }
    }


    fun addMarker(marker: Marker, targetFolder: FolderWithMarkersCount?, mapFile: MapFile?) {
        val id = markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .blockingGet()
        Log.d("wasd", "addMarker: id = $id")
        addToFirebase(marker, id, targetFolder, mapFile)
    }

    fun addMarkerToRoomOnly(marker: Marker) {
        val id = markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .blockingGet()
    }

    fun updateMarker(marker: Marker) {
        markersRepository
            .updateMarkers(marker)
            .subscribeOn(Schedulers.io())
            .execute { copy() }
        updateToFirebase(marker)
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
        ): AddMarkerPoiViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddMarkerPoiViewModel(
                state,
                fragment.get(),
                fragment.get(),
                fragment.get(),
                viewModelContext.activity
            )
        }
    }

    private fun addToFirebase(
        marker: Marker,
        markerId: Long,
        targetFolder: FolderWithMarkersCount?,
        mapFile: MapFile?
    ) {
        val convertedFirebaseMarker: ConvertedFirebaseMarker
        marker.apply {
            val arrayOfGeoPoint: ArrayList<GeoPoint> = arrayListOf()
            for (p: LatLng? in points) {
                arrayOfGeoPoint.add(GeoPoint(p?.latitude!!, p.longitude))
            }

            val arrayOfImagePath: ArrayList<String> = arrayListOf()

            for (i: Uri? in imageUris) {
                arrayOfImagePath.add(i.toString())
            }

            convertedFirebaseMarker = ConvertedFirebaseMarker(
                id = markerId,
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
        }

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")
        val db = FirebaseFirestore.getInstance()
        val doc: DocumentReference = db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(marker.folderId.toString())


        val currentFolder = targetFolder?.title?.let {
            Folder(
                title = it,
                selected = targetFolder.selected,
                color = targetFolder.color,
                iconId = targetFolder.iconId,
                id = targetFolder.id
            )
        }
        currentFolder?.let { doc.set(it) }
        doc.collection(USERS_MARKERS)
            .document(markerId.toString())
            .set(convertedFirebaseMarker)
            .addOnSuccessListener {
                Log.d("wasd", "success")

                val mainCollection = db.collection(USERS_COLLECTION)
                    .document(userEmail)

                Log.d(
                    "wasd",
                    "addFolder: current db name = ${mapFile?.dbName}"
                )

                if (mapFile == null) return@addOnSuccessListener

                mainCollection.collection(sharedPref.currentMapFileId.toString())
                    .document(sharedPref.currentMapFileId.toString())
                    .set(mapFile)

                mainCollection.get().addOnCompleteListener { values ->
                    if (values.isSuccessful) {
                        Log.d("wasd", "onClickCreateNewMapViewPositiveBtn: ohhh yeah")
                        val document: DocumentSnapshot = values.result!!
                        val list: java.util.ArrayList<String> = if (document.get("id") != null) {
                            document.get("id") as java.util.ArrayList<String>
                        } else {
                            arrayListOf()
                        }

                        for (i in list) {
                            if (i == sharedPref.currentMapFileId.toString() || mapFile.dbName != DEFAULT_DB_NAME) return@addOnCompleteListener
                        }
                        list.add(sharedPref.currentMapFileId.toString())

                        val data = mapOf(
                            "id" to list,
                            "isActive" to document.get("isActive")
                        )
                        mainCollection.set(data)
                    }
                }
            }
    }


    private fun updateToFirebase(marker: Marker) {
        val convertedFirebaseMarker: ConvertedFirebaseMarker
        marker.apply {
            val arrayOfGeoPoint: ArrayList<GeoPoint> = arrayListOf()
            for (p: LatLng? in points) {
                arrayOfGeoPoint.add(GeoPoint(p?.latitude!!, p.longitude))
            }

            val arrayOfImagePath: ArrayList<String> = arrayListOf()

            for (i: Uri? in imageUris) {
                arrayOfImagePath.add(i.toString())
            }

            Log.d("wasd", "updateToFirebase: marker edited succesfully")
            convertedFirebaseMarker = ConvertedFirebaseMarker(
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
        }

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")
        val db = FirebaseFirestore.getInstance()
        db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(marker.folderId.toString())
            .collection(USERS_MARKERS)
            .document(marker.id.toString())
            .set(convertedFirebaseMarker)
    }
}
