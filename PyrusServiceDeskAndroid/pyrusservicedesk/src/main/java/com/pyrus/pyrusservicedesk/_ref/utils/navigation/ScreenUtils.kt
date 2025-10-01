package com.pyrus.pyrusservicedesk._ref.utils.navigation

internal object ScreenUtils {

    fun generateKey(screenName: String): String {
        return "$screenName:${System.currentTimeMillis()}"
    }

    fun extractName(screenKey: String): String {
        return screenKey.split(":").firstOrNull() ?: ""
    }

}