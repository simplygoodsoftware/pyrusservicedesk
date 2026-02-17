package com.pyrus.pyrusservicedesk.core

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import java.util.Locale

class ResourceContextWrapper() {
    private var appLocale: Locale? = Locale.getDefault()

    fun createLocalizedContext(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(appLocale)
        config.setLayoutDirection(appLocale)

        val newContext = context.createConfigurationContext(config)
        return ContextThemeWrapper(newContext, context.theme)
    }

    fun setLocale(locale: Locale) {
        this.appLocale = locale
    }
    fun getLocaleFromContext(): Locale {
        return appLocale ?: Locale.getDefault()
    }

}