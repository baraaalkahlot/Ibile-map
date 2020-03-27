package com.ibile.data.database.entities

import android.graphics.*
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.widget.ImageView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.BindingAdapter
import androidx.room.*
import com.google.android.libraries.maps.CameraUpdate
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.ibile.R
import com.ibile.core.Extensions.dp
import com.ibile.core.context
import com.ibile.core.getCurrentDateTime
import com.ibile.core.setColor
import com.maltaisn.icondialog.pack.IconPack
import org.koin.core.KoinComponent
import org.koin.core.get
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "markers")
@TypeConverters(
    Marker.PointsTypeConverter::class,
    Marker.TypeTypeConverter::class,
    Marker.IconTypeConverter::class,
    Marker.UriListTypeConverter::class
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
    var icon: Icon? = null,
    @ColumnInfo(name = "phone_number")
    var phoneNumber: String? = null,
    var imageUris: List<Uri> = listOf(),
    @ColumnInfo(name = "folder_id")
    val folderId: Long = DEFAULT_FOLDER_ID
) {
    val title get() = name ?: "Marker $id"

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

    val formattedCreatedAt: String
        get() = DateFormat
            .getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM)
            .format(createdAt)

    val formattedCreationDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(createdAt)

    val formattedCreationTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(createdAt)

    val formattedPhoneNumber: String?
        get() = phoneNumber?.let { PhoneNumberUtils.formatNumber(it, Locale.getDefault().country) }

    val latitude
        get() = position?.latitude?.toFloat()

    val longitude
        get() = position?.longitude?.toFloat()

    val details: String
        get() = "$title\nlat/lng: (${latitude},${longitude})\nhttps://maps.google.com/?q=${latitude},${longitude}"

    val coordinatesInfo: String
        get() = "lat/lng: (${latitude},${longitude})"

    init {
        if (type == Type.MARKER) icon!!.initBitmap(color)
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

    object IconTypeConverter {
        @TypeConverter
        @JvmStatic
        fun markerIconIdToIcon(iconId: Int?): Icon? = iconId?.let { Icon(iconId, true) }

        @TypeConverter
        @JvmStatic
        fun markerIconToIconId(icon: Icon?): Int? = icon?.id
    }

    object UriListTypeConverter {
        @TypeConverter
        @JvmStatic
        fun urisToString(uris: List<Uri>): String = uris.joinToString(",") { uri -> uri.toString() }

        @TypeConverter
        @JvmStatic
        fun stringToUris(urisString: String): List<Uri> =
            if (urisString.isBlank()) listOf() else urisString.split(",").map { Uri.parse(it) }
    }

    /**
     * A container class for marker bitmaps used on the map.
     *
     * @property id - id of an icon from [IconPack]
     * @property loadBitmap - flag for determining if to load bitmap. Bitmap is only required to be
     * loaded when instance is being created by room.
     */
    data class Icon(val id: Int, val loadBitmap: Boolean = false) {
        var defaultBitmap: Bitmap? = null
            private set
        val activeBitmap: Bitmap by lazy { createActiveBitmap(defaultBitmap!!, id) }

        fun initBitmap(color: Int) {
            if (!loadBitmap || defaultBitmap != null) return
            defaultBitmap = createDefaultBitmap(id, color)
        }

        companion object : KoinComponent {
            fun createDefaultBitmap(iconId: Int, color: Int): Bitmap {
                val drawable = iconPack.getIconDrawable(iconId)!!.mutate()
                return when (iconId) {
                    DEFAULT_MARKER_ICON_ID -> {
                        drawable.setColor(color).toBitmap()
                    }
                    else -> {
                        val backgroundBitmap = context
                            .getDrawable(R.drawable.ic_non_default_marker_icon_bg)!!.mutate()
                            .setColor(color).toBitmap()
                        overlayBitmaps(backgroundBitmap, drawable.toBitmap())
                    }
                }
            }

            fun createActiveBitmap(bitmap: Bitmap, iconId: Int): Bitmap {
                val activeBitmap = Bitmap.createBitmap(bitmap)
                return when (iconId) {
                    DEFAULT_MARKER_ICON_ID -> activeBitmap.applyCanvas {
                        drawCircle(21.7f.dp, 12.5f.dp, 7f.dp, activeBmpDefaultPaint)
                    }
                    else -> activeBitmap.applyCanvas {
                        drawPath(activeBmpPath, activeBmpNonDefaultPaint)
                    }
                }
            }

            private fun overlayBitmaps(bgBmp: Bitmap, overlayBmp: Bitmap): Bitmap {
                val result = Bitmap.createBitmap(bgBmp.width, bgBmp.height, bgBmp.config)
                val canvas = Canvas(result)
                canvas.drawBitmap(bgBmp, Matrix(), null)
                canvas.drawBitmap(overlayBmp, 17f.dp, 8f.dp, null)
                return result
            }

            private val activeBmpDefaultPaint by lazy {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.DKGRAY; this.style = Paint.Style.FILL
                }
            }

            private val activeBmpNonDefaultPaint by lazy {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.DKGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 3f.dp
                }
            }

            private val activeBmpPath by lazy {
                Path().apply {
                    val cornerRad = 6f.dp
                    val corners = floatArrayOf(
                        cornerRad, cornerRad,
                        cornerRad, cornerRad,
                        cornerRad, cornerRad,
                        cornerRad, cornerRad
                    )
                    val rect = RectF(11f.dp, 5f.dp, 46f.dp, 36f.dp)
                    this.addRoundRect(rect, corners, Path.Direction.CW)
                }
            }
        }
    }

    companion object : KoinComponent {
        private val iconPack = get<IconPack>()

        fun createMarker(position: LatLng) = Marker(
            arrayListOf(position), Type.MARKER, icon = Icon(DEFAULT_MARKER_ICON_ID)
        )

        fun createPolyline(points: List<LatLng?>) = Marker(points, Type.POLYLINE)
        fun createPolygon(points: List<LatLng?>) = Marker(points, Type.POLYGON)

        @BindingAdapter("markerIcon")
        @JvmStatic
        fun markerIcon(view: ImageView, marker: Marker) {
            val resources = view.context.resources
            val drawable = when (marker.type) {
                Type.MARKER -> iconPack.getIconDrawable(marker.icon!!.id)!!.mutate().setColor(Color.WHITE)
                Type.POLYLINE -> resources.getDrawable(R.drawable.ic_polyline, null)
                Type.POLYGON -> resources.getDrawable(R.drawable.ic_polygon, null)
            }
            view.setImageDrawable(drawable)
        }

        val DEFAULT_COLOR = Color.rgb(204, 54, 43)
        const val DEFAULT_MARKER_ICON_ID = 3000

        const val POLYLINE_DEFAULT_WIDTH = 3F
        const val ACTIVE_POLYLINE_WIDTH = 6F

        const val POLYGON_DEFAULT_WIDTH = 3F
        const val ACTIVE_POLYGON_WIDTH = 5F
        const val POLYGON_DEFAULT_COLOR_ALPHA = 95
        const val POLYGON_ACTIVE_COLOR_ALPHA = 150

        const val DEFAULT_FOLDER_ID = 1L

        fun Int.alpha(value: Int): Int =
            Color.argb(value, Color.red(this), Color.green(this), Color.blue(this))
    }
}
