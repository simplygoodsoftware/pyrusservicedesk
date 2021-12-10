package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import com.google.gson.GsonBuilder
import com.pyrus.pyrusservicedesk.log.PLog
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.FileResolverImpl
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.data.gson.RemoteGsonExclusionStrategy
import com.pyrus.pyrusservicedesk.sdk.data.gson.UriGsonAdapter
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.PreferenceDraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.general.CentralRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.offline.OfflineRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.offline.PreferenceOfflineRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifierImpl
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.RetrofitWebRepository
import com.pyrus.pyrusservicedesk.utils.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.Executors

class PyrusServiceDesk private constructor(
    internal val application: Application,
    internal val appId: String,
    internal val userId: String?,
    internal val securityKey: String?,
    internal val domain: String?,
    internal val apiVersion: Int,
    loggingEnabled: Boolean
) {

    companion object {

        private val TAG = PyrusServiceDesk::class.java.simpleName

        internal val DISPATCHER_IO_SINGLE =
            Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        internal var FILE_CHOOSER: FileChooser? = null
        internal var onAuthorizationFailed: Runnable? = Runnable {
            stop()
        }
        private var INSTANCE: PyrusServiceDesk? = null
        private var CONFIGURATION: ServiceDeskConfiguration? = null
        private var lastRefreshes = ArrayList<Long>()

        private const val SET_PUSH_TOKEN_TIMEOUT = 5 // Minutes
        private const val REFRESH_MAX_COUNT = 20 // in minute

        internal const val API_VERSION_1: Int = 0
        internal const val API_VERSION_2: Int = 2

        internal var logging = false
            private set

        /**
         * Initializes PyrusServiceDesk embeddable module.
         * The best approach is to call this in [Application.onCreate]
         * ***PS***: Should be done before other public methods are is called.
         * Unhandled IllegalStateException is thrown otherwise.
         *
         * @param application instance of the enclosing application
         * @param appId id of a client
         * @param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
         * @param loggingEnabled If true, then the library will write logs,
         * and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            domain: String? = null,
            loggingEnabled: Boolean = false
        ) {
            initInternal(application, appId, null, null, domain,  API_VERSION_1, loggingEnabled)
        }

        /**
         * Initializes PyrusServiceDesk embeddable module.
         * This init is used for authorized users with device independent sessions.
         * The best approach is to call this in [Application.onCreate]
         * ***PS***: Should be done before other public methods are is called.
         * Unhandled IllegalStateException is thrown otherwise.
         *
         * @param application instance of the enclosing application
         * @param appId id of a client
         * @param userId of the user who is initializing service desk
         * @param securityKey of the user far safe initialization
         * @param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
         * @param loggingEnabled If true, then the library will write logs,
         * and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            userId: String,
            securityKey: String,
            domain: String? = null,
            loggingEnabled: Boolean = false
        ) {
            initInternal(application, appId, userId, securityKey, domain, API_VERSION_2, loggingEnabled)
        }

        private fun initInternal(
            application: Application,
            appId: String,
            userId: String?,
            securityKey: String?,
            domain: String? = null,
            apiVersion: Int = API_VERSION_1,
            loggingEnabled: Boolean
        ) {
            PLog.d(TAG, "initInternal, appId: ${appId.getFirstNSymbols(10)}, userId: ${userId?.getFirstNSymbols(10)}, apiVersion: $apiVersion")
            if (INSTANCE != null && get().userId != userId) {
                INSTANCE?.liveUpdates?.reset(userId)
            }

            val validDomain = if (validateDomain(domain)) domain else null

            if (CONFIGURATION != null || INSTANCE != null && get().userId != userId) {
                clearLocalData {
                    if (CONFIGURATION != null)
                        stop()
                    INSTANCE = PyrusServiceDesk(
                        application,
                        appId,
                        userId,
                        securityKey,
                        validDomain,
                        apiVersion,
                        loggingEnabled
                    )
                }
            }
            else {
                INSTANCE = PyrusServiceDesk(application, appId, userId, securityKey, domain, apiVersion, loggingEnabled)
            }
        }

        private fun validateDomain(domain: String?): Boolean {
            if (domain == null) {
                return true
            }
            val domainRegex = """\w+(.\w+)+""".toRegex()
            return domainRegex.matches(domain)
        }

        /**
         * Launches UI of the PyrusServiceDesk.
         *
         * @param activity Activity that is used for launching service desk UI.
         * @param configuration Instance of [ServiceDeskConfiguration]. This is used for customizing UI.
         * @param onStopCallback The [OnStopCallback] interface to receive notification of stopping PyruServiceDesk.
         */
        @JvmStatic
        @JvmOverloads
        fun start(
            activity: Activity,
            configuration: ServiceDeskConfiguration? = null,
            onStopCallback: OnStopCallback? = null
        ) = startImpl(
            activity = activity,
            configuration = configuration,
            onStopCallback = onStopCallback
        )

        /**
         * Registers [subscriber] that will be notified when new replies from support are received
         */
        @JvmStatic
        @MainThread
        fun subscribeToReplies(subscriber: NewReplySubscriber) {
            PLog.d(TAG, "subscribeToReplies")
            get().liveUpdates.subscribeOnReply(subscriber)
        }

        /**
         * Unregisters [subscriber] from updates of new reply from support
         */
        @JvmStatic
        @MainThread
        fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
            PLog.d(TAG, "unsubscribeFromReplies")
            get().liveUpdates.unsubscribeFromReplies(subscriber)
        }

        /**
         * Assigns custom file chooser, that is appended as variant when the user is offered to choose the source
         * to attach a file to comment from.
         * Files which size exceed [RequestUtils.MAX_FILE_SIZE_MEGABYTES] MB will be ignored.
         *
         * @param fileChooser FileChooser instance that is used for launching custom UI for picking files.
         *                  null can be passed to unregister custom chooser.
         */
        @JvmStatic
        fun registerFileChooser(fileChooser: FileChooser?) {
            PLog.d(TAG, "registerFileChooser, fileChooser == null ${fileChooser == null}")
            FILE_CHOOSER = fileChooser
        }

        /**
         * Sets a callback for when authorization of user has failed.
         * When nothing is passed ServiceDesk will be closed after authorization failure.
         *
         * @param onAuthorizationFailed lambda that executes when authorization error has occurred.
         */
        @JvmStatic
        fun onAuthorizationFailed(onAuthorizationFailed: Runnable?) {
            this.onAuthorizationFailed = onAuthorizationFailed
        }

        /**
         * Launches the request for registering push token.
         * Callback can be invoked in a thread that differs from the one that has invoked [setPushToken]
         * Pass null as token in order to stop push updates.
         *
         * @param token string token to be registered. If null then push notifications stop.
         * @param callback callback that is invoked when result of registering of the token is received.
         *  This is invoked without error when token is successfully registered.
         */
        @JvmStatic
        fun setPushToken(token: String?, callback: SetPushTokenCallback) {
            PLog.d(TAG, "setPushToken, token: $token")
            val serviceDesk = get()
            val lastUpdateTime = serviceDesk.preferences.getLong(PREFERENCE_KEY_LAST_SET_TOKEN, -1L)
            val isSkip = lastUpdateTime != -1L
                    && System.currentTimeMillis() - lastUpdateTime < SET_PUSH_TOKEN_TIMEOUT * MILLISECONDS_IN_MINUTE

            when {
                isSkip -> callback.onResult(Exception("Too many requests. Maximum once every $SET_PUSH_TOKEN_TIMEOUT minutes."))
                serviceDesk.appId.isBlank() -> callback.onResult(Exception("AppId is not assigned"))
                serviceDesk.instanceId.isBlank() -> callback.onResult(Exception("UserId is not assigned"))
                else -> {
                    serviceDesk.preferences.edit().putLong(PREFERENCE_KEY_LAST_SET_TOKEN, System.currentTimeMillis()).apply()
                    GlobalScope.launch {
                        serviceDesk
                            .requestFactory
                            .getSetPushTokenRequest(token)
                            .execute(object : ResponseCallback<Unit> {
                                override fun onSuccess(data: Unit) {
                                    callback.onResult(null)
                                }

                                override fun onFailure(responseError: ResponseError) {
                                    callback.onResult(responseError)
                                }
                            })
                    }
                }
            }
        }

        /**
         * Stops PyrusServiceDesk. If UI was hidden, it will be finished during creating.
         */
        @JvmStatic
        fun stop() {
            PLog.d(TAG, "stop")
            get().sharedViewModel.quitServiceDesk()
        }

        /**
         * Manually refreshes feed of PyrusServiceDesk.
         */
        @JvmStatic
        fun refresh() {
            PLog.d(TAG, "refresh")
            if (lastRefreshes.size == REFRESH_MAX_COUNT
                && System.currentTimeMillis() - lastRefreshes.first() < MILLISECONDS_IN_MINUTE
            )
                return

            PLog.d(TAG, "refresh, execute")
            lastRefreshes.add(System.currentTimeMillis())
            if (lastRefreshes.size > REFRESH_MAX_COUNT)
                lastRefreshes.removeAt(0)

            get().sharedViewModel.triggerUpdate()
        }

        /**
         * Start tickets update if it is not already running.
         *
         * @param lastActiveTime Time of last user activity in unit millisecond
         */
        internal fun startTicketsUpdatesIfNeeded(lastActiveTime: Long) {
            get().liveUpdates.updateGetTicketsIntervalIfNeeded(lastActiveTime)
        }

        internal fun onServiceDeskStop() {
            get().onStopCallback?.onServiceDeskStop()
            get().onStopCallback = null
        }

        internal fun get(): PyrusServiceDesk {
            return checkNotNull(INSTANCE) { "Instantiate PyrusServiceDesk first" }
        }

        internal fun getConfiguration(): ServiceDeskConfiguration {
            if (CONFIGURATION == null)
                CONFIGURATION = ServiceDeskConfiguration()
            return CONFIGURATION!!
        }

        internal fun setConfiguration(config: ServiceDeskConfiguration) {
            CONFIGURATION = config
        }

        /**
         * @return Service desk shared preferences.
         */
        internal fun getPreferencesManager(): PreferencesManager {
            return get().preferencesManager
        }

        private fun startImpl(
            activity: Activity,
            configuration: ServiceDeskConfiguration? = null,
            onStopCallback: OnStopCallback? = null
        ) {
            CONFIGURATION = configuration
            get().sharedViewModel.clearQuitServiceDesk()
            get().onStopCallback = onStopCallback

            activity.startActivity(TicketActivity.getLaunchIntent())

            if (configuration == null)
                return
            val currentUserId = get().preferences.getString(PREFERENCE_KEY_USER_ID_V2, null)
            if (currentUserId != get().userId)
                refresh()
            get().preferences.edit().putString(PREFERENCE_KEY_USER_ID_V2, get().userId).apply()
        }

        private fun clearLocalData(doOnCleared : () -> Unit) {
            GlobalScope.launch {
                if (get().serviceDeskProvider.getRequestFactory().getRemoveAllPendingCommentsRequest().execute().hasError().not()) {
                    withContext(Dispatchers.Main) {
                        get().draftRepository.saveDraft("")
                        refresh()
                        doOnCleared.invoke()
                    }
                }
            }
        }
    }

    internal val serviceDeskProvider: ServiceDeskProvider by lazy {
        object : ServiceDeskProvider {
            override fun getApplication(): Application = application
            override fun getRequestFactory(): RequestFactory = requestFactory
            override fun getDraftRepository(): DraftRepository = draftRepository
            override fun getLiveUpdates(): LiveUpdates = liveUpdates
            override fun getLocalDataProvider(): LocalDataProvider = localDataProvider
            override fun getLocalDataVerifier(): LocalDataVerifier = localDataVerifier
        }
    }

    internal var instanceId: String

    private val requestFactory: RequestFactory
    private val draftRepository: DraftRepository
    private val liveUpdates: LiveUpdates

    private val localDataProvider: LocalDataProvider by lazy {
        LocalDataProvider(
            offlineRepository,
            fileResolver
        )
    }
    private val localDataVerifier: LocalDataVerifier

    private var sharedViewModel = SharedViewModel()

    private val fileResolver: FileResolver = FileResolverImpl(application.contentResolver)
    private val preferences = application.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
    private val preferencesManager = PreferencesManager(preferences)
    private val offlineRepository: OfflineRepository

    private var onStopCallback: OnStopCallback? = null

    init {
        migratePreferences(application, preferences)
        logging = loggingEnabled
        if (logging)
            PLog.instantiate(application)

        instanceId = ConfigUtils.getInstanceId(preferences)

        localDataVerifier = LocalDataVerifierImpl(fileResolver)

        val offlineGson =
            GsonBuilder()
                .setDateFormat(ISO_DATE_PATTERN)
                .registerTypeAdapter(Uri::class.java, UriGsonAdapter())
                .create()
        offlineRepository = PreferenceOfflineRepository(preferences, localDataVerifier, offlineGson)

        val remoteGson =
            GsonBuilder()
                .setDateFormat(ISO_DATE_PATTERN)
                .addSerializationExclusionStrategy(RemoteGsonExclusionStrategy())
                .create()

        val centralRepository = CentralRepository(
            RetrofitWebRepository(appId, instanceId, fileResolver, domain, remoteGson),
            offlineRepository
        )

        requestFactory = RequestFactory(centralRepository)
        draftRepository = PreferenceDraftRepository(preferences)
        liveUpdates = LiveUpdates(requestFactory, preferencesManager, userId)
    }

    internal fun getSharedViewModel() = sharedViewModel
}