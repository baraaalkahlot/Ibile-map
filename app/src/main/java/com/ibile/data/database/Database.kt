package com.ibile.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ibile.data.database.daos.MarkerDao
import com.ibile.data.database.entities.Marker

@Database(entities = [Marker::class], version = 1)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
}