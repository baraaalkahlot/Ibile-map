package com.ibile.features.main

import android.content.Context
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.ibile.R
import com.ibile.data.database.entities.Marker
import com.ibile.features.MarkerImagesPreviewFragment
import com.ibile.features.MarkerPhoneNumberActionsDialogDirections
import com.ibile.features.editmarker.EditMarkerDialogFragment

class MarkerInfoPresenter(private val fragmentManager: FragmentManager) {
    val data: ObservableField<Marker> = ObservableField()

    var markerId: Long? = null

    var marker: Marker?
        get() = data.get()
        set(value) {
            data.set(value)
        }

    private val markerImagesPreviewFragment: MarkerImagesPreviewFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
                as? MarkerImagesPreviewFragment ?: MarkerImagesPreviewFragment()

    private val editMarkerDialogFragment: EditMarkerDialogFragment
        get() = fragmentManager.findFragmentByTag(FRAGMENT_TAG_EDIT_MARKER)
                as? EditMarkerDialogFragment ?: EditMarkerDialogFragment()

    var clickedMarkerImageIndex: Int = 0
    val mode: MarkerImagesPreviewFragment.Callback.Mode = object
        : MarkerImagesPreviewFragment.Callback.Mode.View() {
        override val marker: Marker
            get() = this@MarkerInfoPresenter.marker!!

        override val initialImageItemIndex: Int
            get() = clickedMarkerImageIndex
    }

    fun handleEditBtnClick() {
        markerId = marker!!.id
        editMarkerDialogFragment.show(fragmentManager, FRAGMENT_TAG_EDIT_MARKER)
        marker = null
    }

    fun handleCopyBtnClick() {

    }

    fun handleNavigationBtnClick() {

    }

    fun handleCallBtnClick(navController: NavController, context: Context) {
        val phoneNumber = marker?.phoneNumber
        if (phoneNumber.isNullOrBlank()) {
            val text = context.getString(R.string.text_empty_marker_number)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        } else {
            val direction = MarkerPhoneNumberActionsDialogDirections
                .actionGlobalMarkerPhoneNumberActionsDialog(phoneNumber)
            navController.navigate(direction)
        }
    }

    fun handleImageClick(index: Int) {
        clickedMarkerImageIndex = index
        markerImagesPreviewFragment.show(fragmentManager, FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
    }

    fun onMapClick() {
        marker = null
    }

    fun onEditMarkerComplete() {
        editMarkerDialogFragment.dismiss()
        markerId = null
    }

    fun onMarkerPointsUpdateInit(marker: Marker) {
        markerId = marker.id
        if (editMarkerDialogFragment.dialog?.isShowing == true)
            editMarkerDialogFragment.dismiss()
    }

    fun onCancelAddOrEditMarkerPoints() {
        markerId = null
    }

    companion object {
        const val FRAGMENT_TAG_MARKER_IMAGES_PREVIEW = "FRAGMENT_TAG_MARKER_IMAGES_PREVIEW"
        const val FRAGMENT_TAG_EDIT_MARKER = "FRAGMENT_TAG_EDIT_MARKER"
    }
}
