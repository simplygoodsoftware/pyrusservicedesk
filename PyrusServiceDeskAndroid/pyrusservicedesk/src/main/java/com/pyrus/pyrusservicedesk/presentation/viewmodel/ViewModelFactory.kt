package com.pyrus.pyrusservicedesk.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview.FilePreviewViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel

/**
 * Factory that provides view models.
 * @param arguments is optional arguments that are requested by some view models and should be provided by
 * the request side
 */
internal class ViewModelFactory(private val arguments: Intent, private val application: Application): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            SharedViewModel::class.java -> PyrusServiceDesk.injector().sharedViewModel as T
            FilePreviewViewModel::class.java -> FilePreviewViewModel(application, arguments) as T
            AttachFileSharedViewModel::class.java -> AttachFileSharedViewModel() as T
            PendingCommentActionSharedViewModel::class.java -> PendingCommentActionSharedViewModel() as T
            else -> throw IllegalStateException("View model for class $modelClass was not found")
        }
    }
}