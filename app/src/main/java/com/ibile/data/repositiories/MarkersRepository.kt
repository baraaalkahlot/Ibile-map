package com.ibile.data.repositiories

import com.ibile.data.database.daos.MarkerDao
import com.ibile.data.database.entities.Marker

class MarkersRepository(private val markerDao: MarkerDao) {
    fun insertMarker(marker: Marker) = markerDao.insertMarker(marker)

    fun updateMarker(marker: Marker) = markerDao.updateMarker(marker)

    fun getAllMarkers() = markerDao.getAllMarkers()
}
