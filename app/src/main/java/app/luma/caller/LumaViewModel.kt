package app.luma.caller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.luma.caller.data.CallEntry
import app.luma.caller.data.Contact
import app.luma.caller.data.Msg
import app.luma.caller.data.Repo
import app.luma.caller.data.ThreadItem
import app.luma.caller.sms.Bus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LumaViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(app)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _calls = MutableStateFlow<List<CallEntry>>(emptyList())
    val calls: StateFlow<List<CallEntry>> = _calls

    private val _threads = MutableStateFlow<List<ThreadItem>>(emptyList())
    val threads: StateFlow<List<ThreadItem>> = _threads

    /** Bumped whenever stored messages may have changed, so open chats reload. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    init {
        viewModelScope.launch {
            Bus.smsTick.collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repo.contacts()
            val index = repo.index(all)
            _contacts.value = all
            _calls.value = repo.calls(index)
            _threads.value = repo.threads(index)
            _version.value = _version.value + 1
        }
    }

    suspend fun messages(threadId: Long): List<Msg> = withContext(Dispatchers.IO) {
        repo.markRead(threadId)
        repo.messages(threadId)
    }

    fun threadIdFor(address: String): Long? = repo.threadIdFor(address, _threads.value)

    fun send(address: String, body: String, subId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.sendSms(address, body, subId)) refresh()
        }
    }

    fun addContact(name: String, number: String, done: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.addContact(name, number)
            if (ok) refresh()
            withContext(Dispatchers.Main) { done(ok) }
        }
    }
}
