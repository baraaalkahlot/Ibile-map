package com.ibile.features.main.markerslist

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.withState
import com.google.android.libraries.maps.GoogleMap
import com.ibile.R
import com.ibile.data.database.entities.Marker
import com.ibile.features.MarkerImagesPreviewFragment
import com.ibile.features.MarkerPhoneNumberActionsDialogDirections
import com.ibile.features.editmarker.EditMarkerDialogFragment
import com.ibile.features.main.MainFragment
import com.ibile.utils.extensions.copyTextToClipboard

class MarkersPresenter(
    private val markersViewModel: MarkersViewModel, private val fragmentManager: FragmentManager
) {
    private val activeMarker: Marker?
        get() {
            val (activeMarkerId, markersListAsync) = markersViewModel.state
            return markersListAsync()?.find { element -> element.id == activeMarkerId }
        }

    private val markers: List<Marker>?
        get() = markersViewModel.state.markersListAsync()

    private val markerImagesPreviewFragment: MarkerImagesPreviewFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
                as? MarkerImagesPreviewFragment ?: MarkerImagesPreviewFragment()

    private val editMarkerDialogFragment: EditMarkerDialogFragment
        get() {
            val activeMarkerId = markersViewModel.state.activeMarkerId
            return fragmentManager.findFragmentByTag(FRAGMENT_TAG_EDIT_MARKER) as? EditMarkerDialogFragment
                ?: EditMarkerDialogFragment.newInstance(activeMarkerId!!)
        }

    var clickedMarkerImageIndex: Int = 0
    val mode: MarkerImagesPreviewFragment.Callback.Mode = object
        : MarkerImagesPreviewFragment.Callback.Mode.View() {
        override val marker: Marker
            get() = this@MarkersPresenter.activeMarker!!

        override val initialImageItemIndex: Int
            get() = clickedMarkerImageIndex
    }

    fun init() {
        if (markersViewModel.state.markersListAsync !is Success) markersViewModel.getMarkers()
    }

    fun buildModels(
        map: GoogleMap,
        controller: EpoxyController,
        onMarkerAdded: (marker: Marker) -> Unit
    ) {
        withState(markersViewModel) { state ->
            state.markersListAsync()?.map { marker ->
                controller.markerView {
                    id(marker.id)
                    marker(marker)
                    map(map)
                    isActive(state.activeMarkerId == marker.id)
                    isVisible(marker.id != state.editMarkerId)
                    onMarkerAdded { onMarkerAdded(it) }
                    this.onUnbind { _, view -> view.removeMarker() }
                }
            }
        }
    }

    fun onClickMarker(markerId: Long) {
        markersViewModel.updateState { copy(activeMarkerId = markerId) }
    }

    fun onMapClick() {
        markersViewModel.updateState { copy(activeMarkerId = null) }
    }

    fun onMarkerCreatedOrUpdated(markerId: Long) {
        markersViewModel.updateState { copy(activeMarkerId = markerId, editMarkerId = null) }
    }

    fun onEditMarkerComplete(result: Long?) {
        markersViewModel.updateState { copy(activeMarkerId = result, editMarkerId = null) }
        editMarkerDialogFragment.dismiss()
    }

    fun onExternalOverlayResult(result: MainFragment.Companion.ExternalOverlaysResult) {
        when (result) {
            is MainFragment.Companion.ExternalOverlaysResult.BrowseMarkers -> {
                markersViewModel.updateState { copy(activeMarkerId = result.selectedMarkerId) }
            }
            is MainFragment.Companion.ExternalOverlaysResult.LocationsSearch -> {
                markersViewModel.updateState { copy(activeMarkerId = result.createdMarkerId) }
            }
            is MainFragment.Companion.ExternalOverlaysResult.OrganizeMarkers -> {
                // no action required for organize markers result
            }
        }
    }

    fun onMarkerPointsUpdateInit(): Marker {
        editMarkerDialogFragment.dismiss()
        val marker = markers?.find { it.id == markersViewModel.state.editMarkerId }!!
        return marker.copy()
    }

    fun onMarkerPointsUpdateInit(markerId: Long): Marker {
        val marker = markers?.find { it.id == markerId }!!
        markersViewModel.updateState { copy(activeMarkerId = null, editMarkerId = markerId) }
        return marker.copy()
    }

    fun onClickAddMarker() {
        markersViewModel.updateState { copy(activeMarkerId = null) }
    }

    fun onCancelAddOrEditMarkerPoints() {
        markersViewModel.updateState {
            copy(editMarkerId = null, activeMarkerId = markersViewModel.state.editMarkerId)
        }
    }

    fun onClickEditMarkerBtn() {
        markersViewModel.updateState { copy(activeMarkerId = null, editMarkerId = activeMarkerId) }
        editMarkerDialogFragment.show(fragmentManager, FRAGMENT_TAG_EDIT_MARKER)
    }

    fun onClickMarkerInfoCopyBtn(context: Context) {
        context.copyTextToClipboard(activeMarker!!.details)
        val text = context.getString(R.string.text_toast_marker_details_copied)
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun onClickMarkerInfoNavigationBtn() {
        MarkerNavigationOptionsDialogFragment.newInstance(activeMarker!!.position!!)
            .show(fragmentManager, FRAGMENT_TAG_MARKER_NAV_OPTIONS)
    }

    fun onClickMarkerInfoCallBtn(navController: NavController, context: Context) {
        val phoneNumber = activeMarker!!.phoneNumber
        if (phoneNumber.isNullOrBlank()) {
            val text = context.getString(R.string.text_empty_marker_number)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } else {
            val direction = MarkerPhoneNumberActionsDialogDirections
                .actionGlobalMarkerPhoneNumberActionsDialog(phoneNumber)
            navController.navigate(direction)
        }
    }

    fun onClickMarkerInfoImage(index: Int) {
        clickedMarkerImageIndex = index
        markerImagesPreviewFragment.show(fragmentManager, FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
    }

    fun onClickActionBarShareBtn(): Long? {
        return markersViewModel.state.activeMarkerId
    }

    companion object {
        const val FRAGMENT_TAG_MARKER_IMAGES_PREVIEW = "FRAGMENT_TAG_MARKER_IMAGES_PREVIEW"
        const val FRAGMENT_TAG_EDIT_MARKER = "FRAGMENT_TAG_EDIT_MARKER"
        const val FRAGMENT_TAG_MARKER_NAV_OPTIONS = "FRAGMENT_TAG_MARKER_NAV_OPTIONS"
    }
}
