package com.pyrus.pyrusservicedesk._ref.whitetea.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal typealias Action<Dependencies, Message> = suspend CoroutineScope.(Dependencies) -> Flow<Message>?

internal interface Command<Dependencies, out Message> {
    val tag: String?
    val singleStrategy: Boolean
    val run: Action<Dependencies, Message>

    companion object {

        operator fun <Dependencies, Message> invoke(
            tag: String?,
            singleTagStrategy: Boolean,
            action: Action<Dependencies, Message>,
        ) = object : Command<Dependencies, Message> {
            override val tag: String? = tag
            override val singleStrategy: Boolean = singleTagStrategy
            override val run = action
        }

        fun <Dependencies, Message> single(
            f: suspend CoroutineScope.(Dependencies) -> Message
        ) = Builder().single(f)

        fun <Dependencies, Message> idle(
            f: suspend CoroutineScope.(Dependencies) -> Unit
        ) = Builder().idle<Dependencies, Message>(f)

        fun <Dependencies, Message> flow(
            f: suspend CoroutineScope.(Dependencies) -> Flow<Message>
        ) = Builder().flow(f)

        val onMain get() = Builder(Dispatchers.Main)
        val onIO get() = Builder(Dispatchers.IO)
        val onDefault get() = Builder(Dispatchers.Default)
        val onUnconfined get() = Builder(Dispatchers.Unconfined)

        class Builder(private val dispatcher: CoroutineDispatcher? = null) {

            private var debounceMs = 0
            private var tag: String? = null
            private var singleStrategy = false

            fun <Dependencies, Message> single(
                f: suspend CoroutineScope.(Dependencies) -> Message
            ) = withDispatcher<Dependencies, Message> { flowOf(f(it)) }

            fun <Dependencies, Message> idle(
                f: suspend CoroutineScope.(Dependencies) -> Unit
            ) = withDispatcher<Dependencies, Message> { f(it); null }

            fun <Dependencies, Message> flow(
                f: suspend CoroutineScope.(Dependencies) -> Flow<Message>
            ) = withDispatcher(f)

            fun setDebounce(debounce: Int) = this.apply { debounceMs = debounce }

            fun setTag(tag: String?) = this.apply { this.tag = tag }

            fun singleTagStrategy() = this.apply { this.singleStrategy = true }

            private fun <Dependencies, Message> withDispatcher(
                action: Action<Dependencies, Message>
            ) = Command<Dependencies, Message>(tag, singleStrategy) {
                if (dispatcher != null) withContext(dispatcher) { action(it) }
                else action(it)
            }
        }
    }
}

internal inline fun <Dependencies1, Message1, Dependencies2, Message2> Command<Dependencies1, Message1>.adapt(
    crossinline f1: (Flow<Message1>?) -> Flow<Message2>?,
    crossinline f2: (Dependencies2) -> Dependencies1,
): Command<Dependencies2, Message2> = Command(tag, singleStrategy) { d2 ->
    val d = f2(d2)
    val message = this@adapt.run(this, d)
    f1(message)
}

internal fun <Message1, Dependencies2, Message2> Command<Unit, Message1>.adapt(
    f: (Message1) -> Message2,
): Command<Dependencies2, Message2> = adapt({ flow -> flow?.map(f) }, { Unit })

internal inline fun <Dependencies1, Message, Dependencies2, Message2> Command<Dependencies1, Message>.adaptIdle(
    crossinline fa: (Dependencies2) -> Dependencies1
): Command<Dependencies2, Message2> = adapt({ null }, fa)