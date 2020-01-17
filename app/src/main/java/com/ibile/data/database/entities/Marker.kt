package com.ibile.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ibile.core.getCurrentDateTime
import java.text.DateFormat
import java.util.*

@Entity(tableName = "markers", indices = [Index("name", unique = true)])
data class Marker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") var name: String? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Date = getCurrentDateTime(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Date = getCurrentDateTime(),
    var latitude: Double,
    var longitude: Double
) {
    val title get() = name ?: "Marker $id"

    val formattedCreatedAt: String
        get() = DateFormat
            .getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM)
            .format(createdAt)
}
