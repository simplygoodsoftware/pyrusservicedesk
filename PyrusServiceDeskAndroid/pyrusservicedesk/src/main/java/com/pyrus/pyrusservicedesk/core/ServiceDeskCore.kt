package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import com.google.gson.GsonBuilder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeature
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.FileResolverImpl
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.gson.RemoteGsonExclusionStrategy
import com.pyrus.pyrusservicedesk.sdk.data.gson.UriGsonAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifierImpl
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.ISO_DATE_PATTERN
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getBaseUrl
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory2
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory2
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class ServiceDeskCore(
    internal val diInjector: DiInjector,
)

internal sealed interface Account {

    val instanceId: String
    val appId: String
    val domain: String

    data class V1(
        override val instanceId: String,
        override val appId: String,
        override val domain: String,
    ) : Account

    data class V2(
        override val instanceId: String,
        override val appId: String,
        override val domain: String,
        val userId: String,
        val securityKey: String,
    ): Account

}

internal class DiInjector(
    private val application: Application,
    private val account: Account,
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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            authToken?.let { authToken ->
                requestBuilder.header("Authorization", authToken)
            }

            val userAgent = "ServicedeskClient/android/" +
                Build.MANUFACTURER + "/" +
                Build.MODEL + "/" +
                Build.VERSION.SDK_INT

            requestBuilder.header("User-Agent", userAgent)
            chain.proceed(requestBuilder.build())
        }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(getBaseUrl(account.domain))
        .addConverterFactory(GsonConverterFactory.create(remoteGson))
        .client(okHttpClient)
        .build()

    private val api: ServiceDeskApi = retrofit.create(ServiceDeskApi::class.java)

    private val fileManager: FileManager = FileManager(application, fileResolver)

    private val remoteStore: RemoteStore = RemoteStore(
        instanceId = ConfigUtils.getInstanceId(preferences),
        fileResolver = fileResolver,
        fileManager = fileManager,
        api = api,
        account = account
    )

    private val repository: Repository = Repository(localStore, remoteStore)

    private val preferencesManager = PreferencesManager(preferences)

    private val storeFactory: StoreFactory2 = DefaultStoreFactory2()

    fun ticketFeatureFactory(welcomeMessage: String): TicketFeatureFactory {
        return TicketFeatureFactory(storeFactory, repository, welcomeMessage)
    }

    val liveUpdates = LiveUpdates(
        repository = repository,
        preferencesManager = preferencesManager,
        userId = account.instanceId,
        coreScope = coreScope,
    )

    val picasso = Picasso.Builder(application)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()

    val setPushTokenUseCase = SetPushTokenUseCase(account, coreScope, preferencesManager)

}