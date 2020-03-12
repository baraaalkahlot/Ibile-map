package com.ibile.data.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class FolderWithMarkers(
    @Embedded val folder: Folder,
    @Relation(
        parentColumn = "id",
        entityColumn = "folder_id"
    ) val markers: List<Marker>
)
