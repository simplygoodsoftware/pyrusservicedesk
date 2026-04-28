package com.pyrus.pyrusservicedesk.core

import android.app.Application
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk._ref.helpers.ThreadsHelper
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessDeniedFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketFeatureFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record.AudioRecordControllerFactory
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFeatureFactory
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouterImpl
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.FinishEventBus
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * UI-only dependency subgraph.
 *
 * Created on the main thread when the SDK UI is opened ([PyrusServiceDesk.ensureUiInjector] from
 * `MainActivity.onCreate`) and closed when the activity is actually finishing.
 *
 * Heavy / main-thread-bound resources (Picasso, ExoPlayer, MediaSession) are kept lazy so
 * `UiInjector` construction itself is cheap and never blocks main with `Thread.sleep` from
 * MediaSession retries — they're created only on first real use.
 */
internal class UiInjector(
    private val application: Application,
    private val coreScope: CoroutineScope,
    private val okHttpClientProvider: () -> okhttp3.OkHttpClient,
    private val accountStore: AccountStore,
    private val preferencesManager: PreferencesManager,
    private val idStore: IdStore,
    private val localCommandsStore: LocalCommandsStore,
    private val localTicketsStore: LocalTicketsStore,
    private val systemMessageStore: SystemMessageStore,
    private val repository: SdRepository,
    private val draftRepository: DraftRepository,
    private val fileManager: FileManager,
    private val addUserEventBus: AddUserEventBus,
    private val storeFactory: StoreFactory,
    private val accessDeniedEventBus: AccessDeniedEventBus,
    private val finishEventBus: FinishEventBus,
) {

    init {
        ensureMainThread("UiInjector")
    }

    val picassoManager: PicassoManager = PicassoManager(application)

    private var picassoCreated = false
    val picasso: Picasso by lazy(LazyThreadSafetyMode.NONE) {
        ensureMainThread("Picasso")
        picassoCreated = true
        picassoManager.providePicasso(okHttpClientProvider().newBuilder())
    }

    private val cicerone: Cicerone<PyrusRouterImpl> = Cicerone.create(PyrusRouterImpl())

    val router: PyrusRouterImpl = cicerone.router
    val navHolder: NavigatorHolder = cicerone.getNavigatorHolder()

    val sharedViewModel: SharedViewModel = SharedViewModel()

    private val downloadHelper: DownloadHelper = DownloadHelper(context = application)

    private val audioRecordControllerFactory: AudioRecordControllerFactory =
        AudioRecordControllerFactory(application.cacheDir)

    private val mediaSessionManager = MediaSessionManager()

    private var playerCreated = false
    private val player: ExoPlayer by lazy(LazyThreadSafetyMode.NONE) {
        ensureMainThread("ExoPlayer")
        playerCreated = true
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

    private var sessionCreated = false
    private val session: MediaSession by lazy(LazyThreadSafetyMode.NONE) {
        ensureMainThread("MediaSession")
        sessionCreated = true
        mediaSessionManager.createMediaSessionWithRetry(application, player)
    }

    private var audioWrapperCreated = false
    val audioWrapper: AudioWrapper by lazy(LazyThreadSafetyMode.NONE) {
        ensureMainThread("AudioWrapper")
        audioWrapperCreated = true
        AudioWrapper(session, downloadHelper, coreScope)
    }

    // Factories themselves are lightweight, but TicketFeatureFactory / TicketsFeatureFactory
    // capture `audioWrapper`. Building them eagerly would force the audio stack lazy chain
    // (audioWrapper -> session -> MediaSession with Thread.sleep retry) to fire during
    // UiInjector construction, defeating the laziness. Keep the factories lazy too.
    val ticketFeatureFactory: TicketFeatureFactory by lazy(LazyThreadSafetyMode.NONE) {
        TicketFeatureFactory(
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
    }

    val ticketsFeatureFactory: TicketsFeatureFactory by lazy(LazyThreadSafetyMode.NONE) {
        TicketsFeatureFactory(
            storeFactory = storeFactory,
            repository = repository,
            router = router,
            commandsStore = localCommandsStore,
            addUserEventBus = addUserEventBus,
            audioWrapper = audioWrapper,
            accountStore = accountStore,
        )
    }

    val searchFeatureFactory: SearchFeatureFactory by lazy(LazyThreadSafetyMode.NONE) {
        SearchFeatureFactory(
            storeFactory = storeFactory,
            repository = repository,
            router = router,
            accountStore = accountStore,
        )
    }

    val accessDeniedFeatureFactory: AccessDeniedFeatureFactory by lazy(LazyThreadSafetyMode.NONE) {
        AccessDeniedFeatureFactory(
            storeFactory = storeFactory,
            accountStore = accountStore,
            accessDeniedEventBus = accessDeniedEventBus,
            ticketsStore = localTicketsStore,
            finishEventBus = finishEventBus,
            preferencesManager = preferencesManager,
        )
    }

    fun stopSession() {
        if (!audioWrapperCreated) return
        coreScope.launch(Dispatchers.Main) {
            audioWrapper.clearCurrentUrl()
            session.player.clearMediaItems()
        }
    }

    fun close() {
        ThreadsHelper().syncRunOnMainThread {
            if (sessionCreated) {
                runCatching {
                    session.run {
                        player.release()
                        release()
                    }
                }
            } else if (playerCreated) {
                runCatching { player.release() }
            }

            if (picassoCreated) {
                runCatching { picassoManager.dispose(picasso) }
            }
        }
    }

    private fun ensureMainThread(name: String) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "$name must be used on the main thread, was: ${Thread.currentThread().name}"
        }
    }
}
