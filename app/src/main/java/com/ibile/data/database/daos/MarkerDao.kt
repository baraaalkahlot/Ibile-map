package com.ibile.data.database.daos

import androidx.room.*
import com.ibile.data.database.entities.Marker
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface MarkerDao {
    @Insert
    fun insertMarker(marker: Marker): Single<Long>

    @Query("SELECT * FROM markers")
    fun getAllMarkers(): Flowable<List<Marker>>

    @Update
    fun updateMarker(marker: Marker): Completable

    @Query("SELECT * FROM markers where id = :id LIMIT 1")
    fun getMarker(id: Long): Single<Marker>

    @Delete
    fun deleteMarker(marker: Marker): Completable
}
