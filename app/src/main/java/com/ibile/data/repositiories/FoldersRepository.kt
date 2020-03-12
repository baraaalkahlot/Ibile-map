package com.ibile.data.repositiories

import com.ibile.data.database.daos.FoldersDao
import com.ibile.data.database.entities.Folder

class FoldersRepository(private val foldersDao: FoldersDao) {
    fun getAllFoldersWithMarkers() = foldersDao.getAllFoldersWithMarkers()

    fun getAllSelectedFoldersWithMarkers() = foldersDao.getAllSelectedFoldersWithMarkers()

    fun getAllFoldersWithMarkersCount() = foldersDao.getAllFoldersWithMarkersCount()

    fun addFolder(folder: Folder) = foldersDao.insertFolder(folder)

    fun updateFolders(vararg folder: Folder) = foldersDao.updateFolders(*folder)
}
