package net.papirus.pyrusservicedesk.broadcasts

import android.content.BroadcastReceiver
import android.content.IntentFilter

internal abstract class ReceiverBase: BroadcastReceiver() {
    abstract fun getIntentFilter(): IntentFilter
}