package com.ibile.features.main.editfolder

import android.content.Context
import android.util.Log
import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.ibile.USERS_COLLECTION
import com.ibile.USERS_MARKERS
import com.ibile.core.BaseViewModel
import com.ibile.data.SharedPref
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class EditFolderViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val markersRepository: MarkersRepository,
    private val sharedPref: SharedPref,
    private val context: Context
) :
    BaseViewModel<EditFolderViewModel.State>(initialState) {

    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) {
            folder.set(it)
        }
    }

    fun getFolder(folderId: Long) {
        foldersRepository.getFolder(folderId)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFolderAsync = it) }
    }

    fun updateFolder(folder: Folder) {
        foldersRepository
            .updateFolders(folder)
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }

        val db = FirebaseFirestore.getInstance()

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(folder.id.toString())
            .set(folder)
            .addOnSuccessListener {
                Log.d("wasd", "success")
            }
            .addOnFailureListener { e ->
                Log.w("wasd", "Error adding document", e)
            }
        Log.d("wasd", "updateFolder: ")

    }

    fun updateFolderWithMarkers(folder: Folder, updateMarkers: (List<Marker>) -> List<Marker>) {
        markersRepository
            .getMarkersByFolderId(folder.id)
            .flatMapCompletable { markersRepository.updateMarkers(*updateMarkers(it).toTypedArray()) }
            .concatWith(foldersRepository.updateFolders(folder))
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }

        val db = FirebaseFirestore.getInstance()

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        val folderDoc = db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(folder.id.toString())

        folderDoc.set(folder)

        folderDoc.collection(USERS_MARKERS)
            .get().addOnCompleteListener { values ->
                for (item: QueryDocumentSnapshot in values.result!!) {
                    folderDoc.collection(USERS_MARKERS).document(item.id)
                        .update(
                            "icon", folder.iconId,
                            "color", folder.color
                        )
                }
            }
    }

    fun deleteFolderWithMarkers(folder: Folder) {
        markersRepository
            .getMarkersByFolderId(folder.id)
            .flatMapCompletable { markersRepository.deleteMarkers(*it.toTypedArray()) }
            .concatWith(foldersRepository.deleteFolders(folder))
            .subscribeOn(Schedulers.io())
            .execute { copy(updateFolderAsync = it) }


        val db = FirebaseFirestore.getInstance()

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(folder.id.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("wasd", "success")
            }
            .addOnFailureListener { e ->
                Log.w("wasd", "Error adding document", e)
            }
    }

    data class State(
        val folderId: Long,
        val folder: Folder = Folder(""),
        val getFolderAsync: Async<Folder> = Uninitialized,
        val updateFolderAsync: Async<Unit> = Uninitialized
    ) :
        MvRxState

    companion object : MvRxViewModelFactory<EditFolderViewModel, State> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): EditFolderViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return EditFolderViewModel(
                state,
                fragment.get(),
                fragment.get(),
                fragment.get(),
                viewModelContext.activity
            )
        }

        override fun initialState(viewModelContext: ViewModelContext): State? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            val folderId = fragment.arguments?.getLong(EditFolderDialogFragment.ARG_FOLDER_ID)!!
            return State(folderId)
        }
    }
}
