package com.pyrus.pyrusservicedesk._ref.whitetea.core

import androidx.annotation.MainThread

/**
 * Represents a consumer of the `View State`
 *
 */
internal interface ViewRenderer<Model : Any> {

    /**
     * Renders (displays) the provided `View State`
     *
     * @param model a `View State` to be rendered (displayed)
     */
    @MainThread
    fun render(model: Model)

}