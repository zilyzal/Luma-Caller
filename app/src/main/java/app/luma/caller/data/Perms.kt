package app.luma.caller.data

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager

object Perms {
    val core: Array<String> = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.WRITE_CALL_LOG)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun missing(ctx: Context): List<String> =
        core.filter { ctx.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

    fun dialerHeld(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            ctx.getSystemService(TelecomManager::class.java).defaultDialerPackage == ctx.packageName
        }

    fun smsHeld(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
        }

    fun dialerIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, ctx.packageName)
        }

    fun smsIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
        }
}
