package com.ibile.features.main.addfolder

import android.content.Context
import android.util.Log
import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ibile.DEFAULT_DB_NAME
import com.ibile.USERS_COLLECTION
import com.ibile.core.BaseViewModel
import com.ibile.data.SharedPref
import com.ibile.data.database.entities.Folder
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MapFile
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get
import java.util.*

class AddFolderViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val sharedPref: SharedPref,
    private val context: Context
) :
    BaseViewModel<AddFolderViewModel.State>(initialState) {

    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) { folder.set(it) }
    }


    fun addFolder(folder: Folder, mapFile: MapFile?) {

        val id = foldersRepository.addFolder(folder)
            .subscribeOn(Schedulers.io())
            .blockingGet()
        state.addFolderAsync

        val db = FirebaseFirestore.getInstance()

        val userEmail = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        val createdFolder = Folder(
            title = folder.title,
            id = id,
            iconId = folder.iconId,
            color = folder.color,
            selected = folder.selected
        )
        db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection(sharedPref.currentMapFileId.toString())
            .document(id.toString())
            .set(createdFolder)
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
                        val list: ArrayList<String> = if (document.get("id") != null) {
                            document.get("id") as ArrayList<String>
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

    fun addFolderToRoomOnly(folder: Folder) {
        val id = foldersRepository.addFolder(folder)
            .subscribeOn(Schedulers.io())
            .blockingGet()
        state.addFolderAsync
    }

    data class State(
        val folder: Folder = Folder(""),
        val addFolderAsync: Async<Long> = Uninitialized
    ) : MvRxState

    companion object : MvRxViewModelFactory<AddFolderViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): AddFolderViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AddFolderViewModel(
                state,
                fragment.get(),
                fragment.get(),
                viewModelContext.activity
            )
        }
    }
}
