package com.pyrus.pyrusservicedesk

import android.app.Application
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier

/**
 * Dependency provider to get rid of [PyrusServiceDesk] object in presentation
 */
@MainThread
internal interface ServiceDeskProvider {
    /**
     * Provides [Application]
     */
    fun getApplication(): Application

    /**
     * Provides [RequestFactory]
     */
    fun getRequestFactory(): RequestFactory

    /**
     * Provides [DraftRepository]
     */
    fun getDraftRepository(): DraftRepository

    /**
     * Provides [LiveUpdates]
     */
    fun getLiveUpdates(): LiveUpdates

    /**
     * Provides [LocalDataProvider]
     */
    fun getLocalDataProvider(): LocalDataProvider

    fun getCopypaster(): FileManager

    /**
     * Provides [LocalDataVerifier]
     */
    fun getLocalDataVerifier(): LocalDataVerifier
}