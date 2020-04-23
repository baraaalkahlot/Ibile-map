package com.ibile.features.dataimport

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment.DIRECTORY_PICTURES
import androidx.core.net.toUri
import com.google.android.libraries.maps.model.LatLng
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.database.entities.Marker
import com.maltaisn.icondialog.pack.IconPack
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.reader.CsvRow
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import io.reactivex.Single
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.KoinComponent
import org.koin.core.get
import java.io.*
import java.text.DateFormat
import java.text.ParseException
import java.util.*
import java.util.zip.ZipInputStream

class DataProcessor(private val csvReader: CsvReader, private val context: Context) {
    fun processCsv(uri: Uri): Single<List<Marker>> = Single.create { emitter ->
        val markers = context.contentResolver.openInputStream(uri)!!.use {
            csvReader.setContainsHeader(true)
            val csv = csvReader.read(it.reader())
            val headerRow = csv.header

            if (headerRow.isNullOrEmpty()
                || !headerRow.contains(CsvFields.LATITUDE)
                || !headerRow.contains(CsvFields.LONGITUDE)
            ) throw UnsupportedOperationException("Cannot process CSV: does not contain required fields")

            csv.rows.mapNotNull { row -> getMarkerFromRow(row) }
        }
        emitter.onSuccess(markers)
    }

    fun processKml(uri: Uri, standalone: Boolean = true): Single<List<FolderWithMarkers>> =
        Single.create { emitter ->
            val folderWithMarkers = context.contentResolver.openInputStream(uri)!!.use {
                processKmlStream(it, standalone)
            }
            emitter.onSuccess(folderWithMarkers)
        }

    fun processKmz(uri: Uri) = Single.create<List<FolderWithMarkers>> { emitter ->
        val result = context.contentResolver.openInputStream(uri)!!.use {
            val kmzFiles = unzip(it)
            val kmlFile = kmzFiles.find { file -> file.extension == "kml" }
                ?: throw Exception("Kmz Archive does not include a kml doc file")
            FileInputStream(kmlFile)
                .use { stream -> processKmlStream(stream, false) }
                .also { kmlFile.delete() }
        }
        emitter.onSuccess(result)
    }

    private fun getMarkerFromRow(row: CsvRow): Marker? {
        val fieldMap = row.fieldMap
        val latitude = fieldMap[CsvFields.LATITUDE]?.toDoubleOrNull()
        val longitude = fieldMap[CsvFields.LONGITUDE]?.toDoubleOrNull()

        if (latitude == null || longitude == null) return null
        return Marker.createMarker(LatLng(latitude, longitude))
            .copy(
                icon = Marker.Icon(processIconId(fieldMap[CsvFields.ICON_ID])),
                name = fieldMap[CsvFields.TITLE],
                description = fieldMap[CsvFields.DESCRIPTION],
                color = fieldMap[CsvFields.COLOR]?.toIntOrNull() ?: Marker.DEFAULT_COLOR,
                phoneNumber = fieldMap[CsvFields.PHONE_NUMBER],
                createdAt = processDate(fieldMap[CsvFields.TIMESTAMP])
            )
    }

    private fun processKmlStream(
        stream: InputStream,
        standalone: Boolean
    ): List<FolderWithMarkers> {
        val documentJson = XmlToJson.Builder(stream, null)
            .forceList("/kml/Document/Folder")
            .forceList("/kml/Document/Style")
            .forceList("/kml/Document/Folder/Style")
            .forceList("/kml/Document/Folder/Placemark")
            .forceList("/kml/Document/Folder/Placemark/ExtendedData/Data")
            .build()
            .toJson()!!
            .getJSONObject("kml")
            .getJSONObject("Document")

        val foldersJson = documentJson.getJSONArray("Folder")
        val foldersStylesMap = getStylesMap(documentJson.getJSONArray("Style"))

        val result = mutableListOf<FolderWithMarkers>()
        for (i in 0 until foldersJson.length()) {
            val folderJson = foldersJson.getJSONObject(i)
            val folderWithMarkers = processFolder(folderJson, foldersStylesMap, standalone)
            result.add(folderWithMarkers)
        }
        return result
    }

    private fun processFolder(
        folderJson: JSONObject,
        stylesMap: Map<String, JSONObject>,
        standalone: Boolean
    ): FolderWithMarkers = with(folderJson) {
        val markersStylesMap = getStylesMap(getJSONArray("Style"))
        val markersJson = getJSONArray("Placemark")

        var markers = mutableListOf<Marker>()
        for (i in 0 until markersJson.length()) {
            val markerJson = markersJson.getJSONObject(i)
            val marker = getMarkerFromJsonObject(markerJson, markersStylesMap, standalone)
            markers.add(marker)
        }

        val id = getString("id").toLong()
        markers = markers.map { it.copy(folderId = id) }.toMutableList()

        val name = getString("name")
        val color = getColor(stylesMap, getString("styleUrl"))
        val iconId = processIconId(
            getJSONObject("ExtendedData").getJSONObject("Data").getString("value")
        )
        val folder = Folder(name, id, iconId, color)
        FolderWithMarkers(folder, markers)
    }

    private fun getStylesMap(styles: JSONArray): Map<String, JSONObject> {
        val stylesMap: MutableMap<String, JSONObject> = mutableMapOf()
        for (i in 0 until styles.length()) {
            val style = styles.getJSONObject(i)
            val id = style.getString("id")
            stylesMap[id] = style
        }
        return stylesMap
    }

