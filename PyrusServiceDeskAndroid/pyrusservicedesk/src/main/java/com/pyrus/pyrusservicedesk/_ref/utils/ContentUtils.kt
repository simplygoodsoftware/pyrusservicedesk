package com.pyrus.pyrusservicedesk._ref.utils

import android.net.Uri

private const val URI_SCHEME_HTTPS = "https"

internal fun Uri.isRemote() = scheme == URI_SCHEME_HTTPS