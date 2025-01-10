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
}