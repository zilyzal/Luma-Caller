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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val KEYS = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@Composable
fun DialKey(digit: String, letters: String, size: Dp, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .size(size)
            .glass(CircleShape, fill = 0.14f)
            .pressable(onLongClick = onLongClick, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LText(digit, 30, FontWeight.Medium)
        Box(Modifier.height(12.dp), contentAlignment = Alignment.Center) {
            if (letters.isNotEmpty()) LText(letters, 10, FontWeight.SemiBold, Color.White.copy(alpha = 0.7f), spacing = 2f)
        }
    }
}

@Composable
fun DialPad(size: Dp, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in KEYS) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for ((digit, letters) in row) {
                    DialKey(
                        digit, letters, size,
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
        val key = if (compact) 64.dp else 78.dp
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
            DialPad(key, onKey = { onNumber(number + it) })
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            CallRow(sims, enabled = number.isNotEmpty(), onCall = onCall)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(112.dp))
        }
        if (number.isNotEmpty()) {
            Box(
                Modifier
                    .padding(top = if (compact) 24.dp else 56.dp, end = 20.dp)
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .pressable(onLongClick = { onNumber("") }, onClick = { onNumber(number.dropLast(1)) }),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Backspace, size = 26.dp)
            }
        }
    }
}

@Composable
private fun CallRow(sims: List<PhoneAccountHandle>, enabled: Boolean, onCall: (PhoneAccountHandle?) -> Unit) {
    val alpha = if (enabled) 1f else 0.5f
    if (sims.size >= 2) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SimButton("SIM 1", SimGreen, enabled, alpha) { onCall(sims[0]) }
            SimButton("SIM 2", SimBlue, enabled, alpha) { onCall(sims[1]) }
        }
    } else {
        Box(
            Modifier
                .size(68.dp)
                .gradientFill(SimGreen, CircleShape)
                .pressable(enabled = enabled) { onCall(sims.firstOrNull()) },
            contentAlignment = Alignment.Center
        ) {
            LumaIcon(LIcon.Call, size = 30.dp, strokeWidth = 1.9f)
        }
    }
}

@Composable
private fun SimButton(label: String, colors: List<Color>, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
    Row(
        Modifier
            .width(112.dp)
            .height(50.dp)
            .gradientFill(colors, RoundedCornerShape(25.dp))
            .pressable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LumaIcon(LIcon.Call, size = 20.dp, strokeWidth = 2f, tint = Color.White.copy(alpha = alpha))
        Spacer(Modifier.width(8.dp))
        LText(label, 15, FontWeight.Bold, Color.White.copy(alpha = alpha))
    }
}
