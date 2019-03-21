package net.papirus.pyrusservicedesk

import android.app.Application
import android.arch.lifecycle.Observer
import kotlinx.coroutines.asCoroutineDispatcher
import net.papirus.pyrusservicedesk.presentation.viewmodel.SharedViewModel
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.sdk.RepositoryFactory
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.web.retrofit.RetrofitWebRepository
import java.util.concurrent.Executors

class PyrusServiceDesk private constructor(
        internal val application: Application,
        internal val appId: String,
        internal val clientId: Int,
        internal val clientName: String,
        internal val enableFeedUi: Boolean){

    companion object {
        private var INSTANCE: PyrusServiceDesk? = null

        @JvmStatic
        fun init(application: Application, appId: String, clientId: Int, clientName: String) {
            INSTANCE = PyrusServiceDesk(application, appId, clientId, clientName, false)
        }

        internal fun getInstance() : PyrusServiceDesk {
            return checkNotNull(INSTANCE){ "Instantiate PyrusServiceDesk first" }
        }

        internal val DISPATCHER_IO_SINGLE = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    internal val requestFactory: RequestFactory by lazy {
        RequestFactory(
            RepositoryFactory.create(
                RetrofitWebRepository(
                    appId,
                    clientId.toString(),
                    clientName,
                    fileResolver))
        )
    }

    internal val localDataProvider: LocalDataProvider by lazy {
        LocalDataProvider(clientName, fileResolver =  fileResolver)
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