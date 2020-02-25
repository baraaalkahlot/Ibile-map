package com.ibile.features.mainexternaloverlays

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.ObservableField
import androidx.navigation.fragment.navArgs
import com.airbnb.mvrx.*

data class UIStateViewModelState(val currentView: UIStateViewModel.CurrentView) : MvRxState

class UIStateViewModel(initialState: UIStateViewModelState) :
    BaseMvRxViewModel<UIStateViewModelState>(initialState) {
    val state
        get() = withState(this) { it }

    private val currentViewObservable = ObservableField<CurrentView>()

    init {
        selectSubscribe(UIStateViewModelState::currentView) {
            currentViewObservable.set(it)
        }
    }

    fun updateCurrentView(view: CurrentView) {
        setState { copy(currentView = view) }
    }

    sealed class CurrentView : Parcelable {
        class BrowseMarkers() : CurrentView() {
            constructor(parcel: Parcel) : this() {

            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {

            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<BrowseMarkers> {
                override fun createFromParcel(parcel: Parcel): BrowseMarkers {
                    return BrowseMarkers(parcel)
                }

                override fun newArray(size: Int): Array<BrowseMarkers?> {
                    return arrayOfNulls(size)
                }
            }
        }

        class LocationsSearch() : CurrentView() {
            constructor(parcel: Parcel) : this() {
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {

            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<LocationsSearch> {
                override fun createFromParcel(parcel: Parcel): LocationsSearch {
                    return LocationsSearch(parcel)
                }

                override fun newArray(size: Int): Array<LocationsSearch?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object : MvRxViewModelFactory<UIStateViewModel, UIStateViewModelState> {
        override fun initialState(viewModelContext: ViewModelContext): UIStateViewModelState? {
            val args by (viewModelContext as FragmentViewModelContext)
                .fragment.navArgs<MainExternalOverlaysDialogFragmentArgs>()
            return UIStateViewModelState(args.view)
        }
    }
}