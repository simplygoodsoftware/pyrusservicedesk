package net.papirus.pyrusservicedesk.sdk.updates

internal interface UpdateSubscriber {
    fun <T: UpdateBase> onUpdateReceived(update: T)
    fun getUpdateTypes(): Set<UpdateType>
}