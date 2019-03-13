package net.papirus.pyrusservicedesk.sdk.updates

import net.papirus.pyrusservicedesk.sdk.data.Comment

internal class GetConversationUpdate(
    val comments: List<Comment>? = null,
    error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.ConversationReceived
}