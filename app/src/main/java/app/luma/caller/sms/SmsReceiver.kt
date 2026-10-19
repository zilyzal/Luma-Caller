package app.luma.caller.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import app.luma.caller.MainActivity
import app.luma.caller.R
import app.luma.caller.data.Repo
import kotlinx.coroutines.flow.MutableStateFlow

/** Lets screens know the SMS store changed while the app is open. */
object Bus {
    val smsTick = MutableStateFlow(0)
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val address = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val date = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, date)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            // Not the default SMS app (or storage unavailable): still alert the user.
        }

        notifyMessage(context, address, body)
        Bus.smsTick.value = Bus.smsTick.value + 1
    }

    private fun notifyMessage(ctx: Context, address: String, body: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val title = Repo(ctx).lookup(address)?.name ?: address
        val open = PendingIntent.getActivity(
            ctx,
            address.hashCode(),
            Intent(ctx, MainActivity::class.java)
                .setAction(Intent.ACTION_SENDTO)
                .setData(Uri.fromParts("sms", address, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            nm.notify(address.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted.
        }
    }

    private companion object {
        const val CHANNEL = "luma_messages"
    }
}
