package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.core.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class AccountStore(initialAccount: Account) {

    private val accountState: MutableStateFlow<Account> = MutableStateFlow(initialAccount)

    fun accountStateFlow(): StateFlow<Account> = accountState

    fun setAccount(account: Account) {
        accountState.value = account
    }

    fun getAccount(): Account {
        return accountState.value
    }

    //TODO check it
    fun removeUsers(users: List<String>) {
        val account = getAccount()
        if (account is Account.V3) {
            val usersToRemove = account.users.filter { users.contains(it.userId) }
            setAccount(account.copy(users = account.users.toMutableList().apply { removeAll(usersToRemove) }))
        }
    }
}