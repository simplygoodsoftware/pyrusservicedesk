package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.Repository
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal class CreateTicketRequest(
    repository: Repository,
    private val description: TicketDescription,
    private val uploadFileHooks: UploadFileHooks
)
    : RequestBase<Int>(repository){

    override fun run(repository: Repository): ResponseBase<Int> {
        return repository.createTicket(description, uploadFileHooks)
    }
}
