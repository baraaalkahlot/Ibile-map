package com.ibile.features.main

import com.ibile.features.main.UIStateViewModel

interface MainDataBindingViewData {
    fun handleMyLocationBtnClick()
    fun handleAddMarkerBtnClick()
    fun handleAddPolylineBtnClick()
    fun handleAddPolygonBtnClick()

    val data: UIStateViewModel
}