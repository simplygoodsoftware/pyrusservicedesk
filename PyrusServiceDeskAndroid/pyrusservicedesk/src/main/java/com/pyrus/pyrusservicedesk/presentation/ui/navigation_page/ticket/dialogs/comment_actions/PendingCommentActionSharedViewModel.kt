package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

/**
 * ViewModel for interacting between ui of choosing pending comment action and the parent ui
 */
internal class PendingCommentActionSharedViewModel: ViewModel() {

    companion object{

        private const val ACTION_NONE = -1
        private const val ACTION_CANCELLED = 0
        private const val ACTION_RETRY = 1
        private const val ACTION_DELETE = 2

        /**
         * Checks whether choosing was cancelled
         */
        fun isCancelled(action: Int) = action == ACTION_CANCELLED

        /**
         * Checks whether retry was chosen
         */
        fun isRetryClicked(action: Int) = action == ACTION_RETRY

        /**
         * Checks whether delete was chosen
         */
        fun isDeleteClicked(action: Int) = action == ACTION_DELETE
    }

    private val selectedActionLiveData = MutableLiveData<Int>()

    /**
     * Callback to be invoked when user chooses to retry sending pending comment
     */
    fun onRetryClicked() {
        selectedActionLiveData.value = ACTION_RETRY
        selectedActionLiveData.postValue(ACTION_NONE)
    }

    /**
     * Callback to be invoked when user chooses to delete pending comment
     */
    fun onDeleteClicked() {
        selectedActionLiveData.value = ACTION_DELETE
        selectedActionLiveData.postValue(ACTION_NONE)
    }

    /**
     * Callback to be invoked when user decided no to perform any action on pending comment
     */
    fun onCancelled() {
        selectedActionLiveData.value = ACTION_CANCELLED
        selectedActionLiveData.postValue(ACTION_NONE)
    }

    /**
     * Provides live data of selected action on pending comment
     */
    fun getSelectedActionLiveData(): LiveData<Int> = selectedActionLiveData
}