package com.ibile.features.main.markerslist

interface MarkerInfoDatabindingViewData {
    fun handleEditBtnClick()
    fun handleCopyBtnClick()
    fun handleNavigationBtnClick()
    fun handleCallBtnClick()
    fun handleImageClick(index: Int)

    val data: MarkersViewModel
}