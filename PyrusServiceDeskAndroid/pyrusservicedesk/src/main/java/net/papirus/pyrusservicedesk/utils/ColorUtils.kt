package net.papirus.pyrusservicedesk.utils

import android.graphics.Color
import android.support.annotation.ColorInt

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

internal enum class ColorChannel {
    Alpha,
    Red,
    Green,
    Blue
}