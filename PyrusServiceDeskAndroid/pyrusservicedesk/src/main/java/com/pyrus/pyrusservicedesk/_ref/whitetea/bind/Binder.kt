package com.pyrus.pyrusservicedesk._ref.whitetea.bind

/**
 * Controls bindings connecting and disconnecting them in lifecycle callbacks
 */
internal interface Binder {

    /**
     * Connects all the managed bindings
     */
    fun start()

    /**
     * Disconnects all the managed bindings
     */
    fun stop()
}