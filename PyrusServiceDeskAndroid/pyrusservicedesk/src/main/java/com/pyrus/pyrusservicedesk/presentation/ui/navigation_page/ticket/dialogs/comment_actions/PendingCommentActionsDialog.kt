package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.utils.getViewModelWithActivityScope

/**
 * Dialog for choosing actions on pending comments
 */
internal class PendingCommentActionsDialog: DialogFragment(), View.OnClickListener {

    private val sharedModel: PendingCommentActionSharedViewModel by getViewModelWithActivityScope(
        PendingCommentActionSharedViewModel::class.java)

    private lateinit var retry: View
    private lateinit var delete: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val view = LayoutInflater
            .from(activity as Context)
            .inflate(R.layout.psd_dialog_comment_actions, null, false)

        retry = view.findViewById(R.id.retry)
        delete = view.findViewById(R.id.delete)

        retry.setOnClickListener(this)
        delete.setOnClickListener(this)

        val dialogBuilder = AlertDialog.Builder(activity as Context)
            .setView(view)

        return dialogBuilder.create()
    }

    override fun onClick(v: View?) {
        when(v){
            retry -> sharedModel.onRetryClicked()
            delete -> sharedModel.onDeleteClicked()
        }
        dismiss()
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        sharedModel.onCancelled()
    }
}