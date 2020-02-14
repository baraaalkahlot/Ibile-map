package com.ibile.data.database.entities

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.room.*
import com.google.android.libraries.maps.CameraUpdate
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.ibile.R
import com.ibile.core.getCurrentDateTime
import com.ibile.core.getIconDrawable
import com.ibile.core.setColor
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import org.koin.core.KoinComponent
import org.koin.core.get
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "markers", indices = [Index("name", unique = true)])
@TypeConverters(
    Marker.PointsTypeConverter::class,
    Marker.TypeTypeConverter::class,
    Marker.IconTypeConverter::class
)
data class Marker(
    val points: List<LatLng?>,
    val type: Type,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") var name: String? = null,
    @ColumnInfo(name = "created_at")
    var createdAt: Date = getCurrentDateTime(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Date = getCurrentDateTime(),
    var description: String? = null,
    var color: Int = DEFAULT_COLOR,
    var icon: Icon? = null
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

    val formattedCreationDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(createdAt)

    val formattedCreationTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(createdAt)

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

    object IconTypeConverter : KoinComponent {
        @TypeConverter
        @JvmStatic
        fun markerIconIdToIcon(iconId: Int?): Icon? {
            val iconPack = get<IconPack>()
            val icon = iconId?.let { iconPack.getIcon(it) }
            iconPack.getIconDrawable(icon)
            return icon
        }

        @TypeConverter
        @JvmStatic
        fun markerIconToIconId(icon: Icon?): Int? = icon?.id
    }

    companion object : KoinComponent {
        private val iconPack = get<IconPack>()

        fun createMarker(position: LatLng) = Marker(
            arrayListOf(position), Type.MARKER, icon = iconPack.getIcon(DEFAULT_MARKER_ICON_ID)
        )

        fun createPolyline(points: List<LatLng?>) = Marker(points, Type.POLYLINE)
        fun createPolygon(points: List<LatLng?>) = Marker(points, Type.POLYGON)

        @BindingAdapter("markerIcon")
        @JvmStatic
        fun markerIcon(view: ImageView, marker: Marker) {
            val resources = view.context.resources
            val drawable = when (marker.type) {
                Type.MARKER -> iconPack.getIconDrawable(marker.icon)?.mutate()?.setColor(Color.WHITE)
                Type.POLYLINE -> resources.getDrawable(R.drawable.ic_polyline, null)
                Type.POLYGON -> resources.getDrawable(R.drawable.ic_polygon, null)
            }
            view.setImageDrawable(drawable)
        }

        val DEFAULT_COLOR = Color.rgb(204, 54, 43)
        const val DEFAULT_MARKER_ICON_ID = 3000
    }
}
