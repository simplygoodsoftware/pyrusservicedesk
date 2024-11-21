package com.pyrus.pyrusservicedesk._ref.whitetea.core

import androidx.annotation.MainThread

/**
 * Represents a consumer of the `View Effect`
 *
 */
internal interface EffectHandler<Effect : Any> {

    @MainThread
    fun handleEffect(effect: Effect)

}