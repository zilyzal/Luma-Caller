package app.luma.caller.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

object Sims {
    /** SIM-backed calling accounts in slot order; empty if permission is missing. */
    fun handles(ctx: Context): List<PhoneAccountHandle> {
        if (ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return try {
            val telecom = ctx.getSystemService(TelecomManager::class.java)
            val all = telecom.callCapablePhoneAccounts
            val sims = all.filter {
                telecom.getPhoneAccount(it)?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true
            }
            sims.ifEmpty { all }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** 1-based SIM number for a call-log account id, or null when unknown. */
    fun indexOf(ctx: Context, accountId: String?): Int? {
        if (accountId.isNullOrEmpty()) return null
        val idx = handles(ctx).indexOfFirst { it.id == accountId }
        return if (idx >= 0) idx + 1 else null
    }

    fun place(ctx: Context, number: String, handle: PhoneAccountHandle?) {
        if (ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return
        val telecom = ctx.getSystemService(TelecomManager::class.java)
        val extras = Bundle()
        if (handle != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        try {
            telecom.placeCall(Uri.fromParts("tel", number, null), extras)
        } catch (e: SecurityException) {
            // Permission revoked while the call was being placed.
        }
    }
}
