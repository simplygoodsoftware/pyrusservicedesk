package com.pyrus.pyrusservicedesk.core

import android.util.Log
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager

internal class UpdateUserUseCase(
    private val accountStore: AccountStore,
    private val preferencesManager: PreferencesManager,
) {

    fun updateUser() {
        val currentUserId = preferencesManager.getCurrentUserId()
        val newUserId = accountStore.getAccount().getUserId()
        val willRefresh = currentUserId != newUserId
        PLog.d(TAG, "SDDBG updateUser: currentUserId=$currentUserId newUserId=$newUserId -> ${if (willRefresh) "refresh()" else "SKIP refresh (same userId, no forced sync on open)"}")
        Log.d(TAG, "SDDBG updateUser: currentUserId=$currentUserId newUserId=$newUserId willRefresh=$willRefresh")
        if (willRefresh) {
            PyrusServiceDesk.refresh()
            newUserId?.let { preferencesManager.saveCurrentUserId(it) }
        }
    }

    private companion object {
        private const val TAG = "UpdateUserUseCase"
    }

}