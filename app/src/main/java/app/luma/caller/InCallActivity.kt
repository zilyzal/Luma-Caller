package app.luma.caller

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.luma.caller.telecom.CallHub
import app.luma.caller.telecom.CallNotifications
import app.luma.caller.telecom.CallUi
import app.luma.caller.telecom.Ringer
import app.luma.caller.ui.InCallScreen
import app.luma.caller.ui.IncomingScreen
import app.luma.caller.ui.LumaBackground
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            CallRoot(onFinish = { finish() }, openMain = ::openMain)
        }
    }

    private fun openMain(number: String?, tab: String?) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (number != null) {
            intent.action = Intent.ACTION_SENDTO
            intent.data = Uri.fromParts("sms", number, null)
        }
        if (tab != null) intent.putExtra(MainActivity.EXTRA_TAB, tab)
        startActivity(intent)
    }
}

@Composable
private fun CallRoot(onFinish: () -> Unit, openMain: (String?, String?) -> Unit) {
    val call by CallHub.primary.collectAsStateWithLifecycle()
    val muted by CallHub.muted.collectAsStateWithLifecycle()
    val speaker by CallHub.speaker.collectAsStateWithLifecycle()
    var last by remember { mutableStateOf<CallUi?>(null) }

    LaunchedEffect(call) {
        if (call != null) {
            last = call
        } else {
            delay(900)
            onFinish()
        }
    }

    val shown = call ?: last
    if (shown == null) {
        LumaBackground {}
        return
    }

    if (call?.ringing == true) {
        IncomingScreen(
            call = shown,
            onAnswer = { CallHub.answer() },
            onDecline = { CallHub.reject() },
            onMessage = {
                Ringer.stop()
                openMain(shown.number, null)
            },
            onSilence = { Ringer.stop() }
        )
    } else {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        InCallScreen(
            call = shown,
            ended = call == null,
            muted = muted,
            speaker = speaker,
            onMute = { CallHub.toggleMute() },
            onSpeaker = { CallHub.toggleSpeaker() },
            onDigit = { CallHub.dtmf(it) },
            onAddCall = { openMain(null, null) },
            onMessage = { openMain(shown.number, null) },
            onContacts = { openMain(null, "Contacts") },
            onEnd = {
                CallHub.hangUp()
                CallNotifications.cancel(ctx)
            }
        )
    }
}
