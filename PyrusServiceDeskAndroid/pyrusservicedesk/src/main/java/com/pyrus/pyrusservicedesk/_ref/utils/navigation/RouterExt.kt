package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.ResultListenerHandler
import com.github.terrakok.cicerone.Screen
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


suspend inline fun <reified T> PyrusRouter.navigateForResult(screen: Screen, key: String): T {
    navigateTo(screen)
    return getResult(key)
}

suspend inline fun <reified T> PyrusRouter.getResult(key: String): T {
    return suspendCancellableCoroutine { continuation ->
        val resultHandler: ResultListenerHandler = setResultListener(key) { token ->
            if (token is T) {
                continuation.resume(token)
            }
        }
        continuation.invokeOnCancellation {
            resultHandler.dispose()
        }
    }
}