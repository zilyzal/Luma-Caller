package app.luma.caller.ui

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

/** Short label for lists: time today, "Yesterday", weekday this week, otherwise a date. */
fun formatWhen(ms: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    if (sameDay(now, then)) return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, then)) return "Yesterday"
    val days = (now.timeInMillis - ms) / 86_400_000L
    if (days < 7) return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(ms))
    return DateFormat.getDateInstance(DateFormat.SHORT).format(Date(ms))
}

fun formatClock(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

fun formatDay(ms: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    if (sameDay(now, then)) return "Today"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameDay(yesterday, then)) return "Yesterday"
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
}

fun formatDuration(totalSeconds: Long): String {
    val s = if (totalSeconds < 0) 0 else totalSeconds
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%02d:%02d", m, sec)
}

/** Groups digits for display; leaves anything with letters or a plus sign untouched. */
fun prettyNumber(n: String): String {
    val d = n.filter { it.isDigit() }
    if (n.startsWith("+") || d.length < 7 || n.any { it.isLetter() }) return n
    return when (d.length) {
        10 -> "${d.substring(0, 3)} ${d.substring(3, 6)} ${d.substring(6)}"
        11 -> "${d.substring(0, 4)} ${d.substring(4, 7)} ${d.substring(7)}"
        else -> n
    }
}
