package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.onAuthorizationFailed
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.setPushToken
import com.pyrus.pyrusservicedesk.SdConstants.PYRUS_BASE_DOMAIN
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.MainActivity
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.getFirstNSymbols
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk._ref.utils.migratePreferences
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.DiInjector
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeature
import com.pyrus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.OnStopCallback
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class PyrusServiceDesk private constructor(
    internal val application: Application,
    internal val appId: String,
    internal val userId: String?,
    internal val securityKey: String?,
) {

    companion object {

        private val TAG = PyrusServiceDesk::class.java.simpleName

        internal var onAuthorizationFailed: Runnable? = Runnable {
            stop()
        }
        private var INSTANCE: PyrusServiceDesk? = null
        private var INJECTOR: DiInjector? = null
        private var lastRefreshes = ArrayList<Long>()

        private var autoRefreshFeatureFactory: AutoRefreshFeature? = null

        private const val REFRESH_MAX_COUNT = 20 // in minute

        internal const val API_VERSION_1: Int = 0
        internal const val API_VERSION_2: Int = 2
        internal const val API_VERSION_3: Int = 4

        private const val DEFAULT_TOKEN_TYPE: String = "android"

        private var onStopCallback: OnStopCallback? = null

        private val liveUpdates = LiveUpdates()
        var sdIsOpen = false

//        @JvmStatic
//        fun getTicketsFeature(): TicketsFeature {
//            val factory = injector().ticketsFeatureFactory.create()
//            return factory
//        }
//
//        @JvmStatic
//        fun getTicketFeature(
//            user: UserInternal,
//            initialTicketId: Long,
//            welcomeMessage: String?,
//            sendComment: String?,
//        ): TicketFeature {
//            val factory = injector().ticketFeatureFactory.create(user, initialTicketId, welcomeMessage, sendComment)
//            return factory
//        }
//
//        @JvmStatic
//        fun getSearchFeature(): SearchFeature {
//            val factory = injector().searchFeatureFactory.create()
//            return factory
//        }
//
//        @JvmStatic
//        fun getAccessDeniedFeature(): AccessDeniedFeature {
//            val factory = injector().accessDeniedFeatureFactory.create()
//            return factory
//        }
//
//        @JvmStatic
//        fun getAutoRefreshFeature(): AutoRefreshFeature {
//            val factory = injector().autoRefreshFeatureFactory.create()
//            return factory
//        }
//
//        @JvmStatic
//        fun isTimeToRate(): Boolean {
//            return injector().rateTimeUseCase.isTimeToRate()
//        }
//
//        @JvmStatic
//        fun audioClear() {
//            injector().audioWrapper.clearPositions()
//        }
//
//        @JvmStatic
//        fun updateMainActivityIsActive(isActive: Boolean) {
//            injector().audioWrapper.updateMainActivityIsActive(isActive)
//            if (!isActive)
//                injector().stopSession()
//        }
//
//        @JvmStatic
//        fun rateApp() {
//            injector().rateTimeUseCase.onAppRated()
//        }
//
//        @JvmStatic
//        fun getUsers(): List<User> {
//            return injector().accountStore.getAccount().getUsers()
//        }
//
//        @JvmStatic
//        fun getVendors(): List<Vendor> {
//            return injector().repository.getApplications().map {
//                Vendor(
//                    appId = it.appId,
//                    orgName = it.orgName ?: "",
//                    orgUrl = it.orgLogoUrl?.let { url -> RequestUtils.getOrganisationLogoUrl(url, "dev.pyrus.com") },
//                    orgDescription = it.orgDescription
//                )
//            }
//        }
//
//        @JvmStatic
//        fun getMembers(userId: String): List<Member> {
//            return injector().repository.getMembers(userId)
//        }
//
//        @JvmStatic
//        fun addUser(user: User) {
//            injector().addUserUseCase.addUser(user)
//        }
//
//        @JvmStatic
//        fun updateName(name: String) {
//            StaticRepository.getConfiguration().userName = name
//        }

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
         * @param authorizationToken authorization token that is sent to the backend.
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            domain: String? = null,
            loggingEnabled: Boolean = false,
            authorizationToken: String? = null,
        ) {
            initInternal(
                application,
                null,
                appId,
                null,
                null,
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
         * @param authorizationToken authorization token that is sent to the backend.
         */
        @JvmStatic
        @JvmOverloads
        fun init(
            application: Application,
            appId: String,
            userId: String,
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
         * @param listUser list of the users.
         * @param authorId author phone number hash.
         * @param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
         * @param loggingEnabled If true, then the library will write logs,
         * and they can be sent as a file to chat by clicking the "Send Library Logs" button in the menu under the "+" sign.
         * @param authorizationToken authorization token that is sent to the backend.
         */
//        @JvmStatic
//        @JvmOverloads
//        fun initAsMultichat(
//            application: Application,
//            listUser: List<User>,
//            authorId: String,
//            domain: String? = null,
//            loggingEnabled: Boolean = false,
//            authorizationToken: String? = null,
//        ) {
//            if (listUser.isEmpty()) throw Exception("user list is empty")
//
//            initInternal(
//                application,
//                listUser,
//                listUser.first().appId,
//                null,
//                authorId,
//                null,
//                domain,
//                API_VERSION_3,
//                loggingEnabled,
//                authorizationToken,
//            )
//        }
        private fun initInternal(
            application: Application,
            listUser: List<User>?,
            appId: String,
            userId: String?,
            authorId: String?,
            securityKey: String?,
            domain: String?,
            apiVersion: Int = API_VERSION_1,
            loggingEnabled: Boolean,
            authorizationToken: String?,
        ) {
            StaticRepository.logging = loggingEnabled
            PLog.d(TAG,"initInternal, appId: ${appId.getFirstNSymbols(10)}, userId: ${userId?.getFirstNSymbols(10)}, apiVersion: $apiVersion")
            Log.d(TAG,"initInternal, appId: ${appId.getFirstNSymbols(10)}, userId: ${userId?.getFirstNSymbols(10)}, apiVersion: $apiVersion")

            val preferences: SharedPreferences = application.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
            val isVersion1 = listUser == null && authorId == null && userId == null && securityKey == null
            val instanceId = ConfigUtils.getInstanceId(preferences, isVersion1)

            val apiDomain = domain ?: PYRUS_BASE_DOMAIN

            val newAccount = if (listUser != null && authorId != null)
                Account.V3(
                    domain = apiDomain,
                    instanceId = instanceId,
                    users = listUser,
                    authorId = authorId,
                )
            else if (userId != null && securityKey != null) Account.V2(
                domain = apiDomain,
                instanceId = instanceId,
                appId = appId,
                userId = userId,
                securityKey = securityKey,
            )
            else Account.V1(
                domain = apiDomain,
                instanceId = instanceId,
                appId = appId,
            )
            val oldUserId = INJECTOR?.accountStore?.getAccount()?.getUserId()
            autoRefreshFeatureFactory?.cancel()

            INJECTOR?.onCancel()

            INJECTOR = DiInjector(
                application = application,
                initialAccount = newAccount,
                authToken = authorizationToken,
                coreScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
                    throwable.printStackTrace()
                    Log.e(TAG, "coreScope global error: ${throwable.message}")
                    PLog.e(TAG, "coreScope global error: ${throwable.message}")
                    throwable.printStackTrace()
                }),
                preferences = preferences
            )

                autoRefreshFeatureFactory = INJECTOR?.autoRefreshFeatureFactory?.create(liveUpdates)
                if (oldUserId != null && oldUserId != newAccount.getUserId()) {
                    liveUpdates.reset(INJECTOR?.preferencesManager)
                    clearLocalData {}
                }

            migratePreferences(application, preferences)
            if (loggingEnabled) PLog.instantiate(application)

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
            onStopCallback: OnStopCallback? = null,
            sendComment: String? = null,
            voiceMessage: Boolean = false,
            openTicketAction: OpenTicketAction? = null,
        ) {
            val account = injector().accountStore.getAccount()
            startImpl(
                activity = activity,
                account = account,
                configuration = configuration,
                onStopCallback = onStopCallback,
                openTicketAction = openTicketAction,
                sendComment = sendComment,
                voiceMessage = voiceMessage,
            )
        }

        /**
         * Registers [subscriber] that will be notified when new replies from support are received
         */
        @JvmStatic
        @MainThread
        fun subscribeToReplies(subscriber: NewReplySubscriber) {
            PLog.d(TAG, "subscribeToReplies")
            liveUpdates.subscribeOnReply(subscriber)
        }

        /**
         * Unregisters [subscriber] from updates of new reply from support
         */
        @JvmStatic
        @MainThread
        fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
            PLog.d(TAG, "unsubscribeFromReplies")
            liveUpdates.unsubscribeFromReplies(subscriber)
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
            callback: SetPushTokenCallback?,
            tokenType: String = DEFAULT_TOKEN_TYPE,
        ) {
            PLog.d(TAG, "setPushToken, token: $${token?.takeLast(5)}")
            injector().setPushTokenUseCase.invoke(token, callback, tokenType)
        }

        /**
         * Stops PyrusServiceDesk. If UI was hidden, it will be finished during creating.
         */
        @JvmStatic
        fun stop() {
            PLog.d(TAG, "stop")
            INJECTOR?.finishEventBus?.post(true)
        }

//        @JvmStatic
//        fun clean() {
//            PLog.d(TAG, "clean")
//            INJECTOR?.cleanDataUseCase()
//        }

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
            INJECTOR?.refreshUseCase?.refresh()
        }

        /**
         * Sets form field data used for autocompletion of task form fields when creating a ticket.
         * Map<field code, field value>
         */
        @JvmStatic
        fun setFieldsData(extraFields: Map<String, String>?) {
            StaticRepository.EXTRA_FIELDS = extraFields
        }

        internal fun onServiceDeskStop() {
            sdIsOpen = false
            injector().releaseSession()
            onStopCallback?.onServiceDeskStop()
            onStopCallback = null
        }

        internal fun get(): PyrusServiceDesk {
            return checkNotNull(INSTANCE) { "Instantiate PyrusServiceDesk first" }
        }

        internal fun injector(): DiInjector {
            if (INJECTOR == null) {
                Log.d("SDS", "INJECTOR == null")
            }
            return checkNotNull(INJECTOR)
        }

        private fun startImpl(
            activity: Activity,
            account: Account,
            configuration: ServiceDeskConfiguration?,
            onStopCallback: OnStopCallback?,
            openTicketAction: OpenTicketAction?,
            sendComment: String? = null,
            voiceMessage: Boolean = false,
        ) {
            configuration?.voiceMessage = voiceMessage
            StaticRepository.setConfiguration(configuration)

            this.onStopCallback = onStopCallback

            val intent = MainActivity.createLaunchIntent(activity, account, openTicketAction, sendComment)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            sdIsOpen = true

            injector().updateUserUseCase.updateUser()
        }

        private fun clearLocalData(doOnCleared : () -> Unit) {
            INJECTOR?.cleanDataUseCase()
            refresh()
            doOnCleared.invoke()
        }


    }

    private var sharedViewModel = SharedViewModel()

    internal fun getSharedViewModel() = sharedViewModel
}