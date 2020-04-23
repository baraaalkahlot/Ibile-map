package com.ibile.features.main.datasharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.core.net.toFile
import com.ibile.data.database.entities.FolderWithMarkers
import com.ibile.utils.extensions.getProviderUri
import com.ibile.utils.extensions.grantUriPermissions
import de.siegmar.fastcsv.writer.CsvWriter
import io.reactivex.Single
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Exporter(
    private val serializer: DataSerializer,
    private val csvWriter: CsvWriter,
    private val context: Context
) {
    private val baseDir = context.getExternalFilesDir(DIR_TEMP_ENTITY_DATA_EXPORT_FILE)
    private lateinit var exportFile: File

    fun exportKml(folders: List<FolderWithMarkers>, name: String) =
        export("im_pois_${timestampString}.kml") {
            serializer.serialize(folders, this, name)
        }

    fun exportSnapshotBitmap(bitmap: Bitmap): Single<Intent> =
        export("im_pois_${timestampString}.jpg") {
            FileOutputStream(this).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                it.flush()
            }
        }

    fun exportCsv(folders: List<FolderWithMarkers>): Single<Intent> =
        export("im_pois_${timestampString}.csv") {
            val headerRow = arrayOf(
                "Folder name",
                "Folder color",
                "Latitude",
                "Longitude",
                "Title",
                "Description",
                "Color",
                "Phone number",
                "Timestamp",
                "Icon id"
            )
            val rows = mutableListOf(headerRow)
            folders.forEach { (folder, markers) ->
                markers.forEach { marker ->
                    rows.add(
                        arrayOf(
                            folder.title,
                            folder.color.toString(),
                            marker.position!!.latitude.toString(),
                            marker.position!!.longitude.toString(),
                            marker.title,
                            marker.description.orEmpty(),
                            Integer.toHexString(marker.color),
                            marker.phoneNumber.orEmpty(),
                            marker.formattedCreatedAt,
                            marker.icon!!.id.toString()
                        )
                    )
                }
            }
            csvWriter.write(this, StandardCharsets.UTF_8, rows)
        }

    fun onCompleteOrCancelExport() {
        if (::exportFile.isInitialized) exportFile.delete()
    }

    fun exportKmz(folders: List<FolderWithMarkers>, name: String): Single<Intent> =
        Single.create { emitter ->
            val exportFolder = File(baseDir, "im_pois_${timestampString}").apply {
                if (!exists()) mkdirs()
            }
            val kmlFile = File(exportFolder, "doc.kml")
            serializer.serialize(folders, kmlFile, name, false)
            folders.flatMap { it.markers }.flatMap { it.imageUris }
                .map { it.toFile() }.forEach { it.copyTo(File(exportFolder, it.name)) }
            exportFile = zipFolder(exportFolder)
            exportFolder.deleteRecursively()

            val exportIntent = createExportIntent(exportFile)
            emitter.onSuccess(exportIntent)
        }

    private fun zipFolder(folder: File): File {
        val outputZip = File(folder.parent, "${folder.name}.kmz")
        val outputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip)))
        outputStream.use { stream ->
            for (file in folder.listFiles()!!) {
                FileInputStream(file).use { inputStream ->
                    BufferedInputStream(inputStream).use { origin ->
                        val path = file.path
                        val name = path.substring(path.lastIndexOf('/'))
                        val entry = ZipEntry(name)
                        stream.putNextEntry(entry)
                        origin.copyTo(stream, 1024)
                    }
                }
            }
        }
        return outputZip
    }

    private fun export(fileName: String, block: File.() -> Unit): Single<Intent> {
        return Single.create { emitter ->
            exportFile = File(baseDir, fileName)
            block(exportFile)
            val exportIntent = createExportIntent(exportFile)
            emitter.onSuccess(exportIntent)
        }
    }

    private fun createExportIntent(file: File): Intent {
        val fileUri = context.getProviderUri(file)
        val shareIntent = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, fileUri)
            .setType(context.contentResolver.getType(fileUri))
        val intent = Intent.createChooser(shareIntent, file.name)
        context.grantUriPermissions(fileUri, intent, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    companion object {
        private val timestampString: String
            get() = DateFormat.format("yyyy_MM_dd-hh_mm_ss", Calendar.getInstance().time).toString()

        private const val DIR_TEMP_ENTITY_DATA_EXPORT_FILE = "TempMapExportFiles"
    }
}
