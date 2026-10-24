package com.hirahira.snowing.power

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.overlay.StopStyle
import kotlinx.coroutines.launch

/**
 * "Stop the snow" from the notification. Some screens (Google account,
 * package installer) refuse every tap while any other app's window lies on
 * top, and the notification shade is the one place still reachable there.
 * See docs/adr/0003-sensitive-screens.md.
 */
class SnowOffReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                container.snowSwitch.turnOff(StopStyle.AT_ONCE)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SnowOffReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
