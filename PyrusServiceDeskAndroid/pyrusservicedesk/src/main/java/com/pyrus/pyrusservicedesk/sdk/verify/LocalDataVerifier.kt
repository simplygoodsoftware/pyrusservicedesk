package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import androidx.core.net.toUri
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.CreateComment
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.MarkTicketAsRead
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType.SetPushToken

internal class LocalDataVerifier(private val fileResolver: FileResolver) {

    /**
     * Checks whether [command] is considered empty
     */
    fun isLocalCommandEmpty(command: CommandWithAttachmentsEntity): Boolean {
        return when (command.command.commandType) {
            CreateComment.ordinal -> {
                command.command.comment.isNullOrEmpty()
                    && (command.attachments.isNullOrEmpty() || !command.attachments.any { isLocalFileExists(it.uri.toUri()) })
                    && command.command.rating == null
            }
            MarkTicketAsRead.ordinal -> {
                command.command.ticketId == null
            }
            SetPushToken.ordinal -> {
                command.command.token == null || command.command.tokenType == null
            }
            else -> false
        }
    }

    private fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}