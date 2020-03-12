package com.ibile.features.main.folderlist

import androidx.room.ColumnInfo
import com.ibile.data.database.entities.Folder

data class FolderWithMarkersCount(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "icon_id") val iconId: Int,
    @ColumnInfo(name = "color") val color: Int,
    @ColumnInfo(name = "selected") val selected: Boolean,
    @ColumnInfo(name = "markers_count") val totalMarkers: Int
) {
    companion object {
        fun FolderWithMarkersCount.toFolder(): Folder {
            val (title, id, iconId, color, selected) = this
            return Folder(title, id, iconId, color, selected)
        }
    }
}
