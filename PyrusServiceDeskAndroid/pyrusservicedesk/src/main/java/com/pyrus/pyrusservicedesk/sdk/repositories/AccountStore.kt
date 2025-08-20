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

    fun addUser(user: User): Boolean {
        return addUser(user, false)
    }

    fun addExtraUser(user: User): Boolean {
        return addUser(user, true)
    }

    private fun addUser(user: User, isExtra: Boolean): Boolean {
        val account = accountState.value
        if (account is Account.V3) {
            val newUserList =
                if (isExtra) account.extraUsers.toMutableList() else account.users.toMutableList()
            val currentUser = newUserList.find { it.userId == user.userId }
            if (currentUser == user) {
                return false
            }
            newUserList.removeAll { user.userId == it.userId && user.appId == it.appId }
            newUserList.add(user)
            val newAccount =
                if (isExtra) account.copy(extraUsers = newUserList) else account.copy(users = newUserList)
            setAccount(newAccount)
        }
        return true
    }

    fun cleanUsers() {
        val account = getAccount()
        if (account is Account.V3) {
            setAccount(account.copy(users = emptyList()))
        }
    }

    fun cleanExtraUsers() {
        val account = getAccount()
        if (account is Account.V3) {
            setAccount(account.copy(extraUsers = emptyList()))
        }
    }

    fun removeUsers(userIds: List<String>) {
        val account = getAccount()
        if (account is Account.V3) {
            val usersToRemove = account.users.filter { userIds.contains(it.userId) }
            val newUsers = account.users.toMutableList().apply { removeAll(usersToRemove) }
            val newAcc = account.copy(users = newUsers)
            setAccount(newAcc)
        }
    }
}