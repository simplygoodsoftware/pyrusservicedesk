package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity
import com.pyrus.pyrusservicedesk.presentation.viewmodel.QuitViewModel
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.RepositoryFactory
import com.pyrus.pyrusservicedesk.sdk.RequestFactory
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseCallback
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.PREFERENCE_KEY
import com.pyrus.pyrusservicedesk.utils.migratePreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class PyrusServiceDesk private constructor(
        internal val application: Application,
        internal val appId: String,
        internal val isSingleChat: Boolean){

    companion object {
        internal val DISPATCHER_IO_SINGLE = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        internal var FILE_CHOOSER: FileChooser? = null
        private var INSTANCE: PyrusServiceDesk? = null
        private var CONFIGURATION: ServiceDeskConfiguration? = null

        /**
         * Initializes PyrusServiceDesk embeddable module.
         * The best approach is to call this in [Application.onCreate]
         * ***PS***: Should be done before other public methods are is called.
         * Unhandled IllegalStateException is thrown otherwise.
         *
         * @param application instance of the enclosing application
         * @param appId id of a client
         */
        @JvmStatic
        fun init(application: Application, appId: String) {
            INSTANCE = PyrusServiceDesk(application, appId, true)
        }

        /**
         * Launches UI of the PyrusServiceDesk with default configuration.
         *
         * @param activity activity that is used for launching service desk UI
         */
        @JvmStatic
        fun start(activity: Activity) {
            startImpl(activity = activity)
        }

        /**
         * Launches UI of the PyrusServiceDesk.
         *
         * @param activity activity that is used for launching service desk UI
         * @param configuration instance of [ServiceDeskConfiguration]. This is used for customizing UI
         */
        @JvmStatic
        fun start(activity: Activity, configuration: ServiceDeskConfiguration) {
            startImpl(activity = activity, configuration = configuration)
        }

        /**
         * Registers [subscriber] that will be notified when new replies from support are received
         */
        @JvmStatic
        fun subscribeToReplies(subscriber: NewReplySubscriber){
            getInstance().liveUpdates.subscribeOnReply(subscriber)
        }

        /**
         * Unregisters [subscriber] from updates of new reply from support
         */
        @JvmStatic
        fun unsubscribeFromReplies(subscriber: NewReplySubscriber) {
            getInstance().liveUpdates.unsubscribeFromReplies(subscriber)
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
        fun registerFileChooser(fileChooser: FileChooser) {
            FILE_CHOOSER = fileChooser
        }

        /**
         * Launches the request for registering push token.
         * Callback can be invoked in a thread that differs from the one that has invoked [setPushToken]
         *
         * @param token string token to be registered
         * @param callback callback that is invoked when result of registering of the token is received.
         *  This is invoked without error when token is successfully registered.
         */
        @JvmStatic
        fun setPushToken(token: String, callback: SetPushTokenCallback) {
            val serviceDesk = getInstance()
            when{
                token.isBlank() -> callback.onResult(Exception("Token is empty"))
                serviceDesk.appId.isBlank() -> callback.onResult(Exception("AppId is not assigned"))
                serviceDesk.userId.isBlank() -> callback.onResult(Exception("UserId is not assigned"))
                else -> {
                    GlobalScope.launch {
                        serviceDesk
                            .requestFactory
                            .getSetPushTokenRequest(token)
                            .execute(object: ResponseCallback<Unit>{
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

        internal fun getInstance() : PyrusServiceDesk {
            return checkNotNull(INSTANCE){ "Instantiate PyrusServiceDesk first" }
        }

        internal fun getConfiguration(): ServiceDeskConfiguration {
            if (CONFIGURATION == null)
                CONFIGURATION = ServiceDeskConfiguration()
            return CONFIGURATION!!
        }

        internal fun setConfiguration(config: ServiceDeskConfiguration) {
            CONFIGURATION = config
        }

        private fun startImpl(ticketId: Int? = null, activity: Activity, configuration: ServiceDeskConfiguration? = null) {
            CONFIGURATION = configuration
            activity.startActivity(createIntent(ticketId))
        }

        private fun createIntent(ticketId: Int? = null): Intent {
            return when{
                ticketId != null -> TicketActivity.getLaunchIntent(ticketId)
                PyrusServiceDesk.getInstance().isSingleChat -> TicketActivity.getLaunchIntent()
                else -> TicketsActivity.getLaunchIntent()
            }
        }
    }

    internal var userId: String

    internal val requestFactory: RequestFactory
    internal val draftRepository: DraftRepository
    internal val liveUpdates: LiveUpdates

    internal val localDataProvider: LocalDataProvider by lazy {
        LocalDataProvider(fileResolver =  fileResolver)
    }

    private var quitViewModel = QuitViewModel()

    private val fileResolver: FileResolver = FileResolver(application.contentResolver)
    private val preferences = application.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)


    init {
        migratePreferences(application, preferences)
        userId = ConfigUtils.getUserId(preferences)
        val repositoryFactory = RepositoryFactory(fileResolver, preferences)
        requestFactory = RequestFactory(repositoryFactory.createCentralRepository(appId, userId))
        draftRepository = repositoryFactory.createDraftRepository()
        liveUpdates = LiveUpdates(requestFactory)
    }

    internal fun getSharedViewModel() = quitViewModel


}