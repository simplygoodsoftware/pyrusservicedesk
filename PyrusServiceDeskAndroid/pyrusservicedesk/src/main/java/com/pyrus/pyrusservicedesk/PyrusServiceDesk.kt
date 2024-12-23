package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.MainActivity
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.core.DiInjector
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.getFirstNSymbols
import com.pyrus.pyrusservicedesk.core.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class PyrusServiceDesk private constructor(
    internal val application: Application,
    internal val appId: String,
    internal val userId: String?,
    internal val securityKey: String?,
    internal val domain: String?,
    internal val apiVersion: Int,
    loggingEnabled: Boolean,
    private val authToken: String?,
) {

    companion object {

        private val TAG = PyrusServiceDesk::class.java.simpleName

        internal var onAuthorizationFailed: Runnable? = Runnable {
            stop()
        }
        private var INSTANCE: PyrusServiceDesk? = null
        private var INJECTOR: DiInjector? = null
        private var lastRefreshes = ArrayList<Long>()

        private const val REFRESH_MAX_COUNT = 20 // in minute

        internal const val API_VERSION_1: Int = 0
        internal const val API_VERSION_2: Int = 2
        internal const val API_VERSION_3: Int = 4

        private const val DEFAULT_TOKEN_TYPE: String = "android"
//TODO delete
//        internal val users: List<User> = listOf(
//            User("255371017", "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg==", "Ресторан 1"),
//            User("251380375", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Много Лосося ДК Москва, Большая Филёвская улица, 3"),
//            User("251374579", "n4Mxu60kICP-XtZkGm2zCRlDtRRBi76h1w7FMx~f2F~z3d~Ayz7~Z7Gfxg7q2dI~sNVS965oM44Buy8uX2ngWib4BIIaf~6uIT6KaRzyGn2N6O2zdj-lufplexg1TvYLTviMSw==", "Старик Хинкалыч - Кострома Коллаж")
//        )

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
         * @param authorizationToken // TODO sds
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            domain: String? = null,
            isMultiChat: Boolean,
            loggingEnabled: Boolean = false,
            authorizationToken: String? = null,
        ) {
            initInternal(
                application,
                null,
                appId,
                null,
                null,
                isMultiChat,
                null,
                domain,
                API_VERSION_1,
                loggingEnabled,
                authorizationToken,
            )
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
         * @param authorizationToken // TODO sds
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            userId: String,
            isMultiChat: Boolean,
            securityKey: String,
            domain: String? = null,
            loggingEnabled: Boolean = false,
            authorizationToken: String? = null,
        ) {
            initInternal(
                application,
                null,
                appId,
                userId,
                null,
                isMultiChat,
                securityKey,
                domain,
                API_VERSION_2,
                loggingEnabled,
                authorizationToken,
            )
        }

        /**
         * Initializes PyrusServiceDesk embeddable module.
         * This init is used for authorized users with device independent sessions.
         * The best approach is to call this in [Application.onCreate]
         * ***PS***: Should be done before other public methods are is called.
         * Unhandled IllegalStateException is thrown otherwise.
         *
         * @param application instance of the enclosing application
         * @param appId id of a client // TODO
         * @param userId of the user who is initializing service desk // TODO
         * @param securityKey of the user far safe initialization // TODO
         * @param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
         * @param loggingEnabled If true, then the library will write logs,
         * and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
         * @param authorizationToken // TODO sds
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            listUser: List<User>,
            authorId: String,
            isMultiChat: Boolean,
            domain: String? = null,
            loggingEnabled: Boolean = false,
            authorizationToken: String? = null,
        ) {
            if (listUser.isEmpty()) throw Exception("user list is empty")

            initInternal(
                application,
                listUser,
                listUser.first().appId,
                null,
                authorId,
                isMultiChat,
                null,
                domain,
                API_VERSION_3,
                loggingEnabled,
                authorizationToken,
            )
        }

        private fun initInternal(
            application: Application,
            listUser: List<User>?,
            appId: String,
            userId: String?,
            authorId: String?,
            isMultiChat: Boolean,
            securityKey: String?,
            domain: String?,
            apiVersion: Int = API_VERSION_1,
            loggingEnabled: Boolean,
            authorizationToken: String?,
        ) {
            PLog.d(TAG, "initInternal, appId: ${appId.getFirstNSymbols(10)}, userId: ${userId?.getFirstNSymbols(10)}, apiVersion: $apiVersion")

            val preferences: SharedPreferences = application.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
            val instanceId = ConfigUtils.getInstanceId(preferences)

            val apiDomain =  domain ?: "pyrus.com"

            val newAccount = if (listUser != null && authorId != null)
                Account.V3(
                    instanceId,
                    appId,
                    apiDomain,
                    isMultiChat,
                    listUser.first().userId,
                    listUser,
                    authorId,
                )
            else if (userId == null || securityKey == null) Account.V1(
                instanceId,
                appId,
                apiDomain,
                isMultiChat
            )
            else Account.V2(
                instanceId,
                appId,
                apiDomain,
                isMultiChat,
                userId,
                securityKey,
            )

            INJECTOR = DiInjector(
                application,
                newAccount,
                loggingEnabled,
                authorizationToken,
                CoroutineScope(Dispatchers.Main),
                preferences
            )


            // TODO sds
//            if (INSTANCE != null && get().userId != userId) {
//                INSTANCE?.liveUpdates?.reset(userId)
//            }

            val validDomain = if (validateDomain(apiDomain)) apiDomain else null

            // TODO sds
//            if (INSTANCE != null && get().userId != userId) {
//                clearLocalData {
////                    if (CONFIGURATION != null)
////                        stop()
//                    INSTANCE = PyrusServiceDesk(
//                        application,
//                        appId,
//                        userId,
//                        securityKey,
//                        validDomain,
//                        apiVersion,
//                        loggingEnabled,
//                        authorizationToken,
//                    )
//                }
//            }
//            else {
//                INSTANCE = PyrusServiceDesk(
//                    application,
//                    appId,
//                    userId,
//                    securityKey,
//                    validDomain,
//                    apiVersion,
//                    loggingEnabled,
//                    authorizationToken,
//                )
//            }
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
            injector().liveUpdates.subscribeOnReply(subscriber)
        }

        /**
         * Unregisters [subscriber] from updates of new reply from support
         */
        @JvmStatic
        @MainThread
        fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
            PLog.d(TAG, "unsubscribeFromReplies")
            injector().liveUpdates.unsubscribeFromReplies(subscriber)
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
            StaticRepository.FILE_CHOOSER = fileChooser
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
         * @param tokenType cloud messaging type. "android" by default.
         */
        @JvmStatic
        @JvmOverloads
        fun setPushToken(
            token: String?,
            callback: SetPushTokenCallback,
            tokenType: String = DEFAULT_TOKEN_TYPE
        ) {
            PLog.d(TAG, "setPushToken, token: $token")
            injector().setPushTokenUseCase.invoke(token, callback, tokenType)
        }

        /**
         * Stops PyrusServiceDesk. If UI was hidden, it will be finished during creating.
         */
        @JvmStatic
        fun stop() {
            PLog.d(TAG, "stop")
            // TODO
//            get().sharedViewModel.quitServiceDesk()
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
            // TODO
//            get().sharedViewModel.triggerUpdate()
        }

        /**
         * Sets form field data used for autocompletion of task form fields when creating a ticket.
         * Map<field code, field value>
         */
        @JvmStatic
        fun setFieldsData(extraFields: Map<String, String>?) {
            StaticRepository.EXTRA_FIELDS = extraFields
        }

        /**
         * Start tickets update if it is not already running.
         *
         * @param lastActiveTime Time of last user activity in unit millisecond
         */
        internal fun startTicketsUpdatesIfNeeded(lastActiveTime: Long) {
            injector().liveUpdates.updateGetTicketsIntervalIfNeeded(lastActiveTime)
        }

        internal fun onServiceDeskStop() {
            // TODO
//            get().onStopCallback?.onServiceDeskStop()
//            get().onStopCallback = null
        }

        internal fun get(): PyrusServiceDesk {
            return checkNotNull(INSTANCE) { "Instantiate PyrusServiceDesk first" }
        }

        internal fun injector(): DiInjector = checkNotNull(INJECTOR)

        private fun startImpl(
            activity: Activity,
            configuration: ServiceDeskConfiguration? = null,
            onStopCallback: OnStopCallback? = null
        ) {
            StaticRepository.setConfiguration(configuration)

            // TODO
//            get().sharedViewModel.clearQuitServiceDesk()
//            get().onStopCallback = onStopCallback

            activity.startActivity(MainActivity.createLaunchIntent(activity))

            // TODO sds
            if (configuration == null)
                return

            // TODO sds добавить логику обновления аккаунта в sp
//            val currentUserId = get().preferences.getString(PREFERENCE_KEY_USER_ID_V2, null)
//            if (currentUserId != get().userId)
//                refresh()
//            get().preferences.edit().putString(PREFERENCE_KEY_USER_ID_V2, get().userId).apply()
        }

        private fun clearLocalData(doOnCleared : () -> Unit) {
            // TODO sds
//            GlobalScope.launch {
//                if (get().serviceDeskProvider.getRequestFactory().getRemoveAllPendingCommentsRequest().execute().hasError().not()) {
//
//                    get().fileManager.clearTempDir()
//
//                    withContext(Dispatchers.Main) {
//                        get().draftRepository.saveDraft("")
//                        refresh()
//                        doOnCleared.invoke()
//                    }
//                }
//            }
        }


    }

    private var sharedViewModel = SharedViewModel()

    private var onStopCallback: OnStopCallback? = null

    init {
        // // TODO sds изменить место инициализации логов, добавить отдельную логику для включения и выключения логов
//        migratePreferences(application, preferences)

        // TODO sds
        if (loggingEnabled) PLog.instantiate(application)

    }

    internal fun getSharedViewModel() = sharedViewModel
}