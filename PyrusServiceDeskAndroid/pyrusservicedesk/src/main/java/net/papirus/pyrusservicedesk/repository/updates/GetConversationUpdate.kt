package net.papirus.pyrusservicedesk.repository.updates

import net.papirus.pyrusservicedesk.repository.data.Comment

internal class GetConversationUpdate(
    val comments: List<Comment>? = null,
    error: UpdateError? = null)
    : UpdateBase(error) {

    override val type: UpdateType = UpdateType.ConversationReceived
}