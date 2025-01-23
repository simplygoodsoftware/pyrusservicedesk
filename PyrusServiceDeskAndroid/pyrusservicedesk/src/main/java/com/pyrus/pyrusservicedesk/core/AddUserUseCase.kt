package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AddUserUseCase(
    private val accountStore: AccountStore,
    private val repository: Repository,
    private val coreScope: CoroutineScope,
) {

    fun addUser(user: User) {
        coreScope.launch {
            accountStore.addUser(user)
            repository.getTicketsInfo(true)
        }
    }

}