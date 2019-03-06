package net.papirus.pyrusservicedesk.repository.updates

internal interface UpdateSubscriber {
    fun <T: UpdateBase> onUpdateReceived(update: T)
    fun getUpdateTypes(): Set<UpdateType>
}