package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.repositories.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.CreateComment
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.MarkTicketAsRead
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.SetPushToken

internal class LocalDataVerifier(private val fileResolver: FileResolver) {

    /**
     * Checks whether [command] is considered empty
     */
    fun isLocalCommandEmpty(command: CommandEntity): Boolean {
        return when (command.commandType) {
            CreateComment.ordinal -> {
                command.comment.isNullOrEmpty()
                    && (command.attachments.isNullOrEmpty() || !command.attachments.any { isLocalFileExists(it.uri) })
                    && command.rating == null
            }
            MarkTicketAsRead.ordinal -> {
                command.ticketId == null
            }
            SetPushToken.ordinal -> {
                command.token == null || command.tokenType == null
            }
            else -> false
        }
    }

    private fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}