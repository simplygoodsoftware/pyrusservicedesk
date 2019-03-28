package net.papirus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity
import net.papirus.pyrusservicedesk.presentation.viewmodel.QuitViewModel
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.RepositoryFactory
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.repositories.draft.DraftRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.updates.LiveUpdates
import net.papirus.pyrusservicedesk.sdk.updates.NewReplySubscriber
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import java.util.concurrent.Executors

class PyrusServiceDesk private constructor(
        internal val application: Application,
        internal val appId: String,
        internal val isSingleChat: Boolean){

    companion object {
        internal val DISPATCHER_IO_SINGLE = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        internal var FILE_CHOOSER: FileChooser? = null
        private var PREFERENCE_KEY = "net.papirus.pyrusservicedesk.PREFERENCES"
        private var INSTANCE: PyrusServiceDesk? = null
        private var CONFIGURATION: ServiceDeskConfiguration? = null

        /**
         * Initializes PyrusServiceDesk embeddable module.
         * The best approach is to call this in [Application.onCreate]
         * ***PS***: Should be done before [start] is called. Unhandled IllegalStateException is thrown otherwise.
         *
         * @param application instance of the enclosing application
         * @param appId id of a client
         */
        @JvmStatic
        fun init(application: Application, appId: String) {
            INSTANCE = PyrusServiceDesk(application, appId, true)
        }

        /**
         * Launches UI of the PyrusServiceDesk.
         *
         * @param activity activity that is used for launching service desk ui
         * @param configuration instance of [ServiceDeskConfiguration].
         */
        @JvmStatic
        fun start(activity: Activity, configuration: ServiceDeskConfiguration) {
            startImpl(activity = activity, configuration = configuration)
        }

        /**
         * Launches UI of the PyrusServiceDesk with default configuration.
         *
         * @param activity activity that is used for launching service desk ui
         */
        @JvmStatic
        fun start(activity: Activity) {
            startImpl(activity = activity)
        }

        /**
         * Registers [subscriber] on updates of new reply from support
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
         */
        @JvmStatic
        fun registerFileChooser(fileChooser: FileChooser) {
            FILE_CHOOSER = fileChooser
        }

        /**
         * Launches request for for registering push token. Callback is invoked without error when
         * token is successfully registered.
         * Callback can be invoked in a thread that differs from the one that has invoked [setPushToken]
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
                        var exception: Exception? = null
                        serviceDesk
                            .requestFactory
                            .getSetPushTokenRequest(serviceDesk.appId)
                            .execute(object: ResponseCallback<Unit>{
                                override fun onSuccess(data: Unit) {
                                }
                                override fun onFailure(responseError: ResponseError) {
                                    exception = responseError
                                }
                            })
                        callback.onResult(exception)
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

        internal fun forceConfiguration(config: ServiceDeskConfiguration) {
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

    internal val requestFactory: RequestFactory
    internal val draftRepository: DraftRepository
    internal val liveUpdates: LiveUpdates

    internal val localDataProvider: LocalDataProvider by lazy {
        LocalDataProvider(fileResolver =  fileResolver)
    }

    private val fileResolver: FileResolver = FileResolver(application.contentResolver)
    private val preferences = application.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)

    internal var userId: String = ConfigUtils.getUserId(preferences)

    init {
        val repositoryFactory = RepositoryFactory(fileResolver, preferences)
        requestFactory = RequestFactory(repositoryFactory.createCentralRepository(appId, userId))
        draftRepository = repositoryFactory.createDraftRepository()
        liveUpdates = LiveUpdates(requestFactory)
    }

    internal fun getSharedViewModel(): QuitViewModel {
        if (quitViewModel == null)
            refreshSharedViewModel()
        return quitViewModel!!
    }


    private var quitViewModel: QuitViewModel? = null

    private val quitObserver = Observer<Boolean> {
        it?.let{value ->
            if (value)
                refreshSharedViewModel()
        }
    }

    private fun refreshSharedViewModel() {
        quitViewModel?.getQuitServiceDeskLiveData()?.removeObserver(quitObserver)
        quitViewModel = QuitViewModel().also { it.getQuitServiceDeskLiveData().observeForever(quitObserver) }
    }
}