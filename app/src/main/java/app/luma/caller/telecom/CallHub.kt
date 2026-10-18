package app.luma.caller.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import app.luma.caller.data.Contact
import app.luma.caller.data.Repo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CallUi(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val state: Int,
    val connectTime: Long,
    val sim: Int?,
    val callCount: Int
) {
    val ringing: Boolean get() = state == Call.STATE_RINGING
    val onHold: Boolean get() = state == Call.STATE_HOLDING
    val active: Boolean get() = state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING
    val title: String get() = name ?: number
}

/** Single source of truth for live calls, fed by [LumaInCallService]. */
object CallHub {
    private val calls = ArrayList<Call>()
    private val callbacks = HashMap<Call, Call.Callback>()
    private val lookups = HashMap<String, Contact?>()
    private val main = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var foreground: Call? = null

    var service: InCallService? = null

    private val _primary = MutableStateFlow<CallUi?>(null)
    val primary: StateFlow<CallUi?> = _primary

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val _speaker = MutableStateFlow(false)
    val speaker: StateFlow<Boolean> = _speaker

    fun add(ctx: Context, call: Call) {
        appContext = ctx.applicationContext
        if (calls.contains(call)) return
        calls.add(call)
        val cb = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) = refresh()
            override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
        }
        callbacks[call] = cb
        call.registerCallback(cb)
        refresh()
    }

    fun remove(call: Call) {
        callbacks.remove(call)?.let { call.unregisterCallback(it) }
        calls.remove(call)
        refresh()
    }

    fun onAudioState(state: CallAudioState) {
        _muted.value = state.isMuted
        _speaker.value = state.route == CallAudioState.ROUTE_SPEAKER
    }

    @Suppress("DEPRECATION")
    private fun refresh() {
        val ctx = appContext
        val fg = calls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: calls.firstOrNull {
                it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING ||
                    it.state == Call.STATE_CONNECTING || it.state == Call.STATE_PULLING_CALL
            }
            ?: calls.firstOrNull()
        foreground = fg
        if (ctx != null) {
            if (calls.any { it.state == Call.STATE_RINGING }) Ringer.start(ctx) else Ringer.stop()
        }
        if (fg == null || ctx == null) {
            _primary.value = null
            return
        }
        val number = fg.details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: "Unknown"
        val contact = lookups.getOrPut(number) { Repo(ctx).lookup(number) }
        val account = fg.details?.accountHandle
        val sim = if (account == null) null else
            Sims.handles(ctx).indexOfFirst { it.id == account.id }.let { if (it >= 0) it + 1 else null }
        _primary.value = CallUi(
            number = number,
            name = contact?.name,
            photoUri = contact?.photoUri,
            state = fg.state,
            connectTime = fg.details?.connectTimeMillis ?: 0L,
            sim = sim,
            callCount = calls.size
        )
    }

    fun answer() {
        foreground?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun reject() {
        foreground?.reject(false, null)
    }

    fun hangUp() {
        foreground?.disconnect()
    }

    fun toggleHold() {
        val c = foreground ?: return
        @Suppress("DEPRECATION")
        if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
    }

    fun dtmf(digit: Char) {
        val c = foreground ?: return
        c.playDtmfTone(digit)
        main.postDelayed({ c.stopDtmfTone() }, 140)
    }

    @Suppress("DEPRECATION")
    fun toggleMute() {
        service?.setMuted(!_muted.value)
    }

    @Suppress("DEPRECATION")
    fun toggleSpeaker() {
        service?.setAudioRoute(
            if (_speaker.value) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
        )
    }
}
