package net.papirus.pyrusservicedesk

import android.app.Application
import android.arch.lifecycle.Observer
import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.RepositoryFactory
import net.papirus.pyrusservicedesk.sdk.data.LocalDataProvider
import net.papirus.pyrusservicedesk.sdk.web_service.WebServiceFactory
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel

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
    }

    internal val repository: Repository by lazy{
        RepositoryFactory.create(
                WebServiceFactory.create(
                    appId,
                    clientId.toString(),
                    enableFeedUi),
                application.contentResolver,
                LocalDataProvider(clientName)
        )
    }

    internal fun getSharedViewModel(): SharedViewModel {
        if (sharedViewModel == null)
            refreshSharedViewModel()
        return sharedViewModel!!
    }

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