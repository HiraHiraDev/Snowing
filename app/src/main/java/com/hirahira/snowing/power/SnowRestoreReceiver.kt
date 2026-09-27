package com.hirahira.snowing.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hirahira.snowing.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Brings snow back after a reboot or an app update, if the user left it on.
 * Both broadcasts are exempt from the background foreground-service start
 * restrictions, and `specialUse` is allowed from BOOT_COMPLETED.
 */
class SnowRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        val snowSwitch = context.appContainer.snowSwitch
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                snowSwitch.reconcile()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
