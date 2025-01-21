package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.User
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

    fun addUser(user: User) {
        val account = accountState.value
        if (account is Account.V3) {
            val newUserList = account.users.toMutableList()
            newUserList.removeAll { user.userId == it.userId && user.appId == it.appId }
            newUserList.add(user)
            val newAccount = account.copy(users = newUserList)
            setAccount(newAccount)
        }
    }

    //TODO check it
    fun removeUsers(userIds: List<String>) {
        val account = getAccount()
        if (account is Account.V3) {
            val usersToRemove = account.users.filter { userIds.contains(it.userId) }
            setAccount(account.copy(users = account.users.toMutableList().apply { removeAll(usersToRemove) }))
        }
    }
}