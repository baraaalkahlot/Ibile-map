package com.ibile.features.main.datasharing

import android.net.Uri
import android.text.format.DateFormat
import androidx.core.net.toFile
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.data.database.entities.Marker
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileWriter
import java.io.Writer

private val NS = null

class DataSerializer(private val serializer: XmlSerializer) {
    fun serialize(
        foldersWithMarkers: List<FolderWithMarkers>,
        file: File,
        name: String,
        standalone: Boolean = true
    ) {
        FileWriter(file).use { write(it, name, foldersWithMarkers, standalone) }
    }

    private fun write(
        writer: Writer,
        name: String,
        data: List<FolderWithMarkers>,
        standalone: Boolean
    ) {
        with(serializer) {
            setOutput(writer)
            startDocument("UTF-8", null)
            setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            setPrefix("", "http://www.opengis.net/kml/2.2")

            tag("kml", "http://www.opengis.net/kml/2.2") {
                tag("Document") {
                    tag("name") { text(name) }
                    data.forEach { writeFolder(it, standalone) }
                }
            }
            endDocument()
        }
    }

    private fun XmlSerializer.writeFolder(
        folderWithMarkers: FolderWithMarkers,
        standalone: Boolean
    ) {
        val (folder, markers) = folderWithMarkers
        val folderColorStyleId = "im_folder_color_${folder.id}"

        writeColorStyle(folderColorStyleId, folder.color)

        tag("Folder") {
            attribute(NS, "id", "${folder.id}")
            tag("styleUrl") { text("#${folderColorStyleId}") }
            tag("name") { text(folder.title) }
            tag("ExtendedData") {
                tag("Data") {
                    attribute(NS, "name", "com_ibile_iconId")
                    tag("value") { text("${folder.iconId}") }
                }
            }
            markers.forEach { marker -> writeMarker(marker, standalone) }
        }
    }

    private fun XmlSerializer.writeMarker(marker: Marker, standalone: Boolean) {
        val poiColorStyleId = "im_marker_color_${marker.id}"
        writeColorStyle(poiColorStyleId, marker.color)
        tag("Placemark") {
            attribute(NS, "id", "${marker.id}")
            tag("name") { text(marker.title) }
            tag("description") {
                this.cdsect(
                    """
                        <pre id="com.ibile.description_p_tag">${marker.description.orEmpty()}<pre/>
                    """.trimIndent()
                )
            }
            tag("phoneNumber") { text(marker.phoneNumber.orEmpty()) }

            when {
                marker.isMarker -> {
                    tag("Point") {
                        tag("coordinates") {
                            text("${marker.position!!.longitude},${marker.position!!.latitude}")
                        }
                    }
                }
                marker.isPolyline -> {
                    tag("LineString") {
                        tag("coordinates") {
                            text(marker.points.joinToString(" ") { "${it!!.longitude},${it.latitude}" })
                        }
                    }
                }
                marker.isPolygon -> {
                    tag("Polygon") {
                        tag("outerBoundaryIs") {
                            tag("LinearRing") {
                                tag("coordinates") {
                                    text(marker.points.joinToString(" ") { "${it!!.longitude},${it.latitude}" })
                                }
                            }
                        }
                    }
                }
            }
            tag("TimeStamp") {
                tag("when") {
                    val createdAt = DateFormat.format("yyyy-MM-dd'T'hh:mm:ssZ", marker.createdAt)
                    text(createdAt.toString())
                }
            }
            tag("ExtendedData") {
                tag("Data") {
                    attribute(NS, "name", "com_ibile_iconId")
                    tag("value") { text("${marker.icon?.id}") }
                }
                if (!standalone) {
                    tag("Data") {
                        attribute(NS, "name", "com_ibile_images")
                        tag("value") {
                            text(createMarkerImagesDataJsonString(marker.imageUris))
                        }
                    }
                }
            }
            tag("styleUrl") { text("#${poiColorStyleId}") }
        }
    }

    private fun XmlSerializer.writeColorStyle(id: String, color: Int) {
        val colorString = Integer.toHexString(color)

        tag("Style") {
            attribute(NS, "id", id)
            tag("IconStyle") {
                tag("color") { text(colorString) }
                tag("colorMode") { text("normal") }
                tag("scale") { text("1") }
            }
            tag("LineStyle") {
                tag("color") { text(colorString) }
            }
            tag("PolyStyle") {
                tag("color") { text(colorString) }
            }
        }
    }

    private fun XmlSerializer.tag(
        name: String,
        namespace: String? = NS,
        block: XmlSerializer.() -> Unit
    ) {
        startTag(namespace, name)
        block()
        endTag(namespace, name)
    }

    companion object {
        private fun createMarkerImagesDataJsonString(imageUris: List<Uri>): String {
            val imagesJsonArray = JSONArray()
            imageUris.forEach { uri ->
                val jsonObject = JSONObject().apply {
                    val file = uri.toFile()
                    put("file_rel_path", file.name)
                    put("file_size", file.length())
                    put("file_extension", file.extension)
                }
                imagesJsonArray.put(jsonObject)
            }
            return imagesJsonArray.toString()
        }
    }
}
