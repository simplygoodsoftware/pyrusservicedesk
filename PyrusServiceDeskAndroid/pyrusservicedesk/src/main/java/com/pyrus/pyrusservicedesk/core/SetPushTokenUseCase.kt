package com.pyrus.pyrusservicedesk.core

import com.pyrus.pyrusservicedesk.SetPushTokenCallback
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager.Companion.S_NO_ID
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SetPushTokenUseCase(
    private val account: Account,
    private val coreScope: CoroutineScope,
    private val preferencesManager: PreferencesManager,
) {

    fun invoke(
        token: String?,
        callback: SetPushTokenCallback,
        tokenType: String,
    ) {
        // TODO
        if (true) return
        val userId = (account as? Account.V2)?.userId
        when {
            calculateSkipTokenRegister(userId) -> callback.onResult(Exception("Too many requests. Maximum once every $SET_PUSH_TOKEN_TIMEOUT minutes."))
            account.appId.isBlank() -> callback.onResult(Exception("AppId is not assigned"))
            account.instanceId.isBlank() -> callback.onResult(Exception("UserId is not assigned"))
            else -> {
                updateTokenTime(userId, System.currentTimeMillis())
                coreScope.launch {

                }

                TODO()
//                GlobalScope.launch {
//                    serviceDesk
//                        .requestFactory
//                        .getSetPushTokenRequest(token, tokenType)
//                        .execute(object : ResponseCallback<Unit> {
//                            override fun onSuccess(data: Unit) {
//                                callback.onResult(null)
//                            }
//
//                            override fun onFailure(responseError: ResponseError) {
//                                callback.onResult(responseError)
//                            }
//                        })
//                }
            }
        }
    }

    private fun calculateSkipTokenRegister(userId: String?): Boolean {
        val currentTime = System.currentTimeMillis()

        val lastUserTime = preferencesManager.getLastTokenRegisterMap()[userId?: S_NO_ID]
        if (lastUserTime != null) {
            return currentTime - lastUserTime < SET_PUSH_TOKEN_TIMEOUT * MILLISECONDS_IN_MINUTE
        }

        val tokenTimeList = preferencesManager.getTokenRegisterTimeList()

        val nWithinFiveMin: Int = tokenTimeList.count { time ->
            currentTime - time < SET_PUSH_TOKEN_TIMEOUT * MILLISECONDS_IN_MINUTE
        }
        return nWithinFiveMin >= SET_PUSH_TOKEN_TIMES_WITHIN_TIMEOUT
    }

    private fun updateTokenTime(userId: String?, time: Long) {
        val timeMap = HashMap(preferencesManager.getLastTokenRegisterMap())
        val timeList = ArrayList<Long>(preferencesManager.getTokenRegisterTimeList())

        timeMap[userId?: S_NO_ID] = time

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