package com.pyrus.pyrusservicedesk.utils

import android.annotation.SuppressLint
import android.content.Context
import com.pyrus.pyrusservicedesk.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

internal const val ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
internal const val MILLISECONDS_IN_MINUTE = 60 * 1000
internal const val MILLISECONDS_IN_SECOND = 1000
internal const val MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000
internal const val MILLISECONDS_IN_HOUR = 60 * 60 * 1000
private const val MONTHS_IN_YEAR = 12
private const val DAYS_IN_WEEK = 7

/**
 * Provides localized time text of the receiver date.
 */
@SuppressLint("SimpleDateFormat")
internal fun Date.getTimeText(context: Context): String {
    return SimpleDateFormat(context.resources.getString(R.string.psd_time_format))
        .format(
            with(TimeZone.getDefault()){
                Calendar.getInstance(this).apply {
                    timeInMillis = this@getTimeText.time + getOffset(System.currentTimeMillis())
                }.time
            })
}

/**
 * Provides localized date in a "when" manner, like "today" or "20th of march". [now] is used as reference values
 * to calculate the result.
 */
@SuppressLint("SimpleDateFormat")
internal fun Date.getWhen(context: Context, now: Calendar): String {
    val zone = TimeZone.getDefault()
    val date = Calendar.getInstance(zone).apply {
        timeInMillis = this@getWhen.time + zone.rawOffset
    }
    return when {
        date.isSameDay(now) -> context.getString(R.string.psd_today)
        date.isOneDayBefore(now) -> context.getString(R.string.psd_yesterday)
        date.isSameYear(now) -> SimpleDateFormat(context.getString(R.string.psd_date_format_d_m)).format(this)
        else -> SimpleDateFormat(context.getString(R.string.psd_date_format_d_m_y)).format(this)
    }
}

/**
 * Provides localized date in a "how much passed" manner like "1 day ago" of "5 years ago". [from] is used as
 * reference value to calculate the result.
 */
internal fun Date.getTimePassedFrom(context: Context, from: Calendar): String {
    val zone = TimeZone.getDefault()
    val date = Calendar.getInstance(zone).apply {
        timeInMillis = this@getTimePassedFrom.time + zone.rawOffset
    }
    with(arrayOf(
        from.yearsFrom(date),
        from.monthsFrom(date),
        from.daysFrom(date),
        from.hoursFrom(date),
        from.minutesFrom(date))){

        return when {
            get(0) > 0 -> context.resources.getQuantityString(R.plurals.psd_years_ago, get(0))
            get(1) > 0 -> context.getString(R.string.psd_months_ago, get(1))
            get(2) > DAYS_IN_WEEK -> context.getString(R.string.psd_weeks_ago, ceil(get(2) / DAYS_IN_WEEK.toDouble()).toInt())
            get(2) > 0 -> context.getString(R.string.psd_days_ago, get(2))
            get(3) > 0 -> context.getString(R.string.psd_hours_ago, get(3))
            get(4) > 0 -> context.getString(R.string.psd_minutes_ago, get(4))
            else -> context.getString(R.string.psd_recent)
        }
    }
}

private fun Calendar.isSameDay(another: Calendar): Boolean = daysFrom(another) == 0

private fun Calendar.isOneDayBefore(another: Calendar): Boolean = daysFrom(another) == -1

private fun Calendar.isSameYear(another: Calendar) = get(Calendar.YEAR) == another.get(Calendar.YEAR)

private fun Calendar.minutesFrom(another: Calendar): Int {
    val millisecondsInMinute = MILLISECONDS_IN_MINUTE.toDouble()
    return ceil(this.timeInMillis / millisecondsInMinute).toInt() -
            ceil(another.timeInMillis / millisecondsInMinute).toInt()
}

private fun Calendar.hoursFrom(another: Calendar): Int {
    val millisecondsInHour = MILLISECONDS_IN_HOUR.toDouble()
    return ceil(this.timeInMillis / millisecondsInHour).toInt() -
            ceil(another.timeInMillis / millisecondsInHour).toInt()
}

private fun Calendar.daysFrom(another: Calendar): Int {
    val millisecondsInDay = MILLISECONDS_IN_DAY.toDouble()
    return ceil(this.timeInMillis / millisecondsInDay).toInt() -
            ceil(another.timeInMillis / millisecondsInDay).toInt()
}

private fun Calendar.monthsFrom(another: Calendar): Int {
    return yearsFrom(another) * MONTHS_IN_YEAR + get(Calendar.MONTH) - get(Calendar.MONTH)
}

private fun Calendar.yearsFrom(another: Calendar): Int {
    return get(Calendar.YEAR) - another.get(Calendar.YEAR)
}

