package com.ibile.features.main.utils

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.request.RequestOptions
import com.ibile.MarkerImageItemBindingModel_
import com.ibile.R
import com.ibile.core.animateSlideVertical
import com.ibile.data.database.entities.Marker
import com.ibile.databinding.PartialActiveMarkerInfoBinding

private fun showActiveMarkerInfo(view: View, addMarkerBtn: ImageButton) {
    with(view) {
        val addBtnToInfoMargin = 16
        val slideDistance =
            addMarkerBtn.marginBottom - ((height.toFloat()) + addBtnToInfoMargin)

        this.animateSlideVertical(-height.toFloat(), 150)
        addMarkerBtn.animateSlideVertical(slideDistance, 150)
    }
}

private fun hideActiveMarkerInfoView(view: View, addMarkerBtn: ImageButton) {
    with(view) {
        val markerBtnToInfoMargin = 16
        val slideDistance =
            height.toFloat() + markerBtnToInfoMargin - addMarkerBtn.marginBottom

        this.animateSlideVertical(height.toFloat(), 150)
        addMarkerBtn.animateSlideVertical(slideDistance, 150)
    }
}

@BindingAdapter("app:marker")
fun setMarker(markerInfoView: ConstraintLayout, oldMarker: Marker?, marker: Marker?) {
    if (oldMarker == null && marker == null) return

    val markerInfoViewBinding: PartialActiveMarkerInfoBinding = DataBindingUtil.getBinding(markerInfoView)!!
    val models = marker?.imageUris?.mapIndexed { i, uri: Uri ->
        MarkerImageItemBindingModel_().apply {
            id(uri.toString())
            uri(uri)
            dimension(60f)
            requestOptions(RequestOptions().centerCrop())
            onClick { _ -> markerInfoViewBinding.handler!!.handleImageClick(i) }
        }
    } ?: arrayListOf()
    markerInfoViewBinding.carouselMarkerImages.setModels(models)

    val addMarkerBtn = (markerInfoView.parent as ViewGroup).findViewById<ImageButton>(R.id.btn_add_marker)

    if (marker == null) {
        hideActiveMarkerInfoView(markerInfoView, addMarkerBtn)
    } else {
        showActiveMarkerInfo(markerInfoView, addMarkerBtn)
    }
}
