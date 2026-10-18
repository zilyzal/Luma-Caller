package app.luma.caller.data

import android.provider.CallLog

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val photoUri: String?
)

data class CallEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val photoUri: String?,
    val type: Int,
    val date: Long,
    val sim: Int?
) {
    val missed: Boolean get() = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE
    val title: String get() = name ?: number
}

data class ThreadItem(
    val id: Long,
    val address: String,
    val name: String?,
    val photoUri: String?,
    val snippet: String,
    val date: Long,
    val unread: Int
) {
    val title: String get() = name ?: address
}

data class Msg(
    val id: Long,
    val body: String,
    val date: Long,
    val outgoing: Boolean
)

/** Compares phone numbers by their last digits so "+98 912..." matches "0912...". */
fun normalizeNumber(n: String): String = n.filter { it.isDigit() }.takeLast(9)
