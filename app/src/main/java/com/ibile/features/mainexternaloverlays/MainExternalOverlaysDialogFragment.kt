package com.ibile.features.mainexternaloverlays

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.ibile.R
import com.ibile.core.BaseDialogFragment
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.databinding.DialogFragmentMainExternalOverlayBinding
import com.ibile.features.browsemarkers.BrowseMarkersPresenter
import com.ibile.features.browsemarkers.BrowseMarkersViewEvents
import com.ibile.features.browsemarkers.BrowseMarkersViewModel
import com.ibile.features.locationssearch.LocationsSearchPresenter
import com.ibile.features.locationssearch.LocationsSearchViewEvents
import com.ibile.features.locationssearch.LocationsSearchViewModel
import com.ibile.features.addmarkerfromlocationssearchresult.LocationSearchSelectedResultFragment
import com.ibile.utils.extensions.navController

interface ActionBarViewBindingData {
    fun onClickBackBtn()
    fun onBrowseMarkersSearchInputChange(value: String)
    fun onLocationsSearchInputChange(value: String)
    fun onClickBrowseMarkersBtn()
    fun onClickLocationsSearchBtn()

    val data: ObservableField<ActionBarViewData>
}


data class ActionBarViewData(val currentView: UIStateViewModel.CurrentView) {
    val currentViewIsBrowseMarkers: Boolean
        get() = currentView is UIStateViewModel.CurrentView.BrowseMarkers

    val currentViewIsSearchLocations: Boolean
        get() = currentView is UIStateViewModel.CurrentView.LocationsSearch
}

class MainExternalOverlaysDialogFragment : BaseDialogFragment(),
    ActionBarViewBindingData, BrowseMarkersViewEvents, LocationsSearchViewEvents,
    LocationSearchSelectedResultFragment.ParentViewCallback {

    private val uiStateViewModel: UIStateViewModel by fragmentViewModel()
    private val presenter by lazy {
        MainPresenter(uiStateViewModel, browseMarkersPresenter, locationsSearchPresenter)
    }

    private val browseMarkersViewModel: BrowseMarkersViewModel by fragmentViewModel()
    private val browseMarkersPresenter by lazy { BrowseMarkersPresenter(browseMarkersViewModel) }

    private val locationsSearchViewModel: LocationsSearchViewModel by fragmentViewModel()
    private val locationsSearchPresenter by lazy {
        LocationsSearchPresenter(
            locationsSearchViewModel,
            childFragmentManager
        )
    }

    override val data by lazy { presenter.actionBarViewBindingData }

    override val layoutId: Int
        get() = R.layout.dialog_fragment_main_external_overlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DialogFragmentMainExternalOverlayBinding.inflate(inflater)
            .apply {
                actionBar.eventHandlers = this@MainExternalOverlaysDialogFragment
            }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.init(this)
    }

    override fun epoxyController(): MvRxEpoxyController = simpleController {
        presenter
            .buildModels(
                this,
                this@MainExternalOverlaysDialogFragment,
                this@MainExternalOverlaysDialogFragment
            )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                presenter.onBackPressed(navController)
                super.onBackPressed()
            }
        }
    }

    override fun onClickBackBtn() {
        this.dialog?.onBackPressed()
    }

    override fun onBrowseMarkersSearchInputChange(value: String) {
        presenter.onBrowseMarkerSearchInputChange(value)
    }

    override fun onLocationsSearchInputChange(value: String) {
        presenter.onLocationsSearchInputChange(value)
    }

    override fun onClickMarkerItem(markerId: Long) {
        presenter.onClickMarkerItem(markerId, navController)
    }

    override fun onClickSearchResultItem(id: String) {
        presenter.onClickLocationsSearchResultItem(id)
    }

    override fun onAddMarkerSuccess(markerId: Long) {
        presenter.onLocationsSearchSelectedResultViewAddMarkerSuccess(markerId, navController)
    }

    override fun onBackBtnClicked() {
        presenter.onLocationsSearchSelectedResultViewBackPressed()
    }

    override fun onClickBrowseMarkersBtn() {
        presenter.onClickActionBarBrowseMarkersBtn()
    }

    override fun onClickLocationsSearchBtn() {
        presenter.onClickActionBarLocationsSearchBtn(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.dispose()
    }

    override val selectedResultId: String
        get() = presenter.locationsSearchSelectedSearchResultId

    override val sessionToken: AutocompleteSessionToken
        get() = presenter.locationsSearchSessionToken
}
