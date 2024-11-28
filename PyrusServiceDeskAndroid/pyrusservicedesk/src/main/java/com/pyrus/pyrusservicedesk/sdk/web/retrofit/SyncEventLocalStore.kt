package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk.sdk.sync.SyncEvent
import kotlinx.coroutines.flow.Flow

interface SyncEventLocalStore {
    fun addSyncEvent(se: SyncEvent?)

    val syncEvents: Array<Any?>?

    val syncEventFlow: Flow<ArrayList<Any?>?>?

    fun getSyncEvent(eventId: String?): SyncEvent?


    fun updateSyncEvent(se: SyncEvent?)
    fun updateSyncEvents(se: List<SyncEvent?>?)


    //fun onSyncEventHandled(ser: GeneralParseResult.SyncEventResult?)

    fun hasSyncEvents(): Boolean

    fun hasFilesForUpload(): Boolean

    val filesForUpload: HashMap<Any?, Any?>?
}
