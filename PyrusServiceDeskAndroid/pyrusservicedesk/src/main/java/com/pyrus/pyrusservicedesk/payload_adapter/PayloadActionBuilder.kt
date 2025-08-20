package com.pyrus.pyrusservicedesk.payload_adapter

import kotlin.reflect.KProperty0

class PayloadActionBuilder<Entry>(
    val entry: Entry,
    private val payload: Set<String>?
) {

    val firstItemBind = payload == null

    private val updateActions = ArrayList<UpdateAction<*>>()

    fun <T> KProperty0<T>.payloadCheck(consumer: () -> Unit) {
        updateActions += UpdateAction(this, consumer)
    }

    fun start() {
        for (action in updateActions) {
            if (hasChanges(action.property, payload)) {
                start(action)
            }
        }
    }

    private fun <T> start(action: UpdateAction<T>) {
        action.consumer()
    }

    private fun hasChanges(property: KProperty0<*>, payload: Set<String>?): Boolean {
        return payload == null || payload.contains(property.name)
    }

}

private class UpdateAction<T>(
    val property: KProperty0<T>,
    val consumer: () -> Unit
)