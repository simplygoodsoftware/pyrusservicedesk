package net.papirus.pyrusservicedesk.sdk.updates

internal abstract class UpdateBase(val error: UpdateError? = null) {

    abstract val type: UpdateType

    fun hasError() = error != null
}