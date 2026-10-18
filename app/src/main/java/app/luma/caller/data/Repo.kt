package app.luma.caller.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Telephony
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import app.luma.caller.telecom.Sims

class Repo(private val ctx: Context) {
    private val resolver: ContentResolver get() = ctx.contentResolver

    private fun has(permission: String) =
        ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun query(sort: String, limit: Int) = Bundle().apply {
        putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sort)
        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
    }

    fun isDefaultSmsApp(): Boolean = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName

    fun contacts(): List<Contact> {
        if (!has(Manifest.permission.READ_CONTACTS)) return emptyList()
        val out = ArrayList<Contact>()
        val seen = HashSet<Long>()
        try {
            resolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_URI),
                null,
                null,
                Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    if (!seen.add(id)) continue
                    val name = c.getString(1) ?: continue
                    val number = c.getString(2) ?: continue
                    out.add(Contact(id, name, number, c.getString(3)))
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return out
    }

    fun index(contacts: List<Contact>): Map<String, Contact> {
        val map = HashMap<String, Contact>(contacts.size * 2)
        for (c in contacts) map.putIfAbsent(normalizeNumber(c.number), c)
        return map
    }

    fun lookup(number: String): Contact? {
        if (number.isBlank() || !has(Manifest.permission.READ_CONTACTS)) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            resolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) Contact(0, c.getString(0) ?: number, number, c.getString(1)) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun calls(index: Map<String, Contact>): List<CallEntry> {
        if (!has(Manifest.permission.READ_CALL_LOG)) return emptyList()
        val out = ArrayList<CallEntry>()
        try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.PHONE_ACCOUNT_ID
                ),
                query("${CallLog.Calls.DATE} DESC", 300),
                null
            )?.use { c ->
                while (c.moveToNext() && out.size < 300) {
                    val number = c.getString(1)?.takeIf { it.isNotBlank() } ?: "Unknown"
                    val contact = index[normalizeNumber(number)]
                    out.add(
                        CallEntry(
                            id = c.getLong(0),
                            number = number,
                            name = contact?.name ?: c.getString(2)?.takeIf { it.isNotBlank() },
                            photoUri = contact?.photoUri,
                            type = c.getInt(3),
                            date = c.getLong(4),
                            sim = Sims.indexOf(ctx, c.getString(5))
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return out
    }

    fun threads(index: Map<String, Contact>): List<ThreadItem> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        val firsts = LinkedHashMap<Long, ThreadItem>()
        val unread = HashMap<Long, Int>()
        try {
            resolver.query(
                Sms.CONTENT_URI,
                arrayOf(Sms._ID, Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY, Sms.DATE, Sms.TYPE, Sms.READ),
                query("${Sms.DATE} DESC", 2500),
                null
            )?.use { c ->
                while (c.moveToNext()) {
                    val tid = c.getLong(1)
                    if (c.getInt(6) == 0 && c.getInt(5) == Sms.MESSAGE_TYPE_INBOX) {
                        unread[tid] = (unread[tid] ?: 0) + 1
                    }
                    if (firsts.containsKey(tid)) continue
                    val address = c.getString(2)?.takeIf { it.isNotBlank() } ?: continue
                    val contact = index[normalizeNumber(address)]
                    firsts[tid] = ThreadItem(
                        id = tid,
                        address = address,
                        name = contact?.name,
                        photoUri = contact?.photoUri,
                        snippet = c.getString(3) ?: "",
                        date = c.getLong(4),
                        unread = 0
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return firsts.values.map { it.copy(unread = unread[it.id] ?: 0) }
    }

    fun messages(threadId: Long): List<Msg> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        val out = ArrayList<Msg>()
        try {
            resolver.query(
                Sms.CONTENT_URI,
                arrayOf(Sms._ID, Sms.BODY, Sms.DATE, Sms.TYPE),
                "${Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Sms.DATE} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    out.add(
                        Msg(
                            id = c.getLong(0),
                            body = c.getString(1) ?: "",
                            date = c.getLong(2),
                            outgoing = c.getInt(3) != Sms.MESSAGE_TYPE_INBOX
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return out
    }

    fun threadIdFor(address: String, threads: List<ThreadItem>): Long? {
        val key = normalizeNumber(address)
        return threads.firstOrNull { normalizeNumber(it.address) == key }?.id
    }

    fun markRead(threadId: Long) {
        if (!isDefaultSmsApp()) return
        try {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            resolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString())
            )
        } catch (e: Exception) {
            // Best effort: the unread badge simply stays until the next refresh.
        }
    }

    @Suppress("DEPRECATION")
    fun sendSms(address: String, body: String): Boolean {
        if (!has(Manifest.permission.SEND_SMS) || body.isBlank() || address.isBlank()) return false
        return try {
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(body)
            manager.sendMultipartTextMessage(address, null, parts, null, null)
            if (isDefaultSmsApp()) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                resolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
