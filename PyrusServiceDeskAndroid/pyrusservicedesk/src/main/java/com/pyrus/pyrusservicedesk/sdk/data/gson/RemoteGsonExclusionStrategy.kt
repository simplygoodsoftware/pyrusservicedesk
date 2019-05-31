package com.pyrus.pyrusservicedesk.sdk.data.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

/**
 * Exclusion strategy that is used for remote calls.
 */
internal class RemoteGsonExclusionStrategy: ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.getAnnotation(Local::class.java) != null
    }
}