package com.ibile.features.dataimport

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
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
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MapFile
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.dataimport.DataImportViewModel.State
import com.ibile.features.main.editfolder.EditFolderViewModel
import de.siegmar.fastcsv.reader.CsvReader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class DataImportViewModel(
    initialState: State,
    private val dataProcessor: DataProcessor,
    private val context: Context,
    private val markersRepository: MarkersRepository,
    private val foldersRepository: FoldersRepository,
    private val sharedPref: SharedPref
) :
    BaseViewModel<State>(initialState) {


    init {
        // not ideal to use this here, but since there is no view controller subscribed by the
        // time this completes, this is the simplest option for now. Context is also from DI/app,
        // which makes it slightly safe. Safest option is to tie this viewmodel with the activity
        // (hard to do with MvRx) and issue commands from there.
        asyncSubscribe(State::dataImportAsyncResult, {

//            Toast.makeText(context, "Data import failed", Toast.LENGTH_SHORT).show()

        }) {

//            Toast.makeText(context, "Data import completed successfully!", Toast.LENGTH_SHORT).show()

        }
    }

    fun onNewIntent(intent: Intent) {
        if (intent.scheme == "geo") return handleLocationIntent(intent)

        val uri = intent.data ?: intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        val mimeType = uri?.let { context.contentResolver.getType(it) }

        setState {
            val canProcess = supportedMimeTypes.contains(mimeType)
            if (canProcess)
                copy(
                    viewCommand = ViewCommand.ShowImportConfirmationDialog,
                    dataMimeType = mimeType,
                    dataUri = uri
                )
            else copy(viewCommand = ViewCommand.Exit)
        }
    }

    fun onClickImportConfirmationDialogPositive(
        mapFile: MapFile?,
        editFolderViewModel: EditFolderViewModel
    ) {


        setState { copy(viewCommand = ViewCommand.DismissImportConfirmationDialog) }
        when (state.dataMimeType) {
            CSV_MIME_TYPE -> handleCsvImport(mapFile, editFolderViewModel)
            KML_MIME_TYPE -> handleKmlImport()
            KMZ_MIME_TYPE -> handleKmzImport()
        }
    }


    fun handleProgressBar(progressBarHandler: ProgressBarHandler){

//        progressBarHandler.show()

        asyncSubscribe(State::dataImportAsyncResult, {

            Toast.makeText(context, "Data import failed ", Toast.LENGTH_SHORT).show()
            progressBarHandler.hide()

        }) {

            Toast.makeText(context, "Data import completed successfully!", Toast.LENGTH_SHORT).show()
            progressBarHandler.hide()
        }

    }


    fun onClickImportConfirmationDialogNegative() {
        /* no-op */
    }


    //TODO Clone data from file and uplode it to firebase
    @SuppressLint("CheckResult")
    private fun handleCsvImport(mapFile: MapFile?, editFolderViewModel: EditFolderViewModel) {
        val uri = state.dataUri
        var markers: List<Marker> = listOf()
        val markIds = dataProcessor.processCsv(uri!!)
            .flatMap {
                markers = it.distinctBy { marker -> MarkerDistinctSelector.fromMarker(marker) }
                markersRepository.insertMarkers(*markers.toTypedArray())
            }
            .subscribeOn(Schedulers.io())
            .blockingGet()

        markers.withIndex().forEach { (index, m) ->
            val folder = editFolderViewModel.getFolderById(m.folderId)
            addToFirebase(m, markIds[index], folder, mapFile)
            Log.d("wasd", "handleCsvImport: index  = $index list size = $markIds")
        }
    }

    @SuppressLint("CheckResult")
    private fun handleKmlImport() {
        dataProcessor.processKml(state.dataUri!!)
            .flatMapCompletable {
                foldersRepository.insertMarkersWithFolders(it)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(dataImportAsyncResult = it) }
    }

    @SuppressLint("CheckResult")
    private fun handleKmzImport() {
        dataProcessor.processKmz(state.dataUri!!)
            .flatMapCompletable {
                foldersRepository.insertMarkersWithFolders(it)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(dataImportAsyncResult = it) }
    }

    private fun handleLocationIntent(intent: Intent) {
        val latLng = intent.data?.toString()
            ?.removePrefix("geo:")
            ?.replaceAfterLast("?", "")
            ?.trimEnd('?')
            ?.split(",")
        if (latLng == null || latLng.size < 2) return
        val latitude = latLng[0].toDoubleOrNull()
        val longitude = latLng[1].toDoubleOrNull()

        val coordIsValid = latitude?.toInt() in -90 until 90 && longitude?.toInt() in -180 until 180
        if (!coordIsValid) return

        val importedMarkerLatLng = LatLng(latitude!!, longitude!!)
        setState { copy(viewCommand = ViewCommand.ShowCreateImportedMarkerView(importedMarkerLatLng)) }
    }

    data class State(
        val viewCommand: ViewCommand? = null,
        val dataMimeType: String? = null,
        val dataUri: Uri? = null,
        val dataImportAsyncResult: Async<Unit> = Uninitialized
    ) :
        MvRxState

    companion object : MvRxViewModelFactory<DataImportViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State)
                : DataImportViewModel {
            val activity = (viewModelContext as ActivityViewModelContext).activity
            val processor = DataProcessor(CsvReader(), activity.get())
            return DataImportViewModel(
                state,
                processor,
                activity.get(),
                activity.get(),
                activity.get(),
                activity.get()
            )
        }

        sealed class ViewCommand {
            data class ShowCreateImportedMarkerView(val latLng: LatLng) : ViewCommand()
            object Exit : ViewCommand()
            object ShowImportConfirmationDialog : ViewCommand()
            object DismissImportConfirmationDialog : ViewCommand()

            object  finishedLoading: ViewCommand()
        }

        const val CSV_MIME_TYPE = "text/comma-separated-values"
        const val KML_MIME_TYPE = "application/vnd.google-earth.kml+xml"
        const val KMZ_MIME_TYPE = "application/vnd.google-earth.kmz"

        private data class MarkerDistinctSelector(
            val points: List<LatLng?>,
            val title: String
        ) {
            companion object {
                fun fromMarker(marker: Marker): MarkerDistinctSelector {
                    return with(marker) {
                        MarkerDistinctSelector(points, title)
                    }
                }
            }
        }

        private val supportedMimeTypes = arrayListOf(CSV_MIME_TYPE, KML_MIME_TYPE, KMZ_MIME_TYPE)
    }


    private fun addToFirebase(
        marker: Marker,
        markerId: Long,
        targetFolder: Folder,
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


        doc.set(targetFolder)

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
}
