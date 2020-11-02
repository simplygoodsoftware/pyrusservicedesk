package com.pyrus.pyrusservicedesk.utils

internal fun String.getFirstNSymbols(n: Int): String {
    return if (length < n)
        this
    else
        substring(0, n)
}