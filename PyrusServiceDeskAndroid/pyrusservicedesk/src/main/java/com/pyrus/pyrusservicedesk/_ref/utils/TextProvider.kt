package com.pyrus.pyrusservicedesk._ref.utils

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date


internal sealed interface TextProvider: Parcelable {

    @Parcelize
    data class Simple(val text: String): TextProvider

    @Parcelize
    data class Date(val timestamp: Long, @StringRes val format: Int): TextProvider

    @Parcelize
    data class Res(@StringRes val res: Int): TextProvider

    @Parcelize
    data class Format(@StringRes val res: Int, val params: List<String>): TextProvider

    @Parcelize
    data class Combine(val leftText: TextProvider, val rightText: TextProvider): TextProvider

}

internal fun Int.textRes(): TextProvider = TextProvider.Res(this)

internal operator fun TextProvider.plus(textProvider: TextProvider): TextProvider {
    return TextProvider.Combine(this, textProvider)
}

internal operator fun TextProvider.plus(text: String): TextProvider {
    return TextProvider.Combine(this, text.textRes())
}

internal fun String.textRes(): TextProvider = TextProvider.Simple(this)

internal fun TextProvider.text(context: Context): String = when(this) {
    is TextProvider.Simple -> text
    is TextProvider.Date -> context.formatDate(timestamp, format)
    is TextProvider.Res -> context.getString(res)
    is TextProvider.Format -> context.getString(res, *params.toTypedArray())
    is TextProvider.Combine -> {
        leftText.text(context) + rightText.text(context)
    }
}

internal fun Context.formatDate(timestamp: Long, @StringRes format: Int): String {
    val date = Date(timestamp)
    return printDate(date, getString(format))
}

// parameter could be in any timeZone
private fun printDate(date: Date?, format: String?): String {
    return SimpleDateFormat(format).format(date)
}