package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.google.gson.GsonBuilder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFeatureFactory
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.ISO_DATE_PATTERN
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getBaseUrl
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.TryCallAdapterFactory
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouterImpl
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.gson.RemoteGsonExclusionStrategy
import com.pyrus.pyrusservicedesk.sdk.data.gson.UriGsonAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifierImpl
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class DiInjector(
    private val application: Application,
    private val account: Account,
    private val loggingEnabled: Boolean,
    private val authToken: String?,
    private val coreScope: CoroutineScope,
    private val preferences: SharedPreferences,
) {

    private val accountStore = AccountStore(account)

    private val fileResolver: FileResolver = FileResolver(application.contentResolver)

    private val localDataVerifier: LocalDataVerifier = LocalDataVerifierImpl(fileResolver)

    private val offlineGson = GsonBuilder()
        .setDateFormat(ISO_DATE_PATTERN)
        .registerTypeAdapter(Uri::class.java, UriGsonAdapter())
        .create()

    private val remoteGson = GsonBuilder()
        .setDateFormat(ISO_DATE_PATTERN)
        .addSerializationExclusionStrategy(RemoteGsonExclusionStrategy())
        .create()

    val localCommandsStore: LocalCommandsStore = LocalCommandsStore(preferences, localDataVerifier, offlineGson) //TODO обязательно приватный?

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
        .addCallAdapterFactory(TryCallAdapterFactory())
        .client(okHttpClient)
        .build()

    private val api: ServiceDeskApi = retrofit.create(ServiceDeskApi::class.java)

    private val fileManager: FileManager = FileManager(application, fileResolver)

    private val repositoryMapper = RepositoryMapper(account)

    private val remoteFileStore = RemoteFileStore(api) // TODO use different api

    private val localTicketsStore = com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore()

    private val synchronizer = Synchronizer(
        api = api,
        localTicketsStore = localTicketsStore,
        accountStore = accountStore
    )

    private val repository: Repository = Repository(
        localCommandsStore = localCommandsStore,
        repositoryMapper = repositoryMapper,
        fileResolver = fileResolver,
        remoteFileStore = remoteFileStore,
        synchronizer = synchronizer,
        localTicketsStore = localTicketsStore
    )

    private val preferencesManager = PreferencesManager(preferences)

    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private val draftRepository = DraftRepository(preferences)

    private val cicerone: Cicerone<PyrusRouterImpl> = Cicerone.create(PyrusRouterImpl())

    val router = cicerone.router

    val navHolder: NavigatorHolder = cicerone.getNavigatorHolder()

    val ticketFeatureFactory = TicketFeatureFactory(
        account = account,
        storeFactory = storeFactory,
        repository = repository,
        draftRepository = draftRepository,
        router = router,
        fileManager = fileManager,
    )

    // TODO fix it
    val ticketsFeatureFactory = TicketsFeatureFactory(account as Account.V3, storeFactory, repository, router)

    var intentQr: Intent? = null
    var intentSettings: Intent? = null

    fun setIntent(openQrIntent: Intent?, openSettingsIntent: Intent?) {
        intentQr = openQrIntent
        intentSettings = openSettingsIntent
    }
    val isMultiChat = account is Account.V3

    val sharedViewModel = SharedViewModel()


    val liveUpdates = LiveUpdates(
        repository = repository,
        preferencesManager = preferencesManager,
        // TODO sds fix live updates
        userId = account.getUserId(),
        coreScope = coreScope,
    )

    val picasso = Picasso.Builder(application)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()

    val setPushTokenUseCase = SetPushTokenUseCase(account, coreScope, preferencesManager)

    // TODO remove it
    val usersAccount = (account as? Account.V3)

}