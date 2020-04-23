package com.ibile.data.database.daos

import androidx.room.*
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.folderlist.FolderWithMarkersCount
import io.reactivex.Completable
import io.reactivex.Flowable

@Dao
abstract class FoldersWithMarkersDao {
    @Transaction
    open fun _insertFoldersWithMarkersSync(foldersWithMarkers: List<FolderWithMarkers>) {
        val markers = foldersWithMarkers.flatMap { it.markers }
        val folders = foldersWithMarkers.map { it.folder }
        _insertFolders(*folders.toTypedArray())
        _insertMarkers(*markers.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun _insertFolders(vararg folder: Folder): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun _insertMarkers(vararg markers: Marker): List<Long>

    fun insertFoldersWithMarkers(foldersWithMarkers: List<FolderWithMarkers>) = Completable.create {
        _insertFoldersWithMarkersSync(foldersWithMarkers)
        it.onComplete()
    }

    // @Query("SELECT * FROM folders WHERE selected = 1 AND (SELECT COUNT(*) FROM markers WHERE folder_id = folders.id) > 0")
    @Transaction
    @Query("SELECT * FROM folders")
    abstract fun getAllFoldersWithMarkers(): Flowable<List<FolderWithMarkers>>

    @Query("SELECT *, (SELECT COUNT(*) FROM markers WHERE markers.folder_id = folders.id) AS markers_count FROM folders")
    abstract fun getAllFoldersWithMarkersCount(): Flowable<List<FolderWithMarkersCount>>
}
