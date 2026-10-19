package app.luma.caller.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import app.luma.caller.InCallActivity

class LumaInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        CallHub.service = this
    }

    override fun onDestroy() {
        CallHub.service = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onCallAdded(call: Call) {
        CallHub.add(applicationContext, call)
        val state = CallHub.primary.value
        if (call.state == Call.STATE_RINGING) {
            CallNotifications.showIncoming(
                this,
                state?.title ?: "Incoming call",
                "Incoming call" + (state?.sim?.let { " · SIM $it" } ?: "")
            )
        }
        openCallScreen()
    }

    override fun onCallRemoved(call: Call) {
        CallHub.remove(call)
        CallNotifications.cancel(this)
    }

    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallHub.onAudioState(audioState)
    }

    private fun openCallScreen() {
        try {
            startActivity(
                Intent(this, InCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } catch (e: Exception) {
            // Background launch can be blocked; the full-screen notification covers that case.
        }
    }
}
