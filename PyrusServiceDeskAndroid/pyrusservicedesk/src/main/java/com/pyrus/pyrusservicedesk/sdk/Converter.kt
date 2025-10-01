package com.pyrus.pyrusservicedesk.sdk

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pyrus.pyrusservicedesk._ref.data.RatingTextValues

class Converter {
    @TypeConverter
    fun listRatingTextValuesToJson(value: List<RatingTextValues>): String = Gson().toJson(value).toString()

    @TypeConverter
    fun jsonToListRatingTextValues(value: String): List<RatingTextValues> = Gson().fromJson(value, Array<RatingTextValues>::class.java).toList()

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return Gson().toJson(map)
    }
}