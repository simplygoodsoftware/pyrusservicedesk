package com.pyrus.pyrusservicedesk

internal object SdConstants {

    val PYRUS_BASE_DOMAIN: String = when {
        BuildConfig.DEBUG -> "dev.pyrus.com"
        else -> "pyrus.com" // НЕ ТРОГАТЬ!!!
    }

}