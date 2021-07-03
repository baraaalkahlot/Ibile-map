package com.ibile.features.main.addpolylinepoi

import android.content.Context
import android.net.Uri
import android.util.Log
import com.airbnb.mvrx.*
import com.google.android.apps.gmm.map.util.jni.NativeHelper.context
import com.google.android.libraries.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.ibile.USERS_COLLECTION
import com.ibile.USERS_MARKERS
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.ConvertedFirebaseMarker
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.addpolylinepoi.AddPolylinePoiViewModel.State
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddPolylinePoiViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository
) : BaseViewModel<State>(initialState) {

    fun addMarker(marker: Marker) {
       val id =  markersRepository
            .insertMarker(marker)
            .subscribeOn(Schedulers.io())
            .blockingGet()
        addToFirebase(marker, id)

    }

    fun updateMarker(marker: Marker) {
        val id = markersRepository
            .updateMarkers(marker)
            .subscribeOn(Schedulers.io())
            .blockingGet()
        updateToFirebase(marker)

    }

    data class State(
        val points: List<LatLng> = listOf(),
        val activePointIndex: Int = -1,
        val cameraPosition: LatLng = LatLng(0.0, 0.0),
        val addMarkerAsync: Async<Long> = Uninitialized,
        val mode: AddPolylinePoiPresenter.Mode = AddPolylinePoiPresenter.Mode.Add,
        val updateMarkerAsync: Async<Unit> = Uninitialized
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

    private fun addToFirebase(
        marker: com.ibile.data.database.entities.Marker,
        markerId: Long
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
            .collection("file")
            .document(marker.folderId.toString())


        doc.collection(USERS_MARKERS)
            .document(markerId.toString())
            .set(convertedFirebaseMarker)
    }


    private fun updateToFirebase(marker: com.ibile.data.database.entities.Marker) {
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
            .collection("file")
            .document(marker.folderId.toString())
            .collection(USERS_MARKERS)
            .document(marker.id.toString())
            .set(convertedFirebaseMarker)
    }
}
