package com.pyrus.pyrusservicedesk.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * @return true if [permission] is already granted.
 */
internal fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Determine whether a particular permission is present in Manifest.
 *
 * @param permission The name of the permission being checked.
 */
internal fun Context.hasPermissionInManifeset(permission: String): Boolean {
    val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    return info.requestedPermissions?.any { it == permission } ?: false
}