package com.ibile.data.database.daos

import androidx.room.*
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface FoldersDao {
    @Transaction
    @Query("SELECT * FROM folders")
    fun getAllFoldersWithMarkers(): Flowable<List<FolderWithMarkers>>

    @Transaction
//    @Query("SELECT * FROM folders WHERE selected = 1 AND (SELECT COUNT(*) FROM markers WHERE folder_id = folders.id) > 0")
    @Query("SELECT * FROM folders")
    fun getAllSelectedFoldersWithMarkers(): Flowable<List<FolderWithMarkers>>

    @Insert
    fun insertFolder(folder: Folder): Single<Long>

    @Query("SELECT *, (SELECT COUNT(*) FROM markers WHERE markers.folder_id = folders.id) AS markers_count FROM folders")
    fun getAllFoldersWithMarkersCount(): Flowable<List<FolderWithMarkersCount>>

    @Update
    fun updateFolders(vararg folder: Folder): Completable

    @Query("SELECT * FROM folders where id = :id LIMIT 1")
    fun getFolder(id: Long): Flowable<Folder>

    @Delete
    fun deleteMarkers(vararg folders: Folder): Completable
}
