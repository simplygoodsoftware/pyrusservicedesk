package com.pyrus.pyrusservicedesk._ref.whitetea.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

internal interface Actor<in Effect : Any, out Message : Any> {

    fun handleEffect(effect: Effect): Flow<Message>

}

internal inline fun <reified Eff1 : Any, Msg1 : Any, Eff2 : Any, reified Msg2 : Any>
    Actor<Eff1, Msg1>.adaptCast(): Actor<Eff2, Msg2> =
    adapt(
        effAdapter = { it as? Eff1 },
        msgAdapter = { it as? Msg2 }
    )

internal fun <Eff1 : Any, Msg1 : Any, Eff2 : Any, Msg2 : Any> Actor<Eff1, Msg1>.adapt(
    effAdapter: (Eff2) -> Eff1?,
    msgAdapter: (Msg1) -> Msg2? = { null }
): Actor<Eff2, Msg2> = object : Actor<Eff2, Msg2> {

    override fun handleEffect(effect: Eff2): Flow<Msg2> = effAdapter(effect)
        ?.let {
            handleEffect(effect = it).mapNotNull { msgAdapter(it) }
        }
        ?: emptyFlow()


}