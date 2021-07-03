package com.ibile.data.database.daos

import androidx.room.*
import com.ibile.data.database.entities.Marker
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface MarkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMarker(marker: Marker): Single<Long>

    // @Query("SELECT * FROM markers")
    // fun getAllMarkers(): Flowable<List<Marker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMarkers(vararg markers: Marker): Single<List<Long>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateMarkers(vararg marker: Marker): Completable

    @Query("SELECT * FROM markers WHERE folder_id in (SELECT id FROM folders WHERE selected = 1)")
    fun getAllMarkers(): Flowable<List<Marker>>

    @Query("SELECT * FROM markers where folder_id = :folderId")
    fun getMarkersByFolderId(folderId: Long): Single<List<Marker>>

    @Query("SELECT * FROM markers where id = :id LIMIT 1")
    fun getMarker(id: Long): Single<Marker>

    @Delete
    fun deleteMarkers(vararg marker: Marker): Completable

    @Query("DELETE FROM markers")
    fun dropMarkerTable()
}
