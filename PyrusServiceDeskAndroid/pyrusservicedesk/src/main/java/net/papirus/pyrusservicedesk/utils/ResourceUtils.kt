package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.util.TypedValue

/**
 * Extracts color from context theme by attribute id
 * @param context context which theme is used to resolve the attribute
 * @param colorAttrId id of the attribute
 * @return int color value
 */
@ColorInt
internal fun getColor(context: Context, @AttrRes colorAttrId: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(colorAttrId, typedValue, true)
    return try {
        ContextCompat.getColor(context, typedValue.resourceId)
    } catch (ex: Resources.NotFoundException) {
        Color.WHITE
    }
}

internal fun Context.isTablet(): Boolean {
    return resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
}