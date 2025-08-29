package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager

internal class UpdateUserUseCase(
    private val accountStore: AccountStore,
    private val preferencesManager: PreferencesManager,
) {

    fun updateUser() {
        val currentUserId = preferencesManager.getCurrentUserId()
        val newUserId = accountStore.getAccount().getUserId() //TODO kate почему-то тот же юзер
        if (currentUserId != newUserId) {
            PyrusServiceDesk.refresh()
            newUserId?.let { preferencesManager.saveCurrentUserId(it) }
        }
    }

}