package com.ibile.features.main.addmarkerpoi

import com.ibile.features.main.addmarkerpoi.AddMarkerPoiViewModel

interface AddMarkerPoiDatabindingViewData {
    fun handleOkBtnClick()
    fun handleCancelBtnClick()

    val data: AddMarkerPoiViewModel
}