
package com.ibile.utils.extensions

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.ibile.utils.FILE_PROVIDER_AUTHORITY
import com.ibile.utils.timeStampString
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass

const val BUFFER_SIZE = 8 * 1024

@Throws(IOException::class)
fun Context.createImageFile(
    extension: String = "jpg", fileName: String = timeStampString()
): File? {
    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("${extension}_${fileName}_", ".$extension", storageDir)
}

fun Context.getProviderUri(file: File): Uri =
    FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file)

fun Context.grantUriPermissions(uri: Uri, intent: Intent, flags: Int) {
    packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .apply {
            forEach {
                val packageName = it.activityInfo.packageName
                grantUriPermission(packageName, uri, flags)
            }
        }
}

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

fun Context.startResolvableActivity(intent: Intent) {
    if (intent.resolveActivity(this.packageManager) != null) {
        this.startActivity(intent)
    }
}

fun Context.restartApp(activityClass: KClass<out Activity>) {
    val intent = Intent(this, activityClass.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(intent)
    Runtime.getRuntime().exit(0)
}
