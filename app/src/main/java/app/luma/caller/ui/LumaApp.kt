package app.luma.caller.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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

    var pendingName by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun doSave(name: String, number: String) {
        vm.addContact(name, number) { ok ->
            Toast.makeText(ctx, if (ok) "Saved $name" else "Couldn't save contact", Toast.LENGTH_SHORT).show()
        }
    }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val p = pendingName
        pendingName = null
        if (granted && p != null) doSave(p.first, p.second)
        else if (p != null) Toast.makeText(ctx, "Contacts permission is needed to save", Toast.LENGTH_SHORT).show()
    }
    fun saveContact(name: String, number: String) {
        if (ctx.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            doSave(name, number)
        } else {
            pendingName = name to number
            writeLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
        vm.refresh()
    }

    val sims = remember(resumeTick) { Sims.handles(ctx) }
    val needsSetup = remember(resumeTick) {
        Perms.missing(ctx).isNotEmpty() || !Perms.dialerHeld(ctx) || !Perms.smsHeld(ctx)
    }

    val byNumber = remember(contacts) {
        HashMap<String, Contact>(contacts.size * 3).also { m ->
            for (c in contacts) for (n in c.numbers) m.putIfAbsent(normalizeNumber(n), c)
        }
    }
    fun nameFor(address: String): Contact? = byNumber[normalizeNumber(address)]

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

    val bannerPad = if (liveCall != null) 48.dp else 0.dp

    LumaBackground {
        val lastChat = remember { arrayOfNulls<ChatTarget>(1) }
        if (chat != null) lastChat[0] = chat
        AnimatedContent(
            targetState = chat != null,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(spring(0.8f, 300f)) { it / 3 } + fadeIn(tween(260)) + scaleIn(spring(0.8f, 300f), 0.94f)) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(260), 0.92f))
                } else {
                    (fadeIn(tween(260)) + scaleIn(spring(0.8f, 300f), 0.92f)) togetherWith
                        (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(200)))
                }.using(SizeTransform(clip = false))
            },
            label = "chat"
        ) { inChat ->
        val current = if (inChat) lastChat[0] else null
        Box(Modifier.fillMaxSize()) {
        if (current != null) {
            ChatScreen(
                title = current.title,
                address = current.address,
                photoUri = current.photo,
                messages = messages,
                isNew = current.isNew,
                onAddressChange = { chat = current.copy(address = it, title = it) },
                simCount = sims.size,
                onSend = { text, simIdx ->
                    vm.send(current.address, text, simIdx?.let { Sims.subscriptionId(ctx, sims.getOrNull(it)) })
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
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(spring(0.75f, 260f)) { dir * it / 4 } + fadeIn(tween(280, 60)) + scaleIn(spring(0.8f, 260f), 0.94f)) togetherWith
                            (slideOutHorizontally(spring(0.9f, 400f)) { -dir * it / 5 } + fadeOut(tween(160)) + scaleOut(tween(220), 0.94f))
                    },
                    label = "tabs"
                ) { shownTab ->
                when (shownTab) {
                    Tab.Keypad -> KeypadScreen(
                        number = dial,
                        savedName = if (dial.count { it.isDigit() } >= 7) nameFor(dial)?.name else null,
                        sims = sims,
                        onNumber = { dial = it },
                        onCall = { handle ->
                            if (dial.isNotEmpty()) {
                                Sims.place(ctx, dial, handle)
                                dial = ""
                            }
                        },
                        onAddContact = { name -> saveContact(name, dial) }
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
            }
            BottomBar(
                selected = tab,
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
                        .pressable(shape = RoundedCornerShape(20.dp), onClick = openCall)
                        .gradientFill(SimGreen, RoundedCornerShape(20.dp))
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
    }
}
