package net.papirus.pyrusservicedesk.sdk.request

import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import net.papirus.pyrusservicedesk.sdk.response.ResponseBase
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks


/**
 * Request for creating new ticket.
 *
 * @param description description that is used for creating ticket.
 * @param uploadFileHooks hooks for handling file uploading/downloading progress and cancellation.
 */
internal class CreateTicketRequest(repository: GeneralRepository,
                                   private val description: TicketDescription,
                                   private val uploadFileHooks: UploadFileHooks?)
    : RequestBase<Int>(repository){

    override suspend fun run(repository: GeneralRepository): ResponseBase<Int> {
        return repository.createTicket(description, uploadFileHooks)
    }
}
