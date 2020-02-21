package com.ibile.data.repositiories

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.ibile.utils.extensions.copyContentUriToFile
import com.ibile.utils.extensions.createImageFile
import com.ibile.utils.extensions.getExtension
import io.reactivex.Observable
import java.io.IOException

class ImageRepository(private val context: Context) {

    @Throws(java.lang.Exception::class)
    fun importImagesToApp(imageUris: List<Uri>): Observable<List<Uri>> = Observable.fromCallable {
        with(context) {
            imageUris.map { uri ->
                val extension = contentResolver.getExtension(uri)!!
                val destFile = createImageFile(extension)
                    ?: throw IOException("Could not import images")
                contentResolver.copyContentUriToFile(uri, destFile)
                Uri.fromFile(destFile)
            }
        }
    }

    fun deleteFiles(uris: List<Uri>) = Observable.fromCallable {
        uris.forEach { uri -> uri.toFile().delete() }
    }
}
