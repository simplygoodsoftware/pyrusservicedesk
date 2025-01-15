package com.pyrus.pyrusservicedesk.sdk.data.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Converts strings like /Date(1668518551683)/ to the timestamp.  */
class DateAdapter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @ToJson
    fun toJson(@DateJ timestamp: Long?): String? {
        if (timestamp == null) return null
        return "/Date($timestamp)/"
    }

    @ToJson
    fun toJson(@DateJ timestamp: Long): String {
        return "/Date($timestamp)/"
    }

    @FromJson
    @DateJ
    fun fromJson(date: Date?): Long? {
        return date?.time
    }

    @FromJson
    @DateJ
    fun fromJson(date: String): Long {
        val simpleDate = dateFormat.parse(date) ?: throw IllegalArgumentException("Invalid date format")
        return simpleDate.time
//        val startIndex = date.indexOf('(')
//        val endIndex = date.indexOf(')')
//        return date.substring(startIndex + 1, endIndex).toLong()

    }

}