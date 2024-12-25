package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.core.Account
import kotlinx.coroutines.flow.MutableStateFlow

internal class AccountStore(initialAccount: Account) {

    private val accountState: MutableStateFlow<Account> = MutableStateFlow(initialAccount)

    fun setAccount(account: Account) {
        accountState.value = account
    }

    fun getAccount(): Account {
        return accountState.value
    }
}