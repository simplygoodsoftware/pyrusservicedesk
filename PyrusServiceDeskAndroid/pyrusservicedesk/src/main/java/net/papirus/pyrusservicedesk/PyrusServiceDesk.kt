package net.papirus.pyrusservicedesk

import android.app.Application
import android.arch.lifecycle.Observer
import net.papirus.pyrusservicedesk.repository.Repository
import net.papirus.pyrusservicedesk.repository.RepositoryFactory
import net.papirus.pyrusservicedesk.repository.web_service.WebServiceFactory
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel

class PyrusServiceDesk private constructor(
        internal val application: Application,
        private val clientId: String,
        internal val clientName: String,
        internal val enableRichUi: Boolean){

    companion object {
        private var INSTANCE: PyrusServiceDesk? = null

        @JvmStatic
        fun init(application: Application, clientId: String, clientName: String, enableRichUi: Boolean) {
            INSTANCE = PyrusServiceDesk(application, clientId, clientName, enableRichUi)
        }

        internal fun getInstance() : PyrusServiceDesk {
            return checkNotNull(INSTANCE){ "Instantiate PyrusServiceDesk first" }

        }
    }

    internal val repository: Repository by lazy{
        RepositoryFactory.create(
                WebServiceFactory.create(
                        "b7206b43-6859-4a20-837d-637a68e92d94",
                        12345.toString()),
                application.contentResolver
        )
    }

    private var sharedViewModel: SharedViewModel? = null

    private val quitObserver = Observer<Boolean> {
        it?.let{value ->
            if (value)
                refreshSharedViewModel()
        }
    }

    internal fun getSharedViewModel(): SharedViewModel{
        if (sharedViewModel == null)
            refreshSharedViewModel()
        return sharedViewModel!!
    }

    private fun refreshSharedViewModel() {
        sharedViewModel?.getQuitServiceDeskLiveData()?.removeObserver(quitObserver)
        sharedViewModel = SharedViewModel().also { it.getQuitServiceDeskLiveData().observeForever(quitObserver) }
    }
}