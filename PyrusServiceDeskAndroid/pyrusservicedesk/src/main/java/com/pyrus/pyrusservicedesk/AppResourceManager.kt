package com.pyrus.pyrusservicedesk

import android.content.Context

class AppResourceManager(
    val context: Context
) {
    fun getString(res: Int): String = context.getString(res)
}