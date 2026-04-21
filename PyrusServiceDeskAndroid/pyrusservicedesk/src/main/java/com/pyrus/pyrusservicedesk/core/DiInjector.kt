package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk.BuildConfig
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk._ref.helpers.ThreadsHelper
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessDeniedFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.rate_time.TimeToRateUseCase
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record.AudioRecordControllerFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFeatureFactory
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getBaseUrl
import com.pyrus.pyrusservicedesk._ref.utils.call_adapter.TryCallAdapterFactory
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouterImpl
import com.pyrus.pyrusservicedesk._ref.whitetea.core.DefaultStoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeatureFactory
import com.pyrus.pyrusservicedesk.core.refresh.RefreshUseCase
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
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
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit


internal class DiInjector(
    application: Application,
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

    private val cicerone: Cicerone<PyrusRouterImpl> = Cicerone.create(PyrusRouterImpl())

    private val addUserEventBus = AddUserEventBus()

    val router = cicerone.router

    val navHolder: NavigatorHolder = cicerone.getNavigatorHolder()

    private val player: ExoPlayer by lazy {
        ExoPlayer
            .Builder(application)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()
    }
    private val mediaSessionManager = MediaSessionManager()

    private val session: MediaSession by lazy {
        mediaSessionManager.createMediaSessionWithRetry(application, player)
    }

    private val downloadHelper = DownloadHelper(
        context = application
    )

    val audioWrapper = AudioWrapper(session, downloadHelper, coreScope)

    private val audioRecordControllerFactory = AudioRecordControllerFactory(application.cacheDir)

    val ticketFeatureFactory = TicketFeatureFactory(
        accountStore = accountStore,
        storeFactory = storeFactory,
        repository = repository,
        draftRepository = draftRepository,
        router = router,
        fileManager = fileManager,
        preferencesManager = preferencesManager,
        audioRecordControllerFactory = audioRecordControllerFactory,
        audioWrapper = audioWrapper,
        localTicketsStore = localTicketsStore,
        commandsStore = localCommandsStore,
        systemMessageStore = systemMessageStore,
        idStore = idStore,
    )

    val ticketsFeatureFactory = TicketsFeatureFactory(
        storeFactory = storeFactory,
        repository = repository,
        router = router,
        commandsStore = localCommandsStore,
        addUserEventBus = addUserEventBus,
        audioWrapper = audioWrapper,
        accountStore = accountStore,
    )

    val searchFeatureFactory = SearchFeatureFactory(
        storeFactory = storeFactory,
        repository = repository,
        router = router,
        accountStore = accountStore,
    )

    val autoRefreshFeatureFactory = AutoRefreshFeatureFactory(
        storeFactory = storeFactory,
        repository = repository,
        preferencesManager = preferencesManager,
        systemMessageStore = systemMessageStore,
        localTicketsStore = localTicketsStore
    )

    val sharedViewModel = SharedViewModel()


    val picassoManager: PicassoManager = PicassoManager(application)

    val picasso: Picasso = picassoManager.providePicasso(createOkHttpClientBuilder())

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

    val accessDeniedFeatureFactory = AccessDeniedFeatureFactory(
        storeFactory = storeFactory,
        accountStore = accountStore,
        accessDeniedEventBus = accessDeniedEventBus,
        ticketsStore = localTicketsStore,
        finishEventBus = finishEventBus,
        preferencesManager = preferencesManager,
    )

    fun onCancel() {
        releaseSession()
        picassoManager.dispose(picasso)
        coreScope.cancel()
        synchronizer.cancel()
        synchronizer.close()
    }

    fun releaseSession() = ThreadsHelper().syncRunOnMainThread {
        session.run {
            player.release()
            release()
        }
    }

    fun stopSession() {
        coreScope.launch(Dispatchers.Main) {
            audioWrapper.clearCurrentUrl()
            session.player.clearMediaItems()
        }
    }

}