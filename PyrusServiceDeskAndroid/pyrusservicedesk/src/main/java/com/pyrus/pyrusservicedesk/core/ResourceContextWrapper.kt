package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class ResourceContextWrapper(private val appContext: Application) {
    //private val locale: Locale? = getLocaleFromContext()

    fun getLocalizedContext(): Context = appContext.createConfigurationContext(
        Configuration(appContext.resources.configuration).apply {
            setLocale(getLocaleFromContext())
        }
    )
    fun getLocaleFromContext(): Locale {
        val config = appContext.resources.configuration
        return config.locales.get(0)
    }

//    private fun getLocalizedString(resId: Int): String {
//        return localizedContext.getString(resId)
//    }
}