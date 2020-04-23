package com.ibile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ibile.data.database.daos.FoldersDao
import com.ibile.data.database.daos.FoldersWithMarkersDao
import com.ibile.data.database.daos.MarkerDao
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker

@Database(entities = [Marker::class, Folder::class], version = 1)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun markerDao(): MarkerDao

    abstract fun foldersDao(): FoldersDao

    abstract fun foldersWithMarkersDao(): FoldersWithMarkersDao

    companion object {
        fun build(context: Context): com.ibile.data.database.Database {
            return Room.databaseBuilder(
                    context,
                    com.ibile.data.database.Database::class.java,
                    "ibile-markers"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // create the default folder that all markers get added to with the default values
                        db.execSQL("INSERT into folders VALUES (\"Default folder\", 1, 3000, -3394005, 1)")
                    }
                })
                .build()
        }
    }
}
