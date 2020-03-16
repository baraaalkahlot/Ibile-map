package com.ibile.features.mainexternaloverlays

import android.os.Parcelable
import androidx.databinding.ObservableField
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.ibile.core.BaseViewModel
import kotlinx.android.parcel.Parcelize

data class UIStateViewModelState(val currentView: UIStateViewModel.CurrentView) : MvRxState

class UIStateViewModel(initialState: UIStateViewModelState) :
    BaseViewModel<UIStateViewModelState>(initialState) {

    val currentViewObservable = ObservableField<CurrentView>()

    init {
        selectSubscribe(UIStateViewModelState::currentView) {
            currentViewObservable.set(it)
        }
    }

    sealed class CurrentView : Parcelable {
        @Parcelize
        object BrowseMarkers : CurrentView()

        @Parcelize
        object LocationsSearch : CurrentView()

        @Parcelize
        object OrganizeMarkers : CurrentView()
    }

    companion object : MvRxViewModelFactory<UIStateViewModel, UIStateViewModelState> {
        override fun initialState(viewModelContext: ViewModelContext): UIStateViewModelState? {
            val args by (viewModelContext as FragmentViewModelContext)
                .fragment.navArgs<MainExternalOverlaysDialogFragmentArgs>()
            return UIStateViewModelState(args.view)
        }
    }
}
