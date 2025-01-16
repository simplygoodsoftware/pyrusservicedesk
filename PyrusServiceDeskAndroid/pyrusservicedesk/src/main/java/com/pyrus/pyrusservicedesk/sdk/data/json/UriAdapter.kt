package com.pyrus.pyrusservicedesk.sdk.data.json

import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class UriAdapter : JsonAdapter<Uri>() {

    @FromJson
    override fun fromJson(reader: JsonReader): Uri? {
        return Uri.parse(reader.nextString())
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Uri?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toString())
        }
    }
}