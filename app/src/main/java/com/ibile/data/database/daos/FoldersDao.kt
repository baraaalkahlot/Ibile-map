package com.ibile.data.database.daos

import androidx.room.*
import com.ibile.data.database.entities.Folder
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface FoldersDao {
    @Query("SELECT * FROM folders where id = :id LIMIT 1")
    fun getFolder(id: Long): Flowable<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFolder(folder: Folder): Single<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateFolders(vararg folder: Folder): Completable

    @Delete
    fun deleteMarkers(vararg folders: Folder): Completable

    @Query("DELETE FROM folders where id != 1")
    fun dropFoldersTable()
}
