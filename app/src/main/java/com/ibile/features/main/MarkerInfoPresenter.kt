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

class MarkerInfoPresenter(private val fragmentManager: FragmentManager) {
    val data: ObservableField<Marker> = ObservableField()

    var marker: Marker?
        get() = data.get()
        set(value) {
            data.set(value)
        }

    private val markerImagesPreviewFragment: MarkerImagesPreviewFragment
        get() = fragmentManager.findFragmentByTag(MarkerImagesPreviewFragment.FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
                as? MarkerImagesPreviewFragment ?: MarkerImagesPreviewFragment()

    var clickedMarkerImageIndex: Int = 0
    val mode: MarkerImagesPreviewFragment.Callback.Mode = object
        : MarkerImagesPreviewFragment.Callback.Mode.View() {
        override val marker: Marker
            get() = this@MarkerInfoPresenter.marker!!

        override val initialImageItemIndex: Int
            get() = clickedMarkerImageIndex
    }

    fun handleEditBtnClick(navController: NavController) {
        val action = MainFragmentDirections.actionMainFragmentToEditMarkerDialogFragment(marker!!.id)
        navController.navigate(action)
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
        markerImagesPreviewFragment
            .show(fragmentManager, MarkerImagesPreviewFragment.FRAGMENT_TAG_MARKER_IMAGES_PREVIEW)
    }

    fun onMapClick() {
        marker = null
    }

    fun onExternalOverlay() {
        marker = null
    }
}
