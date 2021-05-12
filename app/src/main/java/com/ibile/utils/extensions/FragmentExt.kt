package com.ibile.utils.extensions

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ibile.core.currentContext

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

    Log.d("OKAYCHECKS", "INTENT CALLS?")
    if (intent.resolveActivity(currentContext.packageManager) == null) return
    startActivityForResult(intent, requestCode)
}

val Fragment.navController
    get() = findNavController()
