package com.ibile.features.organizemarkers

import android.util.Log
import android.util.Xml
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.*
import com.ibile.core.BaseViewModel
import com.ibile.core.context
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.database.entities.Marker
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.datasharing.DataSerializer
import com.ibile.features.main.datasharing.DataSharingHandler
import com.ibile.features.main.datasharing.DataSharingViewModel
import com.ibile.features.main.datasharing.Exporter
import com.ibile.features.organizemarkers.OrganizeMarkersPresenter.SelectedMarkersAction
import de.siegmar.fastcsv.writer.CsvWriter
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.core.KoinComponent

class OrganizeMarkersViewModel(
    initialState: State,
    private val foldersRepository: FoldersRepository,
    private val markersRepository: MarkersRepository,
    private val fragment: Fragment
) :
    BaseViewModel<OrganizeMarkersViewModel.State>(initialState) {

    fun getFoldersWithMarkers() {
        foldersRepository
            .getAllFoldersWithMarkers()
            .toObservable()
            .subscribeOn(Schedulers.io())
            .execute { copy(getFoldersWithMarkersAsyncResult = it) }
    }

    fun updateMarkers(markers: List<Marker>) {
        markersRepository
            .updateMarkers(*markers.toTypedArray())
            .subscribeOn(Schedulers.io())
            .execute { copy(updateMarkersAsyncResult = it) }
    }

    fun insertMarkers(markers: List<Marker>) {
        markersRepository
            .insertMarkers(*markers.toTypedArray())
            .subscribeOn(Schedulers.io())
            .map { Unit }
            .execute { copy(updateMarkersAsyncResult = it) }
    }

    fun shareMarkers(markers: List<Marker>){



    }



    fun deleteMarkers(markers: List<Marker>) {
        markersRepository
            .deleteMarkers(*markers.toTypedArray())
            .subscribeOn(Schedulers.io())
            .execute { copy(updateMarkersAsyncResult = it) }
    }

    data class State(
        val getFoldersWithMarkersAsyncResult: Async<List<FolderWithMarkers>> = Uninitialized,
        val searchQuery: String = "",
        val selectedMarkersIds: List<Long> = listOf(),
        val selectedMarkersAction: SelectedMarkersAction? = null,
        val markersActionTargetFolder: Folder? = null,
        val updateMarkersAsyncResult: Async<Unit> = Uninitialized
    ) : MvRxState

    companion object : MvRxViewModelFactory<OrganizeMarkersViewModel, State>, KoinComponent {
        override fun create(
            viewModelContext: ViewModelContext,
            state: State
        ): OrganizeMarkersViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment


            return OrganizeMarkersViewModel(state, fragment.get(), fragment.get(), fragment)
        }
    }
}
