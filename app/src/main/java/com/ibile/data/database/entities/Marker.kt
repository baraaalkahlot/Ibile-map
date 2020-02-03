package com.ibile.data.database.entities

import androidx.room.*
import com.google.android.libraries.maps.CameraUpdate
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.ibile.core.getCurrentDateTime
import java.text.DateFormat
import java.util.*

@Entity(tableName = "markers", indices = [Index("name", unique = true)])
@TypeConverters(Marker.PointsTypeConverter::class, Marker.TypeTypeConverter::class)
data class Marker(
    val points: List<LatLng?>,
    val type: Type,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") var name: String? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Date = getCurrentDateTime(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Date = getCurrentDateTime()
) {
    val title get() = name ?: "Marker $id"

    val formattedCreatedAt: String
        get() = DateFormat
            .getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM)
            .format(createdAt)

    val isMarker
        get() = type == Type.MARKER

    val isPolyline
        get() = type == Type.POLYLINE

    val isPolygon
        get() = type == Type.POLYGON

    val position: LatLng?
        get() = points[0]

    val cameraUpdate: CameraUpdate
        get() = if (isMarker) CameraUpdateFactory.newLatLng(position) else {
            val boundsBuilder = LatLngBounds.builder()
            points.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            CameraUpdateFactory.newLatLngBounds(bounds, 100)
        }

    enum class Type { MARKER, POLYLINE, POLYGON }

    object PointsTypeConverter {
        @TypeConverter
        @JvmStatic
        fun stringToPoints(encoded: String): List<LatLng?> = PolyUtil.decode(encoded)

        @TypeConverter
        @JvmStatic
        fun pointsToString(points: List<LatLng?>): String = PolyUtil.encode(points)
    }

    object TypeTypeConverter {
        @TypeConverter
        @JvmStatic
        fun stringToType(markerType: String): Type = Type.valueOf(markerType)

        @TypeConverter
        @JvmStatic
        fun typeToString(markerType: Type): String = markerType.name
    }

    companion object {
        fun point(position: LatLng) = Marker(arrayListOf(position), Type.MARKER)
        fun polyline(points: List<LatLng?>) = Marker(points, Type.POLYLINE)
        fun polygon(points: List<LatLng?>) = Marker(points, Type.POLYGON)
    }
}
