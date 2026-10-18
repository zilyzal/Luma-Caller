package app.luma.caller.sms

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Required for the default-SMS role. MMS download is not implemented yet, so this is a no-op. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/** Required for the default-SMS role ("respond via message" from the call screen). */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
