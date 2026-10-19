package app.luma.caller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.caller.data.Msg

private sealed interface ChatRow {
    data class Day(val label: String) : ChatRow
    data class Bubble(val msg: Msg) : ChatRow
}

private fun toRows(messages: List<Msg>): List<ChatRow> {
    val rows = ArrayList<ChatRow>(messages.size + 8)
    var lastDay = ""
    for (m in messages) {
        val day = formatDay(m.date)
        if (day != lastDay) {
            rows.add(ChatRow.Day(day))
            lastDay = day
        }
        rows.add(ChatRow.Bubble(m))
    }
    return rows
}

@Composable
fun ChatScreen(
    title: String,
    address: String,
    photoUri: String?,
    messages: List<Msg>,
    isNew: Boolean,
    onAddressChange: (String) -> Unit,
    simCount: Int,
    onSend: (String, Int?) -> Unit,
    onCall: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val rows = remember(messages) { toRows(messages) }
    val listState = rememberLazyListState()
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty()) listState.scrollToItem(rows.size - 1)
    }

    Column(modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(64.dp)
                .glass(RoundedCornerShape(32.dp), fill = 0.14f, base = 0.45f)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).pressable(onClick = onBack), contentAlignment = Alignment.Center) {
                LumaIcon(LIcon.Back, size = 22.dp, strokeWidth = 2f)
            }
            if (isNew) {
                Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                    if (address.isEmpty()) LText("To: name or number", 16, color = Color.White.copy(alpha = 0.65f))
                    BasicTextField(
                        value = address,
                        onValueChange = onAddressChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textDirection = TextDirection.Content),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Avatar(title, photoUri, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    LText(prettyNumber(title), 16, FontWeight.SemiBold, maxLines = 1)
                    if (title != address) LText(prettyNumber(address), 12, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                }
            }
            Box(
                Modifier.size(44.dp).pressable(shape = CircleShape, enabled = address.isNotBlank(), onClick = onCall).gradientFill(SimGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Call, size = 20.dp, strokeWidth = 2f)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows.size) { i ->
                when (val row = rows[i]) {
                    is ChatRow.Day -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.glass(RoundedCornerShape(14.dp), fill = 0.1f).padding(horizontal = 14.dp, vertical = 5.dp)
                        ) { LText(row.label, 12, FontWeight.Medium, Color.White.copy(alpha = 0.8f)) }
                    }
                    is ChatRow.Bubble -> Bubble(row.msg)
                }
            }
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 58.dp)
                .glass(RoundedCornerShape(29.dp), fill = 0.14f, base = 0.5f)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) LText("Message", 16, color = Color.White.copy(alpha = 0.65f))
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    maxLines = 4,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, textDirection = TextDirection.Content),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val canSend = draft.isNotBlank() && address.isNotBlank()
            val off = listOf(Color(0x33FFFFFF), Color(0x1FFFFFFF))
            if (simCount >= 2) {
                SendButton(if (canSend) SimGreen else off, canSend) {
                    onSend(draft.trim(), 0)
                    draft = ""
                }
                Spacer(Modifier.width(8.dp))
                SendButton(if (canSend) SimBlue else off, canSend) {
                    onSend(draft.trim(), 1)
                    draft = ""
                }
            } else {
                SendButton(if (canSend) Accent else off, canSend) {
                    onSend(draft.trim(), null)
                    draft = ""
                }
            }
        }
    }
}

@Composable
private fun SendButton(colors: List<Color>, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .pressable(shape = CircleShape, enabled = enabled, onClick = onClick)
            .gradientFill(colors, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        LumaIcon(LIcon.Send, size = 20.dp, strokeWidth = 2.2f)
    }
}

private fun isRtlText(text: String): Boolean {
    for (ch in text) {
        if (!Character.isLetter(ch)) continue
        val d = Character.getDirectionality(ch)
        return d == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    }
    return false
}

@Composable
private fun Bubble(m: Msg) {
    // Each bubble lays itself out in the direction of its own text, so Arabic reads right-to-left.
    val rtl = remember(m.body) { isRtlText(m.body) }
    val dir = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    // Outgoing sits on the right, incoming on the left, regardless of the text direction.
    val outerAlign = if (m.outgoing) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxWidth(), contentAlignment = outerAlign) {
        CompositionLocalProvider(LocalLayoutDirection provides dir) {
            val big = 22.dp
            val small = 6.dp
            // tail corner stays on the sender's side (physical left/right)
            val tailRight = m.outgoing
            val shape = RoundedCornerShape(
                topStart = big, topEnd = big,
                bottomStart = if ((dir == LayoutDirection.Ltr) != tailRight) small else big,
                bottomEnd = if ((dir == LayoutDirection.Ltr) == tailRight) small else big
            )
            Column(
                Modifier
                    .widthIn(max = 290.dp)
                    .let {
                        if (m.outgoing) it.gradientFill(Accent, shape) else it.glass(shape, fill = 0.16f, base = 0.25f)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                LText(m.body, 16, align = TextAlign.Start)
                Spacer(Modifier.height(2.dp))
                LText(formatClock(m.date), 11, color = Color.White.copy(alpha = 0.65f), align = TextAlign.Start)
            }
        }
    }
}
