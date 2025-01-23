package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFeatureFactory
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getBaseUrl
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.TryCallAdapterFactory
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouterImpl
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.json.DateAdapter
import com.pyrus.pyrusservicedesk.sdk.data.json.UriAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

internal class DiInjector(
    application: Application,
    initialAccount: Account,
    private val authToken: String?,
    coreScope: CoroutineScope,
    preferences: SharedPreferences,
) {

    val accountStore = AccountStore(initialAccount)

    private val fileResolver: FileResolver = FileResolver(application.contentResolver)

    private val localDataVerifier: LocalDataVerifier = LocalDataVerifier(fileResolver)

    private val moshi = Moshi.Builder()
        .add(CommandParamsDto.factory)
        .add(DateAdapter())
        .add(UriAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private val idStore = IdStore()

    val localCommandsStore: LocalCommandsStore = LocalCommandsStore(
        preferences = preferences,
        localDataVerifier = localDataVerifier,
        idStore = idStore,
        moshi = moshi,
    )

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

    // TODO sds сделать мультидоменный retrofit
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(getBaseUrl(initialAccount.domain))
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(TryCallAdapterFactory())
        .client(okHttpClient)
        .build()

    private val api: ServiceDeskApi = retrofit.create(ServiceDeskApi::class.java)

    private val fileManager: FileManager = FileManager(application, fileResolver)

    private val repositoryMapper = RepositoryMapper(idStore)

    private val remoteFileStore = RemoteFileStore(api)

    private val localTicketsStore = LocalTicketsStore(idStore)

    private val resourceManager = AppResourceManager(application)

    private val synchronizer = Synchronizer(
        api = api,
        localTicketsStore = localTicketsStore,
        accountStore = accountStore,
        resourceManager = resourceManager,
        idStore = idStore,
    )

    private val repository: Repository = Repository(
        commandsStore = localCommandsStore,
        repositoryMapper = repositoryMapper,
        fileResolver = fileResolver,
        remoteFileStore = remoteFileStore,
        synchronizer = synchronizer,
        ticketsStore = localTicketsStore,
        coroutineScope = coreScope,
        accountStore = accountStore,
        idStore = idStore,
    )

    private val preferencesManager = PreferencesManager(preferences)

    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private val draftRepository = DraftRepository(preferences, idStore, moshi)

    private val cicerone: Cicerone<PyrusRouterImpl> = Cicerone.create(PyrusRouterImpl())

    val router = cicerone.router

    val navHolder: NavigatorHolder = cicerone.getNavigatorHolder()

    val ticketFeatureFactory = TicketFeatureFactory(
        accountStore = accountStore,
        storeFactory = storeFactory,
        repository = repository,
        draftRepository = draftRepository,
        router = router,
        fileManager = fileManager,
    )

    val ticketsFeatureFactory = TicketsFeatureFactory(
        storeFactory = storeFactory,
        repository = repository,
        router = router,
        commandsStore = localCommandsStore,
    )

    val sharedViewModel = SharedViewModel()

    val liveUpdates = LiveUpdates(
        repository = repository,
        preferencesManager = preferencesManager,
        // TODO sds fix live updates
        userId = initialAccount.getUserId(),
        coreScope = coreScope,
    )

    val picasso = Picasso.Builder(application)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()

    val setPushTokenUseCase = SetPushTokenUseCase(accountStore, coreScope, preferencesManager)

    val addUserUseCase = AddUserUseCase(accountStore, repository, coreScope)

}