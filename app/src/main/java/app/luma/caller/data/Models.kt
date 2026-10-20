package app.luma.caller.data

import android.provider.CallLog

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val photoUri: String?,
    /** Every number saved for this contact (the first is [number]). */
    val numbers: List<String> = listOf(number)
) {
    /** Search keys, computed once per contact instead of on every keystroke. */
    val canonNumbers: List<String> = numbers.map { canonNumber(it) }
    val nameLower: String = name.lowercase()
}

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

/**
 * Country-independent form of a number for searching: drops "+98", "0098", "98" and the trunk "0",
 * so "0912...", "+98 912..." and "912..." all become the same digits.
 */
fun canonNumber(raw: String): String {
    val plus = raw.trim().startsWith("+")
    var d = raw.filter { it.isDigit() }
    d = when {
        d.startsWith("0098") -> d.drop(4)
        (plus || d.length >= 4) && d.startsWith("98") -> d.drop(2)
        else -> d
    }
    return if (d.startsWith("0")) d.drop(1) else d
}

/** True when the text looks like a phone number being typed rather than a name. */
fun looksNumeric(q: String): Boolean =
    q.isNotBlank() && q.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' } && q.any { it.isDigit() }
