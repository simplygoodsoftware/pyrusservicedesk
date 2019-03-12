package net.papirus.pyrusservicedesk.utils

import android.content.Context
import com.example.pyrusservicedesk.R
import java.text.SimpleDateFormat
import java.util.*

private const val TIME_ZONE_UTC = "UTC"
internal const val ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

internal fun getTimeText(context: Context, date: Date): String {
    return SimpleDateFormat(context.resources.getString(R.string.psd_time_format)).format(date)
}