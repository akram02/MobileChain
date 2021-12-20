package be.xbd.chain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import be.xbd.chain.server.KtorBackgroundService
import be.xbd.chain.utils.log

class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context) == ServiceState.STARTED) {
            Intent(context, KtorBackgroundService::class.java).also {
                it.action = Actions.START.name
                log("Starting the service")
                context.startForegroundService(it)
            }
        }
    }
}