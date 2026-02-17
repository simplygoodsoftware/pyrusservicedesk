package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R

/**
 * Dialog for choosing actions on pending comments
 */
internal class ErrorCommentActionsDialog: DialogFragment(), View.OnClickListener {

    private lateinit var retry: TextView
    private lateinit var delete: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val view = LayoutInflater
            .from(activity as Context)
            .inflate(R.layout.psd_dialog_comment_actions, null, false)

        retry = view.findViewById(R.id.retry)
        delete = view.findViewById(R.id.delete)

        val localizedContext = injector().resourceContextWrapper.createLocalizedContext(activity as Context)

        retry.text = localizedContext.resources.getString(R.string.psd_retry)
        delete.text = localizedContext.resources.getString(R.string.psd_delete)

        retry.setOnClickListener(this)
        delete.setOnClickListener(this)

        val dialogBuilder = AlertDialog.Builder(activity as Context)
            .setView(view)

        return dialogBuilder.create()
    }

    override fun onClick(v: View?) {
        val router = injector().router
        val key = requireArguments().getString(KEY_OBSERVER)!!
        when(v) {
            retry -> router.sendResult(key, ErrorCommentAction.RETRY)
            delete -> router.sendResult(key, ErrorCommentAction.DELETE)
        }
        dismiss()
    }

    internal companion object {

        private const val KEY_OBSERVER = "KEY_OBSERVER"

        enum class ErrorCommentAction { DELETE, RETRY }

        fun newInstance(uuid: String): ErrorCommentActionsDialog {
            val fragment = ErrorCommentActionsDialog()
            fragment.arguments = bundleOf(KEY_OBSERVER to uuid)
            return fragment
        }

    }
}