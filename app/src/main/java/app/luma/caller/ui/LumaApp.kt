package app.luma.caller.ui

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.luma.caller.Launch
import app.luma.caller.LumaViewModel
import app.luma.caller.data.Contact
import app.luma.caller.data.Msg
import app.luma.caller.data.Perms
import app.luma.caller.data.normalizeNumber
import app.luma.caller.telecom.CallHub
import app.luma.caller.telecom.Sims

private data class ChatTarget(
    val address: String,
    val title: String,
    val photo: String?,
    val isNew: Boolean = false
)

@Composable
fun LumaApp(launch: Launch?, openCall: () -> Unit, vm: LumaViewModel = viewModel()) {
    val ctx = LocalContext.current
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val calls by vm.calls.collectAsStateWithLifecycle()
    val threads by vm.threads.collectAsStateWithLifecycle()
    val version by vm.version.collectAsStateWithLifecycle()
    val liveCall by CallHub.primary.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Keypad) }
    var dial by rememberSaveable { mutableStateOf("") }
    var skipSetup by rememberSaveable { mutableStateOf(false) }
    var chat by remember { mutableStateOf<ChatTarget?>(null) }
    var messages by remember { mutableStateOf<List<Msg>>(emptyList()) }
    var resumeTick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
        vm.refresh()
    }

    val sims = remember(resumeTick) { Sims.handles(ctx) }
    val needsSetup = remember(resumeTick) {
        Perms.missing(ctx).isNotEmpty() || !Perms.dialerHeld(ctx) || !Perms.smsHeld(ctx)
    }

    fun nameFor(address: String): Contact? {
        val key = normalizeNumber(address)
        return contacts.firstOrNull { normalizeNumber(it.number) == key }
    }

    fun openChat(address: String, title: String? = null, photo: String? = null) {
        val c = nameFor(address)
        chat = ChatTarget(address, title ?: c?.name ?: address, photo ?: c?.photoUri)
        tab = Tab.Messages
    }

    LaunchedEffect(launch?.id) {
        val l = launch ?: return@LaunchedEffect
        l.tab?.let { name -> Tab.entries.firstOrNull { it.name == name }?.let { tab = it } }
        l.dial?.let {
            dial = it
            tab = Tab.Keypad
            chat = null
        }
        l.sms?.let { openChat(it) }
    }

    LaunchedEffect(chat?.address, chat?.isNew, version) {
        val c = chat
        messages = if (c == null || c.address.isBlank()) {
            emptyList()
        } else {
            val id = vm.threadIdFor(c.address)
            if (id == null) emptyList() else vm.messages(id)
        }
    }

    BackHandler(enabled = chat != null) {
        chat = null
        vm.refresh()
    }

    if (needsSetup && !skipSetup) {
        SetupScreen(onChanged = { resumeTick++; vm.refresh() }, onSkip = { skipSetup = true })
        return
    }

    val unread = remember(threads) { threads.sumOf { it.unread } }
    val bannerPad = if (liveCall != null) 48.dp else 0.dp

    LumaBackground {
        val current = chat
        if (current != null) {
            ChatScreen(
                title = current.title,
                address = current.address,
                photoUri = current.photo,
                messages = messages,
                isNew = current.isNew,
                onAddressChange = { chat = current.copy(address = it, title = it) },
                onSend = { text ->
                    vm.send(current.address, text)
                    if (current.isNew) {
                        chat = current.copy(isNew = false, title = nameFor(current.address)?.name ?: current.address)
                    }
                },
                onCall = { Sims.place(ctx, current.address, null) },
                onBack = {
                    chat = null
                    vm.refresh()
                }
            )
        } else {
            Box(Modifier.fillMaxSize().padding(top = bannerPad)) {
                when (tab) {
                    Tab.Keypad -> KeypadScreen(
                        number = dial,
                        sims = sims,
                        onNumber = { dial = it },
                        onCall = { handle ->
                            if (dial.isNotEmpty()) {
                                Sims.place(ctx, dial, handle)
                                dial = ""
                            }
                        },
                        onAddContact = {
                            ctx.startActivity(
                                Intent(ContactsContract.Intents.Insert.ACTION)
                                    .setType(ContactsContract.RawContacts.CONTENT_TYPE)
                                    .putExtra(ContactsContract.Intents.Insert.PHONE, dial)
                            )
                        }
                    )
                    Tab.Recents -> RecentsScreen(
                        calls = calls,
                        onCall = { e -> Sims.place(ctx, e.number, e.sim?.let { sims.getOrNull(it - 1) }) },
                        onMessage = { e -> openChat(e.number, e.title, e.photoUri) }
                    )
                    Tab.Messages -> MessagesScreen(
                        threads = threads,
                        onOpen = { t -> openChat(t.address, t.title, t.photoUri) },
                        onNew = { chat = ChatTarget("", "", null, isNew = true) }
                    )
                    Tab.Contacts -> ContactsScreen(
                        contacts = contacts,
                        onCall = { c -> Sims.place(ctx, c.number, null) },
                        onMessage = { c -> openChat(c.number, c.name, c.photoUri) }
                    )
                }
            }
            BottomBar(
                selected = tab,
                unread = unread,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
            )
            val live = liveCall
            if (live != null) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .gradientFill(SimGreen, RoundedCornerShape(20.dp))
                        .pressable(onClick = openCall)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LumaIcon(LIcon.Call, size = 18.dp, strokeWidth = 2f)
                    Spacer(Modifier.width(10.dp))
                    LText("Return to call · ${live.title}", 14, FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}
