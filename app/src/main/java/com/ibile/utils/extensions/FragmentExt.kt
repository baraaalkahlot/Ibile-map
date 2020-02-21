package com.ibile.utils.extensions

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ibile.core.currentContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

fun Fragment.permissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(currentContext, permission) == PERMISSION_GRANTED

fun Fragment.runWithPermissions(
    block: () -> Unit,
    permission: String,
    requestCode: Int,
    showRequestPermissionRationale: () -> Unit = {}
) {
    when {
        permissionGranted(permission) -> block()
//        shouldShowRequestPermissionRationale(permission) -> showRequestPermissionRationale()
        else -> requestPermissions(arrayOf(permission), requestCode)
    }
}

fun Fragment.startResolvableActivity(intent: Intent) {
    if (intent.resolveActivity(currentContext.packageManager) == null) return
    startActivity(intent)
}

fun Fragment.startResolvableActivityForResult(intent: Intent, requestCode: Int) {
    if (intent.resolveActivity(currentContext.packageManager) == null) return
    startActivityForResult(intent, requestCode)
}
