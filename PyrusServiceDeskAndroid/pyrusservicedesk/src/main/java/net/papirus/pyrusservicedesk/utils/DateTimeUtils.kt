package net.papirus.pyrusservicedesk.utils

import android.content.Context
import com.example.pyrusservicedesk.R
import java.text.SimpleDateFormat
import java.util.*

internal const val ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
private const val TIME_ZONE_UTC = "UTC"
private const val MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000

internal fun getTimeText(context: Context, date: Date): String {
    return SimpleDateFormat(context.resources.getString(R.string.psd_time_format)).format(date)
}

internal fun Date.getWhen(context: Context, now: Calendar): String {
    val date = Calendar.getInstance().apply { time = this@getWhen }
    return when {
        date.isSameDay(now) -> context.getString(R.string.psd_today)
        date.isOneDayBefore(now) -> context.getString(R.string.psd_yesterday)
        date.isSameYear(now) -> SimpleDateFormat(context.getString(R.string.psd_date_format_d_m)).format(this)
        else -> SimpleDateFormat(context.getString(R.string.psd_date_format_d_m_y)).format(this)
    }
}

private fun Calendar.isSameDay(another: Calendar): Boolean = daysDiffBy(another) == 0

private fun Calendar.isOneDayBefore(another: Calendar): Boolean = daysDiffBy(another) == -1

private fun Calendar.isSameYear(another: Calendar) = get(Calendar.YEAR) == another.get(Calendar.YEAR)

private fun Calendar.daysDiffBy(another: Calendar): Int {
    val millisecondsInDay = MILLISECONDS_IN_DAY.toDouble()
    return Math.ceil(this.timeInMillis / millisecondsInDay).toInt() -
            Math.ceil(another.timeInMillis / millisecondsInDay).toInt()
}


