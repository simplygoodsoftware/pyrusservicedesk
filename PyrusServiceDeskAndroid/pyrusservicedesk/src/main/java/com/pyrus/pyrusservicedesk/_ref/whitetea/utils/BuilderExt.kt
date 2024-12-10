package com.pyrus.pyrusservicedesk._ref.whitetea.utils

import com.pyrus.pyrusservicedesk._ref.whitetea.core.UpdateBuilder


internal operator fun <State : Any, Message : Any, Effect : Any> UpdateBuilder<State, Message, Effect>.plus(
    effect: Effect,
): UpdateBuilder<State, Message, Effect> {
    effects { +effect }
    return this
}