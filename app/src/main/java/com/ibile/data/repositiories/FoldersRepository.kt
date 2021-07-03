package com.ibile.data.repositiories

import com.ibile.data.database.daos.FoldersDao
import com.ibile.data.database.daos.FoldersWithMarkersDao
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.FolderWithMarkers
import io.reactivex.Completable

class FoldersRepository(
    private val foldersDao: FoldersDao,
    private val foldersWithMarkersDao: FoldersWithMarkersDao
) {
    fun getAllFoldersWithMarkers() = foldersWithMarkersDao.getAllFoldersWithMarkers()

    fun getAllFoldersWithMarkersCount() = foldersWithMarkersDao.getAllFoldersWithMarkersCount()

    fun insertMarkersWithFolders(foldersWithMarkers: List<FolderWithMarkers>) =
        foldersWithMarkersDao.insertFoldersWithMarkers(foldersWithMarkers)

    fun getFolder(folderId: Long) = foldersDao.getFolder(folderId)

    fun addFolder(folder: Folder) = foldersDao.insertFolder(folder)

    fun updateFolders(vararg folder: Folder) = foldersDao.updateFolders(*folder)

    fun deleteFolders(vararg folders: Folder): Completable = foldersDao.deleteMarkers(*folders)

    fun dropFolderTable() = foldersDao.dropFoldersTable()

}
