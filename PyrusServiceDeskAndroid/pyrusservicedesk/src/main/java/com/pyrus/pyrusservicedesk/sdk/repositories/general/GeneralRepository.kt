package com.pyrus.pyrusservicedesk.sdk.repositories.general

import com.pyrus.pyrusservicedesk.sdk.repositories.offline.OfflineRepository

/**
 * Interface for the objects that are responsible for handling general user requests requests.
 * TODO the better approach is to split it to separate repos. At least [setPushToken] is not general use case.
 */
internal interface GeneralRepository : RemoteRepository, OfflineRepository