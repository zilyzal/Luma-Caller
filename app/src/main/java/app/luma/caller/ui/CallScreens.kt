package app.luma.caller.ui

import android.telecom.Call
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.luma.caller.telecom.CallUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CHEVRON = "M9 5l7 7-7 7"

@Composable
private fun rememberFullPhoto(uri: String?): ImageBitmap? {
    val ctx = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, uri) {
        value = if (uri == null) null else PhotoCache.load(ctx, uri, 1080)
    }
    return photo
}

/** Glass wallpaper for contacts without a photo; the photo itself, full screen, otherwise. */
@Composable
private fun CallBackdrop(photo: ImageBitmap?, content: @Composable BoxScope.() -> Unit) {
    if (photo == null) {
        LumaBackground(content = content)
    } else {
        Box(Modifier.fillMaxSize().background(LumaBg)) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0xB3050819),
                        0.32f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to Color(0xCC050819)
                    )
                )
            )
            content()
        }
    }
}

@Composable
private fun CallHeader(call: CallUi, subtitle: String, status: String, hasPhoto: Boolean) {
    Spacer(Modifier.height(if (hasPhoto) 40.dp else 96.dp))
    LText(subtitle, 15, FontWeight.Medium, Color.White.copy(alpha = 0.82f))
    Spacer(Modifier.height(14.dp))
    LText(prettyNumber(call.title), 46, FontWeight.Bold, align = TextAlign.Center, spacing = -1f, maxLines = 2)
    if (call.name != null) {
        Spacer(Modifier.height(6.dp))
        LText(prettyNumber(call.number), 17, color = Color.White.copy(alpha = 0.75f))
    }
    if (status.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        LText(status, 19, color = Color.White.copy(alpha = 0.9f), spacing = 1f)
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, colors: List<Color>?, icon: @Composable () -> Unit) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .let { if (colors == null) it.glass(CircleShape, fill = 0.16f, base = 0.2f) else it.gradientFill(colors, CircleShape) }
                .pressable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(8.dp))
        LText(label, 12, FontWeight.Medium, Color.White.copy(alpha = 0.9f), maxLines = 1)
    }
}

@Composable
fun IncomingScreen(
    call: CallUi,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onMessage: () -> Unit,
    onSilence: () -> Unit
) {
    val photo = rememberFullPhoto(call.photoUri)
    CallBackdrop(photo) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallHeader(call, "Incoming call" + (call.sim?.let { " · SIM $it" } ?: ""), "", photo != null)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton("Remind me", onSilence, null) { LumaIcon(LIcon.Snooze, size = 26.dp) }
                ActionButton("Decline", onDecline, DangerRed) {
                    LumaIcon(LIcon.Call, Modifier.rotate(135f), size = 26.dp, strokeWidth = 1.9f)
                }
                ActionButton("Message", onMessage, null) { LumaIcon(LIcon.Message, size = 26.dp) }
            }
            Spacer(Modifier.height(28.dp))
            SwipeToAnswer(onAnswer)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SwipeToAnswer(onAnswer: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(84.dp)
            .glass(RoundedCornerShape(42.dp), fill = 0.16f, base = 0.4f)
    ) {
        val handle = 66.dp
        val pad = 9.dp
        val maxPx = with(LocalDensity.current) { (maxWidth - handle - pad * 2).toPx() }
        val progress = if (maxPx > 0f) (offset.value / maxPx).coerceIn(0f, 1f) else 0f

        Row(
            Modifier
                .fillMaxSize()
                .padding(start = handle + pad * 2, end = 20.dp)
                .graphicsLayer { alpha = 1f - progress },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LText("Swipe to answer", 17, FontWeight.SemiBold, Color.White.copy(alpha = 0.9f), maxLines = 1)
            Spacer(Modifier.width(6.dp))
            LumaIcon(CHEVRON, size = 20.dp, strokeWidth = 2.2f)
        }

        Box(
            Modifier
                .padding(pad)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(handle)
                .gradientFill(SimGreen, CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxPx)) }
                    },
                    onDragStopped = {
                        if (offset.value > maxPx * 0.72f) {
                            offset.animateTo(maxPx)
                            onAnswer()
                        } else {
                            offset.animateTo(0f)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            LumaIcon(LIcon.Call, size = 30.dp, strokeWidth = 1.9f)
        }
    }
}

@Composable
private fun ControlButton(label: String, active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(76.dp)
                .glass(CircleShape, fill = if (active) 0.32f else 0.14f, base = 0.2f)
                .pressable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(8.dp))
        LText(label, 13, FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun InCallScreen(
    call: CallUi,
    ended: Boolean,
    muted: Boolean,
    speaker: Boolean,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onDigit: (Char) -> Unit,
    onAddCall: () -> Unit,
    onMessage: () -> Unit,
    onContacts: () -> Unit,
    onEnd: () -> Unit
) {
    val photo = rememberFullPhoto(call.photoUri)
    var showKeys by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val status = when {
        ended -> "Call ended"
        call.state == Call.STATE_ACTIVE ->
            if (call.connectTime > 0) formatDuration((now - call.connectTime) / 1000) else "Connected"
        call.state == Call.STATE_HOLDING -> "On hold"
        call.state == Call.STATE_RINGING -> "Ringing"
        call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING -> "Call ended"
        else -> "Calling…"
    }

    CallBackdrop(photo) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallHeader(call, if (call.sim != null) "SIM ${call.sim}" else "Phone call", status, photo != null)
            Spacer(Modifier.weight(1f))

            if (showKeys) {
                LText(typed, 28, FontWeight.Light, align = TextAlign.Center, maxLines = 1, modifier = Modifier.height(40.dp))
                DialPad(64.dp, onKey = {
                    typed += it
                    onDigit(it[0])
                })
                Spacer(Modifier.height(10.dp))
                LText("Hide keypad", 15, FontWeight.Medium, SoftBlue, modifier = Modifier.pressable { showKeys = false })
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ControlButton("Mute", muted, onMute) { LumaIcon(if (muted) LIcon.MicOff else LIcon.Mic, size = 30.dp) }
                    ControlButton("Keypad", false, { showKeys = true }) { DotsIcon(dp = 30.dp) }
                    ControlButton("Speaker", speaker, onSpeaker) { LumaIcon(LIcon.Speaker, size = 30.dp) }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ControlButton("Add call", false, onAddCall) { LumaIcon(LIcon.Plus, size = 30.dp) }
                    ControlButton("Message", false, onMessage) { LumaIcon(LIcon.Message, size = 30.dp) }
                    ControlButton("Contacts", false, onContacts) { LumaIcon(LIcon.Person, size = 30.dp) }
                }
            }

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(84.dp).gradientFill(DangerRed, CircleShape).pressable(onClick = onEnd),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Call, Modifier.rotate(135f), size = 34.dp, strokeWidth = 1.9f)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
