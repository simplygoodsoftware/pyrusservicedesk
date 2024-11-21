package com.pyrus.pyrusservicedesk._ref.whitetea.core

import kotlinx.coroutines.flow.Flow

internal interface ViewMessages<Message : Any> {

    val messages: Flow<Message>

}