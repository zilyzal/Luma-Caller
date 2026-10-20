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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
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
    savedName: String?,
    sims: List<PhoneAccountHandle>,
    onNumber: (String) -> Unit,
    onCall: (PhoneAccountHandle?) -> Unit,
    onAddContact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }
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
                if (savedName != null) {
                    LText(savedName, 16, FontWeight.SemiBold, SoftBlue, maxLines = 1, modifier = Modifier.padding(horizontal = 56.dp))
                } else if (number.isNotEmpty()) {
                    LText(
                        "Add to contacts", 14, FontWeight.Medium, SoftBlue,
                        modifier = Modifier.pressable { showAdd = true }
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
                    .padding(top = if (compact) 24.dp else 56.dp, end = 8.dp)
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .pressable(shape = CircleShape, overlay = false, onLongClick = { onNumber("") }, onClick = { onNumber(number.dropLast(1)) }),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Backspace, size = 26.dp)
            }
        }
        if (showAdd) {
            AddContactDialog(
                number = number,
                onDismiss = { showAdd = false },
                onSave = { name ->
                    showAdd = false
                    onAddContact(name)
                }
            )
        }
    }
}

@Composable
private fun AddContactDialog(number: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000010))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .glass(RoundedCornerShape(32.dp), fill = 0.18f, base = 0.8f, depth = 1.3f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LText("New contact", 20, FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            LText(prettyNumber(number), 15, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .glass(RoundedCornerShape(26.dp), fill = 0.12f, base = 0.35f)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (name.isEmpty()) LText("Name", 17, color = Color.White.copy(alpha = 0.6f))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp, textDirection = TextDirection.Content),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressable(shape = RoundedCornerShape(24.dp), onClick = onDismiss)
                        .glass(RoundedCornerShape(24.dp), fill = 0.14f),
                    contentAlignment = Alignment.Center
                ) { LText("Cancel", 16, FontWeight.SemiBold) }
                val ok = name.isNotBlank()
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressable(shape = RoundedCornerShape(24.dp), enabled = ok) { onSave(name) }
                        .gradientFill(if (ok) Accent else listOf(Color(0x33FFFFFF), Color(0x1FFFFFFF)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) { LText("Save", 16, FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun CallRow(sims: List<PhoneAccountHandle>, size: Dp, gap: Dp, enabled: Boolean, onCall: (PhoneAccountHandle?) -> Unit) {
    val alpha = if (enabled) 1f else 0.5f
    if (sims.size >= 2) {
        // Two wide pills together span the full width of the keypad above them.
        val half = (size * 3 + gap * 2 - gap) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            CallPill(half, size, SimGreen, enabled, alpha) { onCall(sims[0]) }
            CallPill(half, size, SimBlue, enabled, alpha) { onCall(sims[1]) }
        }
    } else {
        CallCircle(size, SimGreen, enabled, alpha) { onCall(sims.firstOrNull()) }
    }
}

@Composable
private fun CallPill(width: Dp, height: Dp, colors: List<Color>, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(height / 2)
    Box(
        Modifier
            .size(width, height)
            .pressable(shape = shape, enabled = enabled, onClick = onClick)
            .gradientFill(colors, shape),
        contentAlignment = Alignment.Center
    ) {
        LumaIcon(LIcon.Call, size = 28.dp, strokeWidth = 1.9f, tint = Color.White.copy(alpha = alpha))
    }
}

@Composable
private fun CallCircle(size: Dp, colors: List<Color>, enabled: Boolean, alpha: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .pressable(shape = CircleShape, enabled = enabled, onClick = onClick)
            .gradientFill(colors, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        LumaIcon(LIcon.Call, size = 28.dp, strokeWidth = 1.9f, tint = Color.White.copy(alpha = alpha))
    }
}
