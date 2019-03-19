package net.papirus.pyrusservicedesk.presentation.usecases.tickets

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.RequestFactory
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseCallback
import net.papirus.pyrusservicedesk.sdk.response.ResponseError
import net.papirus.pyrusservicedesk.sdk.updates.UpdateBase
import net.papirus.pyrusservicedesk.sdk.updates.UpdateType

internal class TicketsViewModel(serviceDesk: PyrusServiceDesk)
    : ConnectionViewModelBase(serviceDesk) {

    private val isLoading = MediatorLiveData<Boolean>()
    private val tickets = MediatorLiveData<List<TicketShortDescription>>()
    private val request = MutableLiveData<Boolean>()

    init{
        isLoading.apply {
            addSource(request){
                value = true
            }
        }

        tickets.apply {
            addSource(
                Transformations.switchMap(request){
                    GetTicketsUseCase(requests).execute()
                }
            ){
                isLoading.value = false
                tickets.value = it?.sortedWith(TicketShortDescriptionComparator())
                onDataLoaded()
            }
        }
        if (isNetworkConnected.value == true) {
            loadData()
            replayProgress()
        }
    }

    override fun <T : UpdateBase> onUpdateReceived(update: T) {
        if (!update.hasError())
            loadData()
    }

    override fun getUpdateTypes(): Set<UpdateType> {
        return setOf(UpdateType.TicketCreated, UpdateType.CommentAdded)
    }

    override fun loadData() {
        request.value = true
    }

    fun getIsLoadingLiveData(): LiveData<Boolean> = isLoading

    fun getTicketsLiveData(): LiveData<List<TicketShortDescription>> = tickets

    private class TicketShortDescriptionComparator : Comparator<TicketShortDescription> {

        override fun compare(o1: TicketShortDescription, o2: TicketShortDescription): Int {
            return when {
                o1.lastComment == null -> return when {
                    o2.lastComment == null -> o1.ticketId - o2.ticketId
                    else -> 1
                }
                o2.lastComment == null -> -1
                o1.lastComment.creationDate.before(o2.lastComment.creationDate) -> 1
                o1.lastComment.creationDate.after(o2.lastComment.creationDate) -> -1
                else -> o1.ticketId - o2.ticketId
            }
        }
    }
}

private class GetTicketsUseCase(val requests: RequestFactory) {
    fun execute(): LiveData<List<TicketShortDescription>> {
        val result = MutableLiveData<List<TicketShortDescription>>()
        CoroutineScope(Dispatchers.IO).launch {
            var tickets: List<TicketShortDescription>? = null
            var error: ResponseError? = null
            requests.getTicketsRequest().execute(object: ResponseCallback<List<TicketShortDescription>>{
                override fun onSuccess(data: List<TicketShortDescription>) {
                    tickets = data
                }

                override fun onFailure(responseError: ResponseError) {

                }

            })
            withContext(Dispatchers.Main){
                tickets?.let {
                    result.value = it
                }
            }
        }
        return result
    }
}