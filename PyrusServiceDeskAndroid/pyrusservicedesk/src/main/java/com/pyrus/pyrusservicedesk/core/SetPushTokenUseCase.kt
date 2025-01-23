package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.SetPushTokenCallback
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager.Companion.S_NO_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SetPushTokenUseCase(
    private val accountStore: AccountStore,
    private val coreScope: CoroutineScope,
    private val preferencesManager: PreferencesManager,
    private val repository: Repository,
) {

    fun invoke(
        token: String?,
        callback: SetPushTokenCallback?,
        tokenType: String,
    ) {
        val account = accountStore.getAccount()
        val firstUserId = account.getUserId()
        when {
            calculateSkipTokenRegister(firstUserId) -> callback?.onResult(Exception("Too many requests. Maximum once every $SET_PUSH_TOKEN_TIMEOUT minutes."))
            token == null -> callback?.onResult(Exception("token is null"))
            else -> {
                updateTokenTime(firstUserId, System.currentTimeMillis())

                for (user in account.getUsers()) {
                    sendPushToken(user, token, tokenType, callback)
                }
            }
        }
    }

    private fun sendPushToken(
        user: User,
        token: String,
        tokenType: String,
        callback: SetPushTokenCallback?,
    ) {
        coreScope.launch {
            val setPushTokenTry = repository.setPushToken(
                user = UserInternal(user.userId, user.appId),
                token = token,
                tokenType = tokenType
            )
            if (!setPushTokenTry.isSuccess()) {
                withContext(Dispatchers.Main) {
                    callback?.onResult(Exception(setPushTokenTry.error))
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    callback?.onResult(null)
                }
            }
        }
    }

    private fun calculateSkipTokenRegister(userId: String): Boolean {
        val currentTime = System.currentTimeMillis()

        val lastUserTime = preferencesManager.getLastTokenRegisterMap()[userId]
        if (lastUserTime != null) {
            return currentTime - lastUserTime < SET_PUSH_TOKEN_TIMEOUT * MILLISECONDS_IN_MINUTE
        }

        val tokenTimeList = preferencesManager.getTokenRegisterTimeList()

        val nWithinFiveMin: Int = tokenTimeList.count { time ->
            currentTime - time < SET_PUSH_TOKEN_TIMEOUT * MILLISECONDS_IN_MINUTE
        }
        return nWithinFiveMin >= SET_PUSH_TOKEN_TIMES_WITHIN_TIMEOUT
    }

    private fun updateTokenTime(userId: String, time: Long) {
        val timeMap = HashMap(preferencesManager.getLastTokenRegisterMap())
        val timeList = ArrayList<Long>(preferencesManager.getTokenRegisterTimeList())

        timeMap[userId] = time

        while (timeList.size >= SET_PUSH_TOKEN_TIMES_WITHIN_TIMEOUT) {
            timeList.removeAt(0)
        }
        timeList.add(time)

        preferencesManager.setLastTokenRegisterMap(timeMap)
        preferencesManager.setTokenRegisterTimeList(timeList)
    }

    companion object {
        private const val SET_PUSH_TOKEN_TIMES_WITHIN_TIMEOUT = 5 // in minute
        private const val SET_PUSH_TOKEN_TIMEOUT = 5 // Minutes
    }

}