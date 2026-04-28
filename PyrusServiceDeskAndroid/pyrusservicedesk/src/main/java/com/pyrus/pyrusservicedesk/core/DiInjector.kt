package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk.BuildConfig
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk._ref.ui_domain.rate_time.TimeToRateUseCase
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getBaseUrl
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.TryCallAdapterFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeatureFactory
import com.pyrus.pyrusservicedesk.core.refresh.RefreshUseCase
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.FinishEventBus
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.json.DateAdapter
import com.pyrus.pyrusservicedesk.sdk.data.json.UriAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RemoteFileStore
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Core (non-UI) dependency graph. Created in `PyrusServiceDesk.init(...)` and lives until the
 * next `init(...)` call or until `stop()`/`onCancel()`.
 *
 * This class is intentionally free of UI-only dependencies (Picasso, ExoPlayer/MediaSession,
 * Cicerone, feature factories, etc.). Those live in [UiInjector] and are owned/lifecycled by
 * `PyrusServiceDesk` companion together with `MainActivity`. This lets the host application call
 * `init()` from any thread without the SDK accidentally creating UI-thread-bound resources off
 * the main looper.
 */
internal class DiInjector(
    private val application: Application,
    initialAccount: Account,
    private val authToken: String?,
    private val coreScope: CoroutineScope,
    preferences: SharedPreferences,
) {

    val accountStore = AccountStore(initialAccount)

    private val fileResolver: FileResolver = FileResolver(application.contentResolver, application)

    private val localDataVerifier: LocalDataVerifier = LocalDataVerifier(fileResolver)

    private val moshi = Moshi.Builder()
        .add(CommandParamsDto.factory)
        .add(DateAdapter())
        .add(UriAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createOkHttpClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                authToken?.let { authToken ->
                    requestBuilder.header("Authorization", authToken)
                }

                val isMultiChat = if (accountStore.getAccount().isMultiChat()) 1 else 0
                val userAgent = "ServiceDesk/Android/" +
                    BuildConfig.VERSION_NAME + "/" +
                    accountStore.getAccount().getAppId()?.take(10) + "/" +
                    Build.VERSION.SDK_INT + "/" +
                    isMultiChat

                requestBuilder.header("User-Agent", userAgent)
                chain.proceed(requestBuilder.build())
            }
    }

    private val okHttpClient = createOkHttpClientBuilder().build()

    // TODO sds сделать мультидоменный retrofit
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(getBaseUrl(initialAccount.domain))
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(TryCallAdapterFactory())
        .client(okHttpClient)
        .build()

    private val idStore = IdStore()


    val systemMessageStore = SystemMessageStore(idStore)

    private val db = SdDatabase.create(application)

    private val ticketsDao = db.ticketsDao()

    private val commandsDao = db.commandsDao()

    private val searchDao = db.searchDao()

    val localCommandsStore: LocalCommandsStore = LocalCommandsStore(
        idStore = idStore,
        commandsDao = commandsDao,
        searchDao = searchDao
    )

    private val api: ServiceDeskApi = retrofit.create(ServiceDeskApi::class.java)

    private val fileManager: FileManager = FileManager(application, fileResolver)

    private val repositoryMapper = RepositoryMapper(idStore)

    private val remoteFileStore = RemoteFileStore(api)

    val localTicketsStore = LocalTicketsStore(idStore, ticketsDao, searchDao, accountStore)

    private val resourceManager = AppResourceManager(application)

    private val accessDeniedEventBus = AccessDeniedEventBus()

    val finishEventBus = FinishEventBus()

    private val initialAccountKey = when(initialAccount) {
        is Account.V1 -> initialAccount.appId
        is Account.V2 -> initialAccount.appId
        is Account.V3 -> initialAccount.authorId
    }

    val preferencesManager = PreferencesManager(initialAccountKey, preferences)

    private val synchronizer = Synchronizer(
        api = api,
        localTicketsStore = localTicketsStore,
        accountStore = accountStore,
        resourceManager = resourceManager,
        idStore = idStore,
        commandsStore = localCommandsStore,
        accessDeniedEventBus = accessDeniedEventBus,
        preferences = preferencesManager,
        systemMessageStore = systemMessageStore,
    )

    val repository: SdRepository = SdRepository(
        commandsStore = localCommandsStore,
        repositoryMapper = repositoryMapper,
        fileResolver = fileResolver,
        remoteFileStore = remoteFileStore,
        synchronizer = synchronizer,
        ticketsStore = localTicketsStore,
        coroutineScope = coreScope,
        accountStore = accountStore,
        idStore = idStore,
        systemMessageStore = systemMessageStore,
    )

    private val storeFactory: StoreFactory = DefaultStoreFactory()

    private val draftRepository = DraftRepository(preferences, idStore, moshi)

    private val addUserEventBus = AddUserEventBus()

    private val downloadHelper = DownloadHelper(
        context = application
    )

    val autoRefreshFeatureFactory = AutoRefreshFeatureFactory(
        storeFactory = storeFactory,
        repository = repository,
        preferencesManager = preferencesManager,
        systemMessageStore = systemMessageStore,
        localTicketsStore = localTicketsStore
    )

    val setPushTokenUseCase = SetPushTokenUseCase(accountStore, coreScope, preferencesManager, repository)

    val cleanDataUseCase = CleanDataUseCase(
        coreScope = coreScope,
        sdDatabase = db,
        fileManager = fileManager,
        downloadHelper = downloadHelper,
        draftRepository = draftRepository,
    )

    val addUserUseCase = AddUserUseCase(accountStore, repository, coreScope, addUserEventBus)

    val refreshUseCase = RefreshUseCase(repository, coreScope)

    val updateUserUseCase = UpdateUserUseCase(accountStore, preferencesManager)

    val rateTimeUseCase = TimeToRateUseCase(preferencesManager)

    /**
     * Factory for the UI subgraph. Must be invoked on the main thread (enforced by [UiInjector]'s
     * own init block). The returned [UiInjector] holds Picasso/ExoPlayer/MediaSession/Cicerone
     * and other UI-thread-bound resources, and must be `close()`-d when the SDK UI is torn down.
     */
    fun createUiInjector(): UiInjector = UiInjector(
        application = application,
        coreScope = coreScope,
        okHttpClientProvider = { okHttpClient },
        accountStore = accountStore,
        preferencesManager = preferencesManager,
        idStore = idStore,
        localCommandsStore = localCommandsStore,
        localTicketsStore = localTicketsStore,
        systemMessageStore = systemMessageStore,
        repository = repository,
        draftRepository = draftRepository,
        fileManager = fileManager,
        addUserEventBus = addUserEventBus,
        storeFactory = storeFactory,
        accessDeniedEventBus = accessDeniedEventBus,
        finishEventBus = finishEventBus,
    )

    fun onCancel() {
        coreScope.cancel()
        synchronizer.cancel()
        synchronizer.close()
        shutdownOkHttpClient(okHttpClient)
    }

    private fun shutdownOkHttpClient(client: OkHttpClient) {
        runCatching { client.dispatcher().executorService().shutdown() }
        runCatching { client.connectionPool().evictAll() }
        runCatching { client.cache()?.close() }
    }

}
