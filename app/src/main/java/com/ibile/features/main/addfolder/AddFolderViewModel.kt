package com.ibile.features.main.addfolder

import androidx.databinding.ObservableField
import com.airbnb.mvrx.MvRxState
import com.ibile.core.BaseViewModel
import com.ibile.data.database.entities.Folder

class AddFolderViewModel(initialState: State) :
    BaseViewModel<AddFolderViewModel.State>(initialState) {
    val folder by lazy { ObservableField(state.folder) }

    init {
        selectSubscribe(State::folder) {
            folder.set(it)
        }
    }

    data class State(val folder: Folder = Folder("")) : MvRxState
}
