package com.ibile.features.main.addfolder

import android.content.Context
import android.util.Log
import androidx.databinding.ObservableField
import com.airbnb.mvrx.*
import com.google.firebase.firestore.FirebaseFirestore
import com.ibile.USERS_COLLECTION
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Folder
import com.ibile.data.repositiories.FoldersRepository
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AddFolderViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val context: Context
) :
    BaseViewModel<AddFolderViewModel.State>(initialState) {

    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) { folder.set(it) }
    }

    fun addFolder(folder: Folder) {

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
            .collection("file")
            .document(id.toString())
            .set(createdFolder)
            .addOnSuccessListener {
                Log.d("wasd", "success")
            }
            .addOnFailureListener { e ->
                Log.w("wasd", "Error adding document", e)
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
            return AddFolderViewModel(state, fragment.get(), viewModelContext.activity)
        }
    }
}
