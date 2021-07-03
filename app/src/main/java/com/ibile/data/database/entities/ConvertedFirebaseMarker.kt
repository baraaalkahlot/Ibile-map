package com.ibile.data.database.entities

import android.graphics.Color
import com.google.firebase.firestore.GeoPoint
import com.ibile.core.getCurrentDateTime
import java.util.*

const val DEFAULT_FOLDER_ID = 1L
val DEFAULT_COLOR = Color.rgb(204, 54, 43)

data class ConvertedFirebaseMarker(
    var id: Long? = null,
    val points: List<GeoPoint> = arrayListOf(),
    val type: Marker.Type = Marker.Type.MARKER,
    var createdAt: Date = getCurrentDateTime(),
    var updatedAt: Date = getCurrentDateTime(),
    var description: String? = null,
    var color: Int = DEFAULT_COLOR,
    var icon: Int? = null,
    var phoneNumber: String? = null,
    var imageUris: List<String> = arrayListOf(),
    val folderId: Long = DEFAULT_FOLDER_ID
)