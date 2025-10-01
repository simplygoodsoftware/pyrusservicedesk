package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AddUserUseCase(
    private val accountStore: AccountStore,
    private val repository: SdRepository,
    private val coreScope: CoroutineScope,
    private val addUserEventBus: AddUserEventBus,
) {

    fun addUser(user: User) {
        coreScope.launch(Dispatchers.IO) {
            addUserEventBus.setFilter(user)
            val userIdAdded = accountStore.addUser(user)
            if (userIdAdded) repository.getTicketsInfo(true)
        }
    }

}