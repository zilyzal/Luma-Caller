package app.luma.caller.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import app.luma.caller.InCallActivity
import app.luma.caller.R

object CallNotifications {
    private const val CHANNEL = "luma_incoming"
    private const val ID = 4201
    const val ACTION_ANSWER = "app.luma.caller.ANSWER"
    const val ACTION_DECLINE = "app.luma.caller.DECLINE"

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(CHANNEL, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun broadcast(ctx: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            code,
            Intent(ctx, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun showIncoming(ctx: Context, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        ensureChannel(nm)
        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(ctx, R.drawable.ic_stat_luma)
        val notification = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_luma)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(Notification.Action.Builder(icon, "Decline", broadcast(ctx, ACTION_DECLINE, 1)).build())
            .addAction(Notification.Action.Builder(icon, "Answer", broadcast(ctx, ACTION_ANSWER, 2)).build())
            .build()
        try {
            nm.notify(ID, notification)
        } catch (e: SecurityException) {
            // Notification permission missing; the call screen is still launched directly.
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CallNotifications.ACTION_ANSWER -> {
                CallHub.answer()
                context.startActivity(
                    Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            CallNotifications.ACTION_DECLINE -> CallHub.reject()
        }
        CallNotifications.cancel(context)
    }
}
