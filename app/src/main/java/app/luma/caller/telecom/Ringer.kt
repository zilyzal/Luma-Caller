package app.luma.caller.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Plays the ringtone and vibration while a call is ringing, honouring the ringer mode. */
object Ringer {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var running = false

    fun start(ctx: Context) {
        if (running) return
        val audio = ctx.getSystemService(AudioManager::class.java)
        val mode = audio.ringerMode
        if (mode == AudioManager.RINGER_MODE_SILENT) return
        running = true

        if (mode == AudioManager.RINGER_MODE_NORMAL) {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(ctx, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        }

        val v = ctx.getSystemService(Vibrator::class.java)
        if (v != null && v.hasVibrator()) {
            vibrator = v
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 800), 0))
        }
    }

    fun stop() {
        if (!running) return
        running = false
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }
}
