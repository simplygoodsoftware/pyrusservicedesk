package com.pyrus.pyrusservicedesk.sdk.data.gson

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * [Gson] adapter for serializing - deserializing android's [Uri] class into Json
 */
internal class UriGsonAdapter : JsonDeserializer<Uri>, JsonSerializer<Uri> {

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Uri {
        return Uri.parse(json.asString)
    }

    override fun serialize(src: Uri?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }
}