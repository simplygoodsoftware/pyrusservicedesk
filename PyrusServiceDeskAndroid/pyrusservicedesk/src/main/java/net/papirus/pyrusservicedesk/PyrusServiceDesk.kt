package net.papirus.pyrusservicedesk

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.Observer
import android.content.Intent
import com.example.pyrusservicedesk.R
import kotlinx.coroutines.asCoroutineDispatcher
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsActivity
import net.papirus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.RepositoryFactory
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.web.retrofit.RetrofitWebRepository
import net.papirus.pyrusservicedesk.utils.isTablet
import java.util.concurrent.Executors

class PyrusServiceDesk private constructor(
        internal val application: Application,
        internal val appId: String,
        internal val isSingleChat: Boolean){

    internal var userId: Int = 0
    internal var userName: String = application.getString(R.string.psd_guest)

    companion object {
        internal val DISPATCHER_IO_SINGLE = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        private var INSTANCE: PyrusServiceDesk? = null
        private var THEME: ServiceDeskTheme? = null

        @JvmStatic
        fun init(application: Application, appId: String, singleChat: Boolean) {
            INSTANCE = PyrusServiceDesk(application, appId, singleChat)
        }

        @JvmStatic
        fun start(activity: Activity, theme: ServiceDeskTheme? = null) {
            startImpl(activity = activity, theme = theme)
        }

        @JvmStatic
        fun startTicket(ticketId: Int, activity: Activity, theme: ServiceDeskTheme? = null) {
            startImpl(ticketId, activity, theme)
        }

        @JvmStatic
        fun setUser(userId: Int, userName: String) {
            getInstance().apply {
                this.userId = userId
                this.userName = userName
            }
        }

        internal fun getInstance() : PyrusServiceDesk {
            return checkNotNull(INSTANCE){ "Instantiate PyrusServiceDesk first" }
        }

        internal fun getTheme(): ServiceDeskTheme {
            if (THEME == null)
                THEME = ServiceDeskTheme(getInstance().application.isTablet())
            return THEME!!
        }

        private fun startImpl(ticketId: Int? = null, activity: Activity, theme: ServiceDeskTheme? = null) {
            THEME = theme
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

    internal val requestFactory: RequestFactory by lazy {
        RequestFactory(
            RepositoryFactory.create(
                RetrofitWebRepository(
                    appId,
                    userId.toString(),
                    userName,
                    fileResolver))
        )
    }

    internal val localDataProvider: LocalDataProvider by lazy {
        LocalDataProvider(userName, fileResolver =  fileResolver)
    }

    internal fun getSharedViewModel(): SharedViewModel {
        if (sharedViewModel == null)
            refreshSharedViewModel()
        return sharedViewModel!!
    }

    private val fileResolver by lazy { FileResolver(application.contentResolver) }

    private var sharedViewModel: SharedViewModel? = null

    private val quitObserver = Observer<Boolean> {
        it?.let{value ->
            if (value)
                refreshSharedViewModel()
        }
    }

    private fun refreshSharedViewModel() {
        sharedViewModel?.getQuitServiceDeskLiveData()?.removeObserver(quitObserver)
        sharedViewModel = SharedViewModel().also { it.getQuitServiceDeskLiveData().observeForever(quitObserver) }
    }
}