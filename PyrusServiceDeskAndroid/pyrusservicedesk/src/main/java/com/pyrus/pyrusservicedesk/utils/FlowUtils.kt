package com.pyrus.pyrusservicedesk.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> singleFlow(block: suspend () -> T): Flow<T> {
    return flow { emit(block()) }
}