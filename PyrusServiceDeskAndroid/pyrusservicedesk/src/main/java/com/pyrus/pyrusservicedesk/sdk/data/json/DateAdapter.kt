package com.pyrus.pyrusservicedesk.sdk.data.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Converts strings like 2025-01-16T13:51:35.347Z to the timestamp.  */
class DateAdapter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @FromJson
    @DateJ
    fun fromJson(dateString: String?): Long? {
        val date = dateString?.let { dateFormat.parse(it) }
        return date?.time
    }

    @FromJson
    @DateJ
    fun fromJson(dateString: String): Long {
        val date = dateFormat.parse(dateString)
        return date?.time ?: 0
    }

    @ToJson
    fun toJson(@DateJ time: Long?): String? {
        if (time == null) return null
        val date = Date(time)
        return dateFormat.format(date)
    }

    @ToJson
    fun toJson(@DateJ time: Long): String {
        val date = Date(time)
        return dateFormat.format(date)
    }
}