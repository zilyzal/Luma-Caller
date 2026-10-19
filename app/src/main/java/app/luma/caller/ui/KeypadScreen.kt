package app.luma.caller.ui

import android.telecom.PhoneAccountHandle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val KEYS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("*", "0", "#")
)

@Composable
fun DialKey(digit: String, size: Dp, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(size)
            .pressable(shape = CircleShape, onLongClick = onLongClick, onClick = onClick)
            .glass(CircleShape, fill = 0.14f),
        contentAlignment = Alignment.Center
    ) {
        if (digit == "*") AsteriskIcon(dp = size * 0.3f)
        else LText(digit, 32, FontWeight.Normal, align = TextAlign.Center)
    }
}

@Composable
fun DialPad(size: Dp, gap: Dp, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in KEYS) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (digit in row) {
                    DialKey(
                        digit, size,
                        onClick = { onKey(digit) },
                        onLongClick = if (digit == "0") ({ onKey("+") }) else null
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadScreen(
    number: String,
    sims: List<PhoneAccountHandle>,
    onNumber: (String) -> Unit,
    onCall: (PhoneAccountHandle?) -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().statusBarsPadding()) {
        val compact = maxHeight < 700.dp
        val key = if (compact) 66.dp else 80.dp
        val gap = if (compact) 14.dp else 18.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(if (compact) 24.dp else 56.dp))

            // Number display; the delete icon only appears once something is typed.
            Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 56.dp), contentAlignment = Alignment.Center) {
                LText(
                    number,
                    size = when {
                        number.length > 16 -> 24
                        number.length > 12 -> 30
                        else -> 40
                    },
                    weight = FontWeight.Light,
                    maxLines = 1,
                    align = TextAlign.Center,
                    spacing = 1f
                )
            }
            Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                if (number.isNotEmpty()) {
                    LText(
                        "Add to contacts", 14, FontWeight.Medium, SoftBlue,
                        modifier = Modifier.pressable(onClick = onAddContact)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            DialPad(key, gap, onKey = { onNumber(number + it) })
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            CallRow(sims, key, gap, enabled = number.isNotEmpty(), onCall = onCall)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(112.dp))
        }
        if (number.isNotEmpty()) {
            Box(
                Modifier
                    .padding(top = if (compact) 24.dp else 56.dp, end = 20.dp)
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .pressable(shape = CircleShape, onLongClick = { onNumber("") }, onClick = { onNumber(number.dropLast(1)) }),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Backspace, size = 26.dp)
            }
        }
    }
}

@Composable
private fun CallRow(sims: List<PhoneAccountHandle>, size: Dp, gap: Dp, enabled: Boolean, onCall: (PhoneAccountHandle?) -> Unit) {
    val alpha = if (enabled) 1f else 0.5f
    if (sims.size >= 2) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CallCircle(size, SimGreen, "1", enabled, alpha) { onCall(sims[0]) }
            Spacer(Modifier.size(size))
            CallCircle(size, SimBlue, "2", enabled, alpha) { onCall(sims[1]) }
        }
    } else {
        CallCircle(size, SimGreen, null, enabled, alpha) { onCall(sims.firstOrNull()) }
    }
}

@Composable
private fun CallCircle(size: Dp, colors: List<Color>, sim: String?, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .pressable(shape = CircleShape, enabled = enabled, onClick = onClick)
            .gradientFill(colors, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LumaIcon(LIcon.Call, size = 28.dp, strokeWidth = 1.9f, tint = Color.White.copy(alpha = alpha))
            if (sim != null) {
                Spacer(Modifier.width(3.dp))
                LText(sim, 15, FontWeight.Bold, Color.White.copy(alpha = alpha))
            }
        }
    }
}
