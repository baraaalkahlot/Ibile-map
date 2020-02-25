package com.ibile.features.addpolylinepoi

import androidx.databinding.ObservableField

interface AddPolyLinePoiDatabindingViewData {
    fun onClickAddBtn()
    fun onClickPrevBtn()
    fun onClickNextBtn()
    fun onClickRemoveBtn()
    fun onClickSaveBtn()

    val data: ObservableField<Data>

    data class Data(
        val instructionsIsVisible: Boolean = true,
        val distanceIsVisible: Boolean = false,
        val previousBtnIsEnabled: Boolean = false,
        val saveBtnIsEnabled: Boolean = false,
        val polylinePathDistance: String = "",
        val newPointTargetIsVisible: Boolean = true,
        val removeBtnIsEnabled: Boolean = false,
        val nextBtnIsEnabled: Boolean = false,
        val currentPointCoords: CurrentPointCoordinates = CurrentPointCoordinates(0f, 0f)
    ) {
        data class CurrentPointCoordinates(val latitude: Float?, val longitude: Float?)
    }
}