package com.pyrus.pyrusservicedesk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Vendor(
    val appId: String,
    val orgName: String,
    val orgUrl: String?,
    val orgDescription: String?,
): Parcelable {
    companion object {
        const val KEY_VENDORS = "KEY_VENDORS"
    }
}