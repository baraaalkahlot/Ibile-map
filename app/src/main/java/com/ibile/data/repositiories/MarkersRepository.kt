package com.ibile.data.repositiories

import com.ibile.data.database.daos.MarkerDao
import com.ibile.data.database.entities.Marker

class MarkersRepository(private val markerDao: MarkerDao) {
    fun insertMarker(marker: Marker) = markerDao.insertMarker(marker)

    fun insertMarkers(vararg markers: Marker) = markerDao.insertMarkers(*markers)

    fun updateMarkers(vararg marker: Marker) = markerDao.updateMarkers(*marker)

    fun getAllMarkers() = markerDao.getAllMarkers()

    fun getMarker(id: Long) = markerDao.getMarker(id)

    fun deleteMarkers(vararg markers: Marker) = markerDao.deleteMarkers(*markers)

    fun getMarkersByFolderId(folderId: Long) = markerDao.getMarkersByFolderId(folderId)
}

