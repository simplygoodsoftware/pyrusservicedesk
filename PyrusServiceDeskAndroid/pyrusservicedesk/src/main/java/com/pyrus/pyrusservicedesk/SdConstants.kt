package com.pyrus.pyrusservicedesk

internal object SdConstants {

    val PYRUS_BASE_DOMAIN: String = when {
        BuildConfig.DEBUG -> "pyrus.com"
        else -> "pyrus.com" // НЕ ТРОГАТЬ!!!
    }

}