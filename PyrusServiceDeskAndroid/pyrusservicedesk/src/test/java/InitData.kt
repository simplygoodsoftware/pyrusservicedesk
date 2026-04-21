import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest

internal object InitData {

    const val TEST_TICKET_ID = 1L
    const val TEST_COMMAND_MARK_TICKET_IS_READ_ID = "a16c1c7c-a301-4eae-b9de-b47816b42533"

    const val TEST_INSTANCE_ID = "instanceId"
    const val TEST_APP_ID = "xZlr1Zf0pZZE43NfjXfY10OvEKwkKLRCO~PYF7SjID-Tp-7sK5EAuWqgOfrCQNOdDUHrZhHlBaqcdzj2ULgf9e~ciFudXo9ff1Y9cx0oXaTGziZKANoCLbWceaF-5g1VAQpfcg=="
    val userInternalV1 = UserInternal(
        userId = TEST_INSTANCE_ID,
        appId = TEST_APP_ID
    )

    val markTicketIsReadRequest = SyncRequest.Command.MarkTicketAsRead(
        localId = -1,
        commandId = TEST_COMMAND_MARK_TICKET_IS_READ_ID,
        userId = if (userInternalV1.userId == TEST_INSTANCE_ID) null else userInternalV1.userId,
        appId = TEST_APP_ID,
        creationTime = System.currentTimeMillis(),
        ticketId = TEST_TICKET_ID,
    )

    val setPushTokenRequest = SyncRequest.Command.SetPushToken(
        localId = -2,
        commandId = "3ec22d08-1ae1-475a-a36d-e96ffbc54e74",
        userId = if (userInternalV1.userId == TEST_INSTANCE_ID) null else userInternalV1.userId,
        appId = TEST_APP_ID,
        creationTime = System.currentTimeMillis(),
        token = "testToken",
        tokenType = "android"
    )

    val createCommentRequest = SyncRequest.Command.CreateComment(
        localId = -3,
        commandId = "b5c9c6b0-5584-4f6b-a8c4-0017998db26e",
        userId = if (userInternalV1.userId == TEST_INSTANCE_ID) null else userInternalV1.userId,
        appId = TEST_APP_ID,
        creationTime = System.currentTimeMillis(),
        requestNewTicket = false,
        ticketId = TEST_TICKET_ID,
        comment = "testComment",
        attachments = null,
        rating = null,
        ratingComment = null,
        extraFields = null,
    )

    val calcOperatorTimeRequest = SyncRequest.Command.CalcOperatorTime(
        localId = -4,
        commandId = "b5c9c6b0-5584-4f6b-a8c4-0017998db261",
        userId = if (userInternalV1.userId == TEST_INSTANCE_ID) null else userInternalV1.userId,
        appId = TEST_APP_ID,
        creationTime = System.currentTimeMillis(),
        ticketId = TEST_TICKET_ID,
    )

}