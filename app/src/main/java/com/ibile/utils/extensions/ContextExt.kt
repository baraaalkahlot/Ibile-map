package com.ibile.utils.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.ibile.utils.FILE_PROVIDER_AUTHORITY
import com.ibile.utils.timeStampString
import java.io.File
import java.io.IOException

const val BUFFER_SIZE = 8 * 1024

@Throws(IOException::class)
fun Context.createImageFile(
    extension: String = "jpg", fileName: String = timeStampString()
): File? {
    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return try {
        File.createTempFile("${extension}_${fileName}_", ".$extension", storageDir)
    } catch (exception: IOException) {
        null
    }
}

fun Context.getProviderUri(file: File): Uri =
    FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file)

fun ContentResolver.copyContentUriToFile(uri: Uri, file: File): File {
    openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output, BUFFER_SIZE) }
    }
    return file
}

fun ContentResolver.getExtension(uri: Uri): String? =
    MimeTypeMap.getSingleton().getExtensionFromMimeType(getType(uri))

fun Context.copyTextToClipboard(text: String, label: String = "") {
    val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clipData)
}