    private fun getMarkerFromJsonObject(
        json: JSONObject,
        stylesMap: Map<String, JSONObject>,
        standalone: Boolean
    ): Marker = with(json) {
//        val id = getString("id").toLong()
        val phoneNumber = getString("phoneNumber")
        val name = getString("name")
        val description = getString("description")
        val createdAt = processDate(getJSONObject("TimeStamp").getString("when"))
        val color = getColor(stylesMap, getString("styleUrl"))
        val (type, coords) = getMarkerTypeAndCoordinates(this)

        val customDataJson = json.getJSONObject("ExtendedData").getJSONArray("Data")
        val iconId = getMarkerIconId(customDataJson)
        val imageUris: List<Uri> =
            if (!standalone) getMarkerImageUris(customDataJson) else listOf()

        Marker(
            coords,
            type,
            id = 0, // marker is exported with id but imports are meant to create new markers regardless if they are duplicate
            name = name,
            createdAt = createdAt,
            updatedAt = createdAt,
            description = description,
            color = color,
            icon = Marker.Icon(iconId),
            phoneNumber = phoneNumber,
            imageUris = imageUris
        )
    }

    private fun getColor(stylesMap: Map<String, JSONObject>, styleUrl: String): Int {
        val styleId = styleUrl.trimStart('#')
        val style = stylesMap[styleId] ?: error("")
        val colorHexString = "#${(style.getJSONObject("IconStyle").getString("color"))}"
        return Color.parseColor(colorHexString)
    }

    private fun getMarkerIconId(customDataJson: JSONArray): Int {
        var iconIdString: String? = null
        for (i in 0 until customDataJson.length()) {
            val data = customDataJson.getJSONObject(i)
            if (data.getString("name") == "com_ibile_iconId") {
                iconIdString = data.getString("value")
                break
            }
        }
        return processIconId(iconIdString)
    }

    private fun getMarkerImageUris(customDataJson: JSONArray): List<Uri> {
        var imageUrisString: String? = null
        for (i in 0 until customDataJson.length()) {
            val data = customDataJson.getJSONObject(i)
            if (data.getString("name") == "com_ibile_images") {
                imageUrisString = data.getString("value")
                break
            }
        }
        val imageUrisJsonArray = JSONArray(imageUrisString!!)
        val imageUris: MutableList<Uri> = mutableListOf()
        for (i in 0 until imageUrisJsonArray.length()) {
            val imageUriJson = imageUrisJsonArray.getJSONObject(i)
            val imageFileName = imageUriJson.getString("file_rel_path")
            val imageFile = File(context.getExternalFilesDir(DIRECTORY_PICTURES), imageFileName)
            imageUris.add(imageFile.toUri())
        }
        return imageUris
    }

    private fun getMarkerTypeAndCoordinates(json: JSONObject): Pair<Marker.Type, List<LatLng>> {
        if (json.has("Point")) {
            val string = json.getJSONObject("Point").getString("coordinates").trim()
            val list = string.split(',').map { it.toDouble() }
            return Marker.Type.MARKER to listOf(LatLng(list[1], list[0]))
        }
        if (json.has("LineString")) {
            val string = json.getJSONObject("LineString").getString("coordinates").trim()
            return Marker.Type.POLYLINE to string.split(" ")
                .map {
                    val coordList = it.split(",")
                    LatLng(coordList[1].toDouble(), coordList[0].toDouble())
                }
        }
        val string = json.getJSONObject("Polygon").getJSONObject("outerBoundaryIs")
            .getJSONObject("LinearRing").getString("coordinates").trim()
        return Marker.Type.POLYGON to string.split(" ")
            .map {
                val coordList = it.split(",")
                LatLng(coordList[1].toDouble(), coordList[0].toDouble())
            }
    }

    private fun unzip(inputStream: InputStream): List<File> =
        BufferedInputStream(inputStream).use { bis ->
            ZipInputStream(bis).use { zis ->

                val importFiles = mutableListOf<File>()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var entry = zis.nextEntry

                while (entry != null) {
                    val file = if (entry.name.endsWith("kml"))
                        File(context.getExternalFilesDir(DIR_TEMP_IMPORT), entry.name)
                    else File(context.getExternalFilesDir(DIRECTORY_PICTURES), entry.name)

                    // don't copy image files that are already existing in the pictures directory
                    // for markers
                    if (file.exists() && file.extension != "kml") {
                        importFiles.add(file); entry = zis.nextEntry; continue
                    }
                    FileOutputStream(file).use { fout ->
                        while (true) {
                            val count = zis.read(buffer)
                            if (count == -1) {
                                importFiles.add(file); entry = zis.nextEntry; break
                            }
                            fout.write(buffer, 0, count)
                        }
                    }
                }
                importFiles
            }
        }

    companion object : KoinComponent {
        object CsvFields {
            const val FOLDER_NAME = "Folder name"
            const val FOLDER_COLOR = "Folder color"
            const val LATITUDE = "Latitude"
            const val LONGITUDE = "Longitude"
            const val TITLE = "Title"
            const val DESCRIPTION = "Description"
            const val COLOR = "Color"
            const val PHONE_NUMBER = "Phone number"
            const val TIMESTAMP = "Timestamp"
            const val ICON_ID = "Icon id"
        }

        fun processIconId(iconId: String?): Int {
            val id = iconId?.toIntOrNull() ?: return Marker.DEFAULT_MARKER_ICON_ID
            return if (get<IconPack>().getIcon(id) != null) id else Marker.DEFAULT_MARKER_ICON_ID
        }

        fun processDate(timestamp: String?): Date {
            if (timestamp == null) return Date()
            return try {
                DateFormat.getDateInstance().parse(timestamp) ?: Date()
            } catch (ex: ParseException) {
                Date()
            }
        }

        private const val DIR_TEMP_IMPORT = "TempMapExportFiles"
    }
}
