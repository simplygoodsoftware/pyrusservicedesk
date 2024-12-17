package com.pyrus.pyrusservicedesk._ref.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> singleFlow(block: suspend () -> T): Flow<T> {
    return flow { emit(block()) }
}