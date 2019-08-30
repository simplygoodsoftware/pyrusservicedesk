package com.pyrus.pyrusservicedesk.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import android.util.TypedValue

/**
 * Extracts color from context theme by attribute id
 * @param context context which theme is used to resolve the attribute
 * @param colorAttrId id of the attribute
 * @return int color value
 */
@ColorInt
internal fun getColorByAttrId(context: Context, @AttrRes colorAttrId: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(colorAttrId, typedValue, true)
    return try {
        ContextCompat.getColor(context, typedValue.resourceId)
    } catch (ex: Resources.NotFoundException) {
        Color.WHITE
    }
}

/**
 * @return TRUE if device is considered tablet.
 */
internal fun Context.isTablet(): Boolean {
    return resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
}