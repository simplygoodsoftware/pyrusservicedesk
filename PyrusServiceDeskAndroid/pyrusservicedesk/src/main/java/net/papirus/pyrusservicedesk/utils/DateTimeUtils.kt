package net.papirus.pyrusservicedesk.utils

import android.content.Context
import com.example.pyrusservicedesk.R
import java.text.SimpleDateFormat
import java.util.*

private const val TIME_ZONE_UTC = "UTC"
private const val ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

internal fun parseUtcIsoDate(isoDate: String): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone(TIME_ZONE_UTC))
            .apply {
                time = SimpleDateFormat(ISO_DATE_PATTERN, Locale.US)
                        .parse(isoDate)
            }
}

internal fun getTimeText(context: Context, calendar: Calendar): String {
    return SimpleDateFormat(context.resources.getString(R.string.psd_time_format))
            .format(
                    (calendar.clone() as Calendar).apply {
                        timeZone = TimeZone.getDefault()
                    }.time)
}