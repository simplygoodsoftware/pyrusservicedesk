package com.pyrus.pyrusservicedesk.sdk.request

import com.pyrus.pyrusservicedesk.sdk.data.TicketDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CreateTicketResponseData
import com.pyrus.pyrusservicedesk.sdk.repositories.general.GeneralRepository
import com.pyrus.pyrusservicedesk.sdk.response.ResponseImpl
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks


/**
 * Request for creating new ticket.
 *
 * @param description description that is used for creating ticket.
 * @param uploadFileHooks hooks for handling file uploading/downloading progress and cancellation.
 */
internal class CreateTicketRequest(repository: GeneralRepository,
                                   private val description: TicketDescription,
                                   private val uploadFileHooks: UploadFileHooks?)
    : RequestBase<CreateTicketResponseData>(repository){

    override suspend fun run(repository: GeneralRepository): ResponseImpl<CreateTicketResponseData> {
        return repository.createTicket(description, uploadFileHooks)
    }
}
