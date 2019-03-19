package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.support.annotation.ColorInt

internal const val CHANNEL_MAX_VALUE = 255

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
    val color = Color.parseColor(String.format("#%06X", 0xFFFFFF and backgroundColor))
    val ref = 0.2126 * Math.pow(Color.red(color)/CHANNEL_MAX_VALUE.toDouble(), 2.2)
    +  0.7151 * Math.pow(Color.green(color)/CHANNEL_MAX_VALUE.toDouble(), 2.2)
    +  0.0721 * Math.pow(Color.blue(color)/CHANNEL_MAX_VALUE.toDouble(), 2.2)
    return when{
        ref > 0.18 -> getColor(context, android.R.attr.textColorPrimary)
        else -> getColor(context, android.R.attr.textColorPrimaryInverse)
    }
}

internal enum class ColorChannel {
    Alpha,
    Red,
    Green,
    Blue
}