package com.pyrus.pyrusservicedesk.sdk

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.pyrus.pyrusservicedesk._ref.data.RatingTextValues

class Converter {
    @TypeConverter
    fun listRatingTextValuesToJson(value: List<RatingTextValues>): String = Gson().toJson(value).toString()

    @TypeConverter
    fun jsonToListRatingTextValues(value: String): List<RatingTextValues> = Gson().fromJson(value, Array<RatingTextValues>::class.java).toList()
}