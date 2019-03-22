package net.papirus.pyrusservicedesk

interface UnreadCounterChangedSubscriber {
    fun onUnreadCounterChanged(unreadCounter: Int)
}