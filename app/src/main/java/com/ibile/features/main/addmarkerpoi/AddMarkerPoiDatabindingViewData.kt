package com.ibile.features.main.addmarkerpoi

interface AddMarkerPoiDatabindingViewData {
    fun handleOkBtnClick()
    fun handleCancelBtnClick()
    fun handleTargetFolderBtnClick()

    val data: AddMarkerPoiViewModel
}