package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v4.graphics.ColorUtils

internal const val COLOR_CHANNEL_MAX_VALUE = 255

@ColorInt
internal fun adjustColor(@ColorInt color: Int, channel: ColorChannel, multiplier: Float): Int {
    fun adjust(currentValue: Int, current: ColorChannel, desired: ColorChannel): Int {
        return when (current) {
            desired -> Math.round(currentValue * multiplier)
            else -> currentValue
        }
    }
    val alpha = adjust(Color.alpha(color), ColorChannel.Alpha, channel)
    val red = adjust(Color.red(color), ColorChannel.Red, channel)
    val green = adjust(Color.green(color), ColorChannel.Green, channel)
    val blue = adjust(Color.blue(color), ColorChannel.Blue, channel )
    return Color.argb(alpha, red, green, blue)
}

@ColorInt
internal fun getTextColorOnBackground(context: Context, @ColorInt backgroundColor: Int): Int {
    return with (Color.parseColor(String.format("#%06X", 0xFFFFFF and backgroundColor))){
        when {
            ColorUtils.calculateLuminance(this) > 0.6 -> getColor(context, android.R.attr.textColorPrimary)
            else -> getColor(context, android.R.attr.textColorPrimaryInverse)
        }
    }
}

internal enum class ColorChannel {
    Alpha,
    Red,
    Green,
    Blue
}