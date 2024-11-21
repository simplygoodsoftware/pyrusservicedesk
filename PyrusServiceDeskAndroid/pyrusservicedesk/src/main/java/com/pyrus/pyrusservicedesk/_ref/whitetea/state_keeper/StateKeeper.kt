package com.pyrus.pyrusservicedesk._ref.whitetea.state_keeper

import android.os.Bundle
import android.os.Parcelable

internal interface StateKeeper {
    fun <State: Parcelable> prepare(key: Any, state: State)

    fun save(bundle: Bundle)

    fun <State : Parcelable> get(key: Any) : State?
}