package com.pyrus.pyrusservicedesk.payload_adapter

inline fun <Entry> PayloadActionBuilder<Entry>.diff(updateAction: PayloadActionBuilder<Entry>.() -> Unit) {
    updateAction()
}