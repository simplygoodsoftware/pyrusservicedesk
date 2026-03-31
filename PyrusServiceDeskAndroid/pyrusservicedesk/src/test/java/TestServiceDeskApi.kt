import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.data.json.DateAdapter
import com.pyrus.pyrusservicedesk.sdk.data.json.UriAdapter
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.ServiceDeskApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Part
import java.net.UnknownHostException

/**
 * Interface that is used for making api calls using [Synchronizer] and [RemoteFileStore].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TestServiceDeskApi(
    private val testScope: TestScope
) : ServiceDeskApi {

    private var lastTime = System.currentTimeMillis()

    private fun createMoshi() = Moshi.Builder()
        .add(CommandParamsDto.factory)
        .add(DateAdapter())
        .add(UriAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Api call for getting tikets.
     */
    override suspend fun getTickets(@Body requestBody: RequestBodyBase) : Try<TicketsDto> {
        syncCount++
        val currentTime = System.currentTimeMillis()
        testScope.advanceTimeBy(currentTime - lastTime)
        lastTime = currentTime
        val time = testScope.currentTime
        listSyncTime.add(time)
        return response.fromJson()
    }

    /**
     * Api call for uploading files.
     */
    override suspend fun uploadFile(@Part file: MultipartBody.Part): Try<FileUploadResponseData> {
        return Try.Failure(Throwable()) //TODO kate
    }

    fun unexpectedCall(): Try.Failure = Try.Failure(UnknownHostException("pyrus.com"))

    inline fun <reified Response> String?.fromJson(): Try<Response> {
        if (this == null) return unexpectedCall()
        val response = createMoshi().adapter(Response::class.java).fromJson(this)
        return Try.Success(response!!)
    }

    companion object {
        private var syncCount = 0
        private val listSyncTime = mutableListOf<Long>()
        private var response: String? = Responses.emptyTickets
        fun getSyncCount() = syncCount
        fun getListSyncTime() = listSyncTime
        fun setGetTicketsResponse(response: String?) {
            this.response = response
        }

        fun cleanSyncData() {
            syncCount = 0
            listSyncTime.clear()
        }
    }

}