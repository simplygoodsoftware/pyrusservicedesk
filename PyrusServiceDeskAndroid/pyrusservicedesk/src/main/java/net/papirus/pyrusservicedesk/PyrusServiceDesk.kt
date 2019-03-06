package net.papirus.pyrusservicedesk

import android.app.Application
import net.papirus.pyrusservicedesk.repository.Repository
import net.papirus.pyrusservicedesk.repository.RepositoryFactory
import net.papirus.pyrusservicedesk.repository.web_service.WebServiceFactory
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel

class PyrusServiceDesk private constructor(
        internal val application: Application,
        private val clientId: String){

    companion object {
        private var INSTANCE: PyrusServiceDesk? = null

        @JvmStatic
        fun init(application: Application, clientId: String) {
            INSTANCE = PyrusServiceDesk(application, clientId)
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

    internal val sharedViewModel:SharedViewModel by lazy { SharedViewModel() }
}