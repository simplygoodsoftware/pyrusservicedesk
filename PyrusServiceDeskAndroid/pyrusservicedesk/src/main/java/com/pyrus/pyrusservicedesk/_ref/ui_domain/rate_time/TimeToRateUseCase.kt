package com.pyrus.pyrusservicedesk._ref.ui_domain.rate_time

import android.util.Log
import com.pyrus.pyrusservicedesk.BuildConfig
import com.pyrus.pyrusservicedesk.sdk.updates.Preferences

internal class TimeToRateUseCase(
    private val prefs: Preferences
) {

    fun isTimeToRate(): Boolean {
        val recentRateTime = prefs.getRecentRatingTime()

        if (prefs.getCreatedTicketsCount() < 3) {
            return false
        }
        else if (recentRateTime == 0L) {
            prefs.setRecentRatingTime(System.currentTimeMillis())
            return true
        }

        val isVersionRated = prefs.getRecentRatingAppCode() == BuildConfig.MAIN_APP_VERSION_CODE
        val timeoutExpired = (System.currentTimeMillis() - prefs.getRecentRatingTime()
            >= RATING_APP_OFFER_TIMEOUT + RATING_APP_TIMEOUT_SPREAD * Math.random())

        val isTimeToRate =  timeoutExpired && !isVersionRated
        if (isTimeToRate) {
            prefs.setRecentRatingTime(System.currentTimeMillis())
        }
        return isTimeToRate
    }

    fun onAppRated() {
        prefs.setRecentRatingAppCode(BuildConfig.MAIN_APP_VERSION_CODE)
    }

    companion object {
        private const val RATING_APP_OFFER_TIMEOUT: Long = 30 * 24 * 60 * 60 * 1000L // 30 days in milliseconds
        private const val RATING_APP_TIMEOUT_SPREAD: Long = 5 * 24 * 60 * 60 * 1000L // 5 days in milliseconds
    }

}