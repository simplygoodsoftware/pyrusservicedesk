package net.papirus.pyrusservicedesk.repository.web_service.retrofit

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.data.intermediate.Comments
import net.papirus.pyrusservicedesk.repository.data.intermediate.FileUploadData
import net.papirus.pyrusservicedesk.repository.data.intermediate.Tickets
import net.papirus.pyrusservicedesk.repository.web_service.BASE_URL
import net.papirus.pyrusservicedesk.repository.web_service.WebService
import net.papirus.pyrusservicedesk.repository.web_service.response.*
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.*
import net.papirus.pyrusservicedesk.utils.ISO_DATE_PATTERN
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors


internal class RetrofitWebService(
        private val appId: String,
        internal val userId: String)
    : WebService {

    private val api: ServiceDeskApi

    init {
        val httpBuilder = OkHttpClient.Builder()
                .dispatcher(Dispatcher(Executors.newSingleThreadExecutor()))

        val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder().setDateFormat(ISO_DATE_PATTERN).create()))
                .client(httpBuilder.build())
                .build()

        api = retrofit.create(ServiceDeskApi::class.java)
    }


    override fun getConversation(request: RequestBase): LiveData<GetConversationResponse> {
        val result = MutableLiveData<GetConversationResponse>()
        api.getConversation(request.makeRequestBody(appId, userId)).enqueue(object: Callback<Comments>{

            override fun onFailure(call: Call<Comments>, t: Throwable) {
                result.postValue(GetConversationResponse(Status.WebServiceError, request))
            }

            override fun onResponse(call: Call<Comments>, response: Response<Comments>) {
                when (response.isSuccessful) {
                    true -> result.postValue(
                        GetConversationResponse(request = request, comments = response.body()?.comments))
                    else -> result.postValue(
                        GetConversationResponse(Status.WebServiceError, request))
                }
            }
        })
        return result
    }

    override fun getTickets(request: RequestBase): LiveData<GetTicketsResponse> {
        val result = MutableLiveData<GetTicketsResponse>()
        api.getTickets(request.makeRequestBody(appId, userId)).enqueue(object: Callback<Tickets>{

            override fun onFailure(call: Call<Tickets>, t: Throwable) {
                result.postValue(GetTicketsResponse(Status.WebServiceError, request))
            }

            override fun onResponse(call: Call<Tickets>, response: Response<Tickets>) {
                when (response.isSuccessful) {
                    true -> result.postValue(
                            GetTicketsResponse(request = request, tickets = response.body()?.tickets))
                    else -> result.postValue(
                            GetTicketsResponse(Status.WebServiceError, request))
                }
            }
        })
        return result
    }

    override fun getTicket(request: GetTicketRequest): LiveData<GetTicketResponse> {
        val result = MutableLiveData<GetTicketResponse>()
        api.getTicket(request.makeRequestBody(appId, userId), request.ticketId)
                .enqueue(object: Callback<Ticket>{

                    override fun onFailure(call: Call<Ticket>, t: Throwable) {
                        result.postValue(GetTicketResponse(Status.WebServiceError, request))
                    }

                    override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
                        when (response.isSuccessful) {
                            true -> result.postValue(
                                    GetTicketResponse(request = request, ticket = response.body()))
                            else -> result.postValue(
                                    GetTicketResponse(Status.WebServiceError, request))
                        }
                    }
                })
        return result
    }

    override fun createTicket(request: CreateTicketRequest): LiveData<CreateTicketResponse> {
        val result = MutableLiveData<CreateTicketResponse>()
        api.createTicket(request.makeRequestBody(appId, userId))
                .enqueue(object: Callback<ResponseBody>{

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        result.postValue(
                                CreateTicketResponse(Status.WebServiceError, request))
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        when (response.isSuccessful) {
                            true -> result.postValue(
                                    CreateTicketResponse(
                                            request = request,
                                            ticketId = Gson().fromJson<Map<String, Int>>(
                                                    response.body()?.string(),
                                                    Map::class.java)
                                                    .values
                                                    .first()
                                                    .toInt()))
                            else -> result.postValue(
                                CreateTicketResponse(Status.WebServiceError, request))
                        }
                    }
                })
        return result
    }

    override fun addComment(request: AddCommentRequest): LiveData<AddCommentResponse>{
        val result = MutableLiveData<AddCommentResponse>()
        api.addComment(request.makeRequestBody(appId, userId), request.ticketId)
                .enqueue(object: Callback<ResponseBody>{

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        result.postValue(
                                AddCommentResponse(Status.WebServiceError, request))
                    }

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        when (response.isSuccessful) {
                            true -> result.postValue(
                                    AddCommentResponse(
                                            request = request,
                                            commentId = Gson().fromJson<Map<String, Double>>(
                                                    response.body()?.string(),
                                                    Map::class.java)
                                                    .values
                                                    .first().toInt()))
                            else -> result.postValue(
                                    AddCommentResponse(Status.WebServiceError, request))
                        }
                    }
                })
        return result
    }

    override fun uploadFile(request: UploadFileRequest): LiveData<UploadFileResponse> {

        val result = MediatorLiveData<UploadFileResponse>()
        api.uploadFile(request.makeRequestBody(appId, userId).toMultipartBody())
                .enqueue(object: Callback<FileUploadData>{

                    override fun onFailure(call: Call<FileUploadData>, t: Throwable) {
                        result.postValue(
                                UploadFileResponse(Status.WebServiceError, request))

                    }

                    override fun onResponse(call: Call<FileUploadData>, response: Response<FileUploadData>) {
                        when (response.isSuccessful) {
                            true -> result.postValue(
                                    UploadFileResponse(request = request, uploadData = response.body()))
                            else -> result.postValue(
                                    UploadFileResponse(Status.WebServiceError, request))
                        }
                    }
        })
        return result
    }
}