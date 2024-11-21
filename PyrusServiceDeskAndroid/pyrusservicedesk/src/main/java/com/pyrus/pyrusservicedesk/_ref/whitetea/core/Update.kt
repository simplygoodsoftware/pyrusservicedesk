package com.pyrus.pyrusservicedesk._ref.whitetea.core

internal data class Update<State : Any, Effect : Any>(
    val state: State,
    val effects: List<Effect> = emptyList(),
)