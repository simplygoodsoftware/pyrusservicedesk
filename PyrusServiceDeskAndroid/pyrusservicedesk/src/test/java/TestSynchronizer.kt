import android.util.Log
import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
import com.pyrus.pyrusservicedesk.sdk.sync.Synchronizer
import com.pyrus.pyrusservicedesk.sdk.updates.Preferences
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestDispatcher
import kotlin.coroutines.CoroutineContext

internal class TestSynchronizer(
    api: ServiceDeskApi,
    localTicketsStore: LocalTicketsStore,
    accessDeniedEventBus: AccessDeniedEventBus,
    accountStore: AccountStore,
    resourceManager: AppResourceManager,
    idStore: IdStore,
    commandsStore: LocalCommandsStore,
    preferences: Preferences,
    systemMessageStore: SystemMessageStore,
    testDispatcher: TestDispatcher
) : Synchronizer(
    api,
    localTicketsStore,
    accessDeniedEventBus,
    accountStore,
    resourceManager,
    idStore,
    commandsStore,
    preferences,
    systemMessageStore,
) {
    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    override val coroutineContext: CoroutineContext = testDispatcher +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            Log.e("TestSynchronizer", "sync global error: ${throwable.message}")
            PLog.e("TestSynchronizer", "sync global error: ${throwable.message}")
            throwable.printStackTrace()
        }

}