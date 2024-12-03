package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.GsonBuilder
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.FileResolverImpl
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.gson.RemoteGsonExclusionStrategy
import com.pyrus.pyrusservicedesk.sdk.data.gson.UriGsonAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifierImpl
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.pyrus.pyrusservicedesk.utils.ISO_DATE_PATTERN
import com.pyrus.pyrusservicedesk.utils.PREFERENCE_KEY
import kotlinx.coroutines.CoroutineScope
import retrofit2.Retrofit

internal class ServiceDeskCore(
    internal val depsInjection: DepsInjection,
)

internal class DepsInjection(
    internal val application: Application,
    private val appId: String,
    private val userId: String?,
    private val securityKey: String?,
    private val domain: String?,
    private val apiVersion: Int,
    private val loggingEnabled: Boolean,
    private val authToken: String?,
    private val coreScope: CoroutineScope,
) {

    private val preferences: SharedPreferences = application
        .getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)

    private val fileResolver: FileResolver = FileResolverImpl(application.contentResolver)

    private val localDataVerifier: LocalDataVerifier = LocalDataVerifierImpl(fileResolver)

    private val offlineGson = GsonBuilder()
        .setDateFormat(ISO_DATE_PATTERN)
        .registerTypeAdapter(Uri::class.java, UriGsonAdapter())
        .create()

    private val remoteGson = GsonBuilder()
        .setDateFormat(ISO_DATE_PATTERN)
        .addSerializationExclusionStrategy(RemoteGsonExclusionStrategy())
        .create()

    private val localStore: LocalStore = LocalStore(preferences, localDataVerifier, offlineGson)

    private val retrofit: Retrofit = TODO()

    private val api: ServiceDeskApi = retrofit.create(ServiceDeskApi::class.java)

    private val fileManager: FileManager = FileManager(application, fileResolver)

    private val remoteStore: RemoteStore = RemoteStore(
        appId = appId,
        instanceId = null,
        fileResolver = fileResolver,
        fileManager = fileManager,
        api = api,
    )

    private val repository: Repository = Repository(localStore, remoteStore)



}