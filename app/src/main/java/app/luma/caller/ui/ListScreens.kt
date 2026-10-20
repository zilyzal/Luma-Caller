package app.luma.caller.ui

import android.provider.CallLog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luma.caller.data.CallEntry
import app.luma.caller.data.Contact
import app.luma.caller.data.canonNumber
import app.luma.caller.data.looksNumeric
import app.luma.caller.data.ThreadItem

private val ListBottomPadding = 120.dp

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LText(text, 16, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun RecentsScreen(
    calls: List<CallEntry>,
    simCount: Int,
    onCall: (CallEntry) -> Unit,
    onCallWithSim: (CallEntry, Int) -> Unit,
    onMessage: (CallEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var chooser by remember { mutableStateOf<CallEntry?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(calls, query) {
        if (query.isBlank()) calls
        else {
            val q = query.trim()
            if (looksNumeric(q)) {
                val cq = canonNumber(q)
                calls.filter { canonNumber(it.number).contains(cq) }
            } else {
                calls.filter { it.title.contains(q, true) }
            }
        }
    }
    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchField(query, { query = it }, "Search calls and contacts", Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth())
        if (shown.isEmpty()) {
            EmptyState(if (calls.isEmpty()) "No recent calls" else "No matches")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = ListBottomPadding)) {
                items(shown, key = { it.id }) { call ->
                    CallRow(call, onCall, onMessage, onHold = if (simCount >= 2) ({ chooser = call }) else null)
                }
            }
        }
    }
    chooser?.let { entry ->
        SimChooser(
            title = prettyNumber(entry.title),
            onDismiss = { chooser = null },
            onPick = { idx ->
                chooser = null
                onCallWithSim(entry, idx)
            }
        )
    }
    }
}

@Composable
private fun SimChooser(title: String, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
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
            LText("Call with", 20, FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            LText(title, 15, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SimOption("SIM 1", SimGreen, Modifier.weight(1f)) { onPick(0) }
                SimOption("SIM 2", SimBlue, Modifier.weight(1f)) { onPick(1) }
            }
        }
    }
}

@Composable
private fun SimOption(label: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .height(56.dp)
            .pressable(shape = shape, onClick = onClick)
            .gradientFill(colors, shape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LumaIcon(LIcon.Call, size = 20.dp, strokeWidth = 2f)
        Spacer(Modifier.width(8.dp))
        LText(label, 16, FontWeight.Bold)
    }
}

@Composable
private fun CallRow(call: CallEntry, onCall: (CallEntry) -> Unit, onMessage: (CallEntry) -> Unit, onHold: (() -> Unit)?) {
    val label = when (call.type) {
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Declined"
        else -> "Incoming"
    } + (call.sim?.let { " · SIM $it" } ?: "")
    Row(
        Modifier.fillMaxWidth().height(72.dp).pressable(onLongClick = onHold) { onCall(call) }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(call.title, call.photoUri, 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            LText(prettyNumber(call.title), 17, FontWeight.SemiBold, if (call.missed) MissedPink else Color.White, maxLines = 1)
            LText(label, 13, color = Color.White.copy(alpha = 0.65f), maxLines = 1)
        }
        LText(formatWhen(call.date), 13, color = Color.White.copy(alpha = 0.65f))
        Box(Modifier.size(44.dp).pressable { onMessage(call) }, contentAlignment = Alignment.Center) {
            LumaIcon(LIcon.Message, size = 22.dp, tint = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
fun MessagesScreen(
    threads: List<ThreadItem>,
    onOpen: (ThreadItem) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(threads, query) {
        if (query.isBlank()) threads
        else {
            val q = query.trim()
            if (looksNumeric(q)) {
                val cq = canonNumber(q)
                threads.filter { canonNumber(it.address).contains(cq) || it.snippet.contains(q) }
            } else {
                threads.filter { it.title.contains(q, true) || it.snippet.contains(q, true) }
            }
        }
    }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(query, { query = it }, "Search messages", Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(48.dp).pressable(shape = CircleShape, onClick = onNew).gradientFill(Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LumaIcon(LIcon.Compose, size = 22.dp, strokeWidth = 1.9f)
            }
        }
        if (shown.isEmpty()) {
            EmptyState(if (threads.isEmpty()) "No messages yet" else "No matches")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = ListBottomPadding)) {
                items(shown, key = { it.id }) { t -> ThreadRow(t, onOpen) }
            }
        }
    }
}

@Composable
private fun ThreadRow(t: ThreadItem, onOpen: (ThreadItem) -> Unit) {
    val weight = if (t.unread > 0) FontWeight.Bold else FontWeight.SemiBold
    Row(
        Modifier.fillMaxWidth().height(78.dp).pressable { onOpen(t) }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(t.title, t.photoUri, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            LText(prettyNumber(t.title), 17, weight, maxLines = 1)
            LText(t.snippet, 14, color = Color.White.copy(alpha = 0.66f), maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LText(formatWhen(t.date), 12, color = Color.White.copy(alpha = 0.66f))
            Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                if (t.unread > 0) {
                    Box(
                        Modifier
                            .height(20.dp)
                            .width(if (t.unread > 9) 28.dp else 20.dp)
                            .background(Brush.linearGradient(Accent), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        LText(if (t.unread > 99) "99+" else t.unread.toString(), 12, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onCall: (Contact) -> Unit,
    onMessage: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(contacts, query) {
        if (query.isBlank()) contacts
        else {
            val q = query.trim()
            if (looksNumeric(q)) {
                val cq = canonNumber(q)
                contacts.filter { c -> c.canonNumbers.any { it.contains(cq) } }
            } else {
                val lq = q.lowercase()
                contacts.filter { it.nameLower.contains(lq) }
            }
        }
    }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        SearchField(query, { query = it }, "Search contacts", Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth())
        if (shown.isEmpty()) {
            EmptyState(if (contacts.isEmpty()) "No contacts" else "No matches")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = ListBottomPadding)) {
                items(shown, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().height(72.dp).pressable { onCall(c) }.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.name, c.photoUri, 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            LText(c.name, 17, FontWeight.SemiBold, maxLines = 1)
                            LText(prettyNumber(c.number), 13, color = Color.White.copy(alpha = 0.65f), maxLines = 1)
                        }
                        Box(Modifier.size(44.dp).pressable { onMessage(c) }, contentAlignment = Alignment.Center) {
                            LumaIcon(LIcon.Message, size = 22.dp, tint = Color.White.copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }
    }
}
