package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_1
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_2
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.API_VERSION_3
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.getFirstNSymbols
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk._ref.utils.map
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.CommandDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.UserDataDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.RepositoryMapper
import com.pyrus.pyrusservicedesk.sdk.response.ApiCallError
import com.pyrus.pyrusservicedesk.sdk.response.AuthorizationError
import com.pyrus.pyrusservicedesk.sdk.response.ResponseError
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit

private const val FAILED_AUTHORIZATION_ERROR_CODE = 403

/**
 * Web Repository implementation based on [Retrofit] library.
 *
 * @param instanceId UID of app instance. Generated installation id is used by default.
 * @param fileResolver helper for making upload file requests.
 */
@Keep
internal class RemoteStore(
    private val instanceId: String,
    private val fileResolver: FileResolver,
    private val fileManager: FileManager,
    private val api: ServiceDeskApi,
    private val account: Account,
) {

    private val apiFlag = "AAAAAAAAAAAU"

    /**
     * Provides tickets in single feed representation.
     */
//    suspend fun getFeed(keepUnread: Boolean): Try<CommentsDto> {
//        PLog.d(
//            TAG, "getFeed, " +
//                    "appId: ${account.appId.getFirstNSymbols(10)}, " +
//                    "userId: ${getUserId().getFirstNSymbols(10)}, " +
//                    "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
//                    "apiVersion: ${getVersion()}"
//        )
//        val commentsTry = api.getTicketFeed(
//            GetFeedBody(
//                account.appId,
//                getUserId(),
//                getSecurityKey(),
//                instanceId,
//                getVersion(),
//                keepUnread,
//                apiFlag
//            )
//        )
//        PLog.d(TAG, "getFeed, isSuccessful: ${commentsTry.isSuccess()}")
//        return commentsTry
//    }

//    suspend fun getTicket(ticketId: Int): Try<TicketDto> {
//        PLog.d(
//            TAG, "getTickets, " +
//                    "appId: ${account.appId.getFirstNSymbols(10)}, " +
//                    "userId: ${getUserId().getFirstNSymbols(10)}, " +
//                    "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
//                    "apiVersion: ${getVersion()}"
//        )
//        val ticketsTry = api.getTickets(
//            RequestBodyBase(
//                needFullInfo = true,
//                additionalUsers = getAdditionalUsers(),
//                authorId = getAuthorId(),
//                authorName = ConfigUtils.getUserName(),
//                appId = account.appId,
//                userId = getUserId(),
//                instanceId = getInstanceId(),
//                version = getVersion(),
//                apiSign = apiFlag,
//                securityKey = getSecurityKey(),
//                lastNoteId = TODO(),
//                commands = TODO(),
//            )
//        )
//        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
//        if (ticketsTry.isSuccess()) {
//            val ticket = ticketsTry.value.tickets
//                ?.find { it.ticketId == ticketId } ?: return Try.Failure(Exception("Ticket not found in response"))
//            return Try.Success(ticket)
//        }
//
////        return ticketsTry
//        TODO()
//    }

    /**
     * Provides available tickets.
     */
//    suspend fun getTickets(): Try<List<TicketDto>> {
//        val ticketsTry = api.getTickets(
//            RequestBodyBase(
//                needFullInfo = true,
//                additionalUsers = getAdditionalUsers(),
//                authorId = getAuthorId(),
//                authorName = ConfigUtils.getUserName(),
//                appId = account.appId,
//                userId = getUserId(),
//                instanceId = getInstanceId(),
//                version = getVersion(),
//                apiSign = apiFlag,
//                securityKey = getSecurityKey(),
//                lastNoteId = TODO(),
//                commands = TODO(),
//            )
//        )
//        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
//        return ticketsTry.map { it.tickets ?: emptyList() }
//    }

    suspend fun getAllData(commands: List<CommandDto>? = null): Try<TicketsInfo> {
        val v3 = account as Account.V3 // TODO
        PLog.d(
            TAG, "getAllData, " +
                    "appId: ${v3.firstAppId.getFirstNSymbols(10)}, " +
                    "userId: ${getUserId().getFirstNSymbols(10)}, " +
                    "instanceId: ${getInstanceId()?.getFirstNSymbols(10)}, " +
                    "apiVersion: ${getVersion()}"
        )
        val ticketsTry = api.getTickets(
            RequestBodyBase(
                needFullInfo = true,
                additionalUsers = getAdditionalUsers(),
                commands = null,
                authorId = getAuthorId(),
                authorName = ConfigUtils.getUserName(),
                appId = v3.firstAppId,
                userId = getUserId(),
                instanceId = getInstanceId(),
                version = getVersion(),
                apiSign = apiFlag,
                securityKey = getSecurityKey(),
                lastNoteId = TODO(),
            )
        )
        PLog.d(TAG, "getTickets, isSuccessful: ${ticketsTry.isSuccess()}")
        return ticketsTry.map { mapTickets(v3, it) }
    }


    private fun mapTickets(accountV3: Account.V3, ticketsDto: TicketsDto): TicketsInfo {
        val mapper = RepositoryMapper(account)
        val usersByAppId = accountV3.users.groupBy { it.appId }

        val ticketsByUserId = ticketsDto.tickets
            ?.map(mapper::map)
            ?.groupBy { it.userId } ?: error("tickets is null")

        val applications = ticketsDto.applications?.associateBy { it.appId } ?: error("applications is null")

        val ticketSetInfoList = usersByAppId.keys.map { appId ->
            val users = usersByAppId[appId] ?: emptyList()
            val tickets = ArrayList<FullTicket>()
            for (user in users) {
                val userTickets = ticketsByUserId[user.userId] ?: continue
                tickets += userTickets
            }
            val application = applications[appId]
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl
            TicketSetInfo(
                appId = appId,
                orgName = orgName ?: "",
                orgLogoUrl = orgLogoUrl,
                tickets = tickets,
            )
        }

        return TicketsInfo(ticketSetInfoList)
    }

    private fun getAdditionalUsers(): List<UserDataDto>? {
        val list = PyrusServiceDesk.ticketsListStateFlow.value.map { UserDataDto(it.appId, it.userId, "", null) }
        return list
    }


    /**
     * Appends [comment] to the ticket to comment feed.
     *
     * @param uploadFileHook is used for posting progress as well as checking cancellation signal.
     */
//    suspend fun addFeedComment(
//        comment: CommentDto,
//        uploadFileHook: UploadFileHook?
//    ): Try<AddCommentResponseData> {
//        // TODO wtf
//        return addComment(injector().localCommandsStore.getLastTicketId(), comment, uploadFileHook)
//    }

//    private suspend fun addComment(
//        ticketId: Int,
//        comment: CommentDto,
//        uploadFileHook: UploadFileHook?,
//    ): Try<AddCommentResponseData> {
//
//        var cament = comment
//        if (cament.hasAttachments()) {
//            // TODO reuse it
////            val uploadAttachmentsTry = uploadAttachments(cament.attachments!!, uploadFileHook)
////            if (!uploadAttachmentsTry.isSuccess()) {
////                return uploadAttachmentsTry
////            }
////            cament = cament.applyNewAttachments(uploadAttachmentsTry.value)
//        }
//        val addFeedCommentTry = api.addFeedComment(
//            AddCommentRequestBody(
//                account.appId,
//                getUserId(),
//                getSecurityKey(),
//                getInstanceId(),
//                getVersion(),
//                cament.body,
//                cament.attachments,
//                ConfigUtils.getUserName(),
//                cament.rating,
//                StaticRepository.EXTRA_FIELDS
//            )
//        )
//        PLog.d(TAG, "addComment, isSuccessful: ${addFeedCommentTry.isSuccess()}}")
//        return addFeedCommentTry
//    }

    private fun getUserId() = when(account) {
        is Account.V1 -> account.userId
        is Account.V2 -> account.userId
        is Account.V3 -> account.users.first().userId
    }

    private fun getVersion(): Int = when (account) {
        is Account.V1 -> API_VERSION_1
        is Account.V2 -> API_VERSION_2
        is Account.V3 -> API_VERSION_3
    }

    private fun getSecurityKey() = when (account) {
        is Account.V1 -> null
        is Account.V2 -> account.securityKey
        is Account.V3 -> null
    }

    private fun getInstanceId() = when (getVersion()) {
        API_VERSION_2 -> instanceId
        API_VERSION_3 -> instanceId
        else -> null
    }

    private fun getAuthorId() = when (account) {
        is Account.V3 -> account.authorId
        else -> null
    }

    private fun AttachmentDto.toRemoteAttachment(guid: String) = AttachmentDto(
        id,
        guid,
        type,
        name,
        bytesSize,
        isText,
        isVideo,
        localUri
    )

    private fun AddCommentResponseData.applyAttachments(
        attachments: List<AttachmentDto>?
    ): AddCommentResponseData {

        if (!mustBeConvertedToRemoteAttachments(attachments, attachmentIds)) {
            return this
        }
        return AddCommentResponseData(
            commentId,
            attachmentIds,
            applyRemoteIdsToAttachments(attachments!!, attachmentIds!!)
        )
    }

    private fun applyRemoteIdsToAttachments(
        sentAttachments: List<AttachmentDto>,
        remoteAttachmentIds: List<Int>,
    ): List<AttachmentDto> {
        val newAttachmentsList = mutableListOf<AttachmentDto>()
        sentAttachments.forEachIndexed { index, attachment ->
            newAttachmentsList.add(attachment.withRemoteId(remoteAttachmentIds[index]))
        }
        return newAttachmentsList
    }

    private fun AttachmentDto.withRemoteId(remoteId: Int) = AttachmentDto(
        remoteId,
        guid,
        type,
        name,
        bytesSize,
        isText,
        isVideo,
        localUri
    )

    private fun mustBeConvertedToRemoteAttachments(
        sentAttachments: List<AttachmentDto>?,
        remoteAttachmentIds: List<Int>?,
    ): Boolean {

        return !sentAttachments.isNullOrEmpty()
                && !remoteAttachmentIds.isNullOrEmpty()
                && sentAttachments.size == remoteAttachmentIds.size
    }

    private fun CommentDto.applyNewAttachments(newAttachments: List<AttachmentDto>): CommentDto {
        return CommentDto(commentId, body, isInbound, newAttachments, creationDate, author)
    }

    private fun <T> createError(response: Response<T>): ResponseError {
        return when (FAILED_AUTHORIZATION_ERROR_CODE) {
            response.code() -> {
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                        PyrusServiceDesk.onAuthorizationFailed?.run()
                    }
                }
                AuthorizationError(response.message())
            }

            else -> ApiCallError(response.message())
        }
    }

    companion object {
        private val TAG = RemoteStore::class.java.simpleName
    }

}