package com.ibile.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ibile.data.database.entities.Marker
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface MarkerDao {
    @Insert
    fun insertMarker(marker: Marker): Single<Long>

    @Query("SELECT * FROM markers")
    fun getAllMarkers(): Flowable<List<Marker>>

    @Update
    fun updateMarker(marker: Marker): Completable
}
