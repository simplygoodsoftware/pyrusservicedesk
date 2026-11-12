import com.pyrus.pyrusservicedesk._ref.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime

@OptIn(ExperimentalCoroutinesApi::class)
class TestTimeProvider(private val testScope: TestScope): TimeProvider {
    override fun currentTimeMillis(): Long {
        return testScope.currentTime
    }
}