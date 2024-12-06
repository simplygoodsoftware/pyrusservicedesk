package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.FileChooser
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration

internal object StaticRepository {

    var logging: Boolean = false

    var EXTRA_FIELDS: Map<String, String>? = null

    var FILE_CHOOSER: FileChooser? = null

    private var CONFIGURATION: ServiceDeskConfiguration = ServiceDeskConfiguration()

    fun getConfiguration(): ServiceDeskConfiguration {
        return CONFIGURATION
    }

    fun setConfiguration(configuration: ServiceDeskConfiguration?) {
        CONFIGURATION = configuration ?: ServiceDeskConfiguration()
    }


}