package com.ibile.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @ColumnInfo(name = "title") val title: String = "",
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "icon_id") val iconId: Int = Marker.DEFAULT_MARKER_ICON_ID,
    @ColumnInfo(name = "color") val color: Int = Marker.DEFAULT_COLOR,
    @ColumnInfo(name = "selected") val selected: Boolean = true
)
