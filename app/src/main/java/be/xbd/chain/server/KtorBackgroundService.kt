package be.xbd.chain.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import be.xbd.chain.*
import be.xbd.chain.common.domain.Blockchain
import be.xbd.chain.common.service.service.*
import be.xbd.chain.utils.log
import kotlinx.coroutines.*

class KtorBackgroundService: Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    companion object {
        var BLOCKCHAIN: Blockchain? = null
        var SERVER_SET: HashSet<String> = HashSet()
        var MY_SERVER_SET: HashSet<String> = HashSet()
        val PORT = "8080"
        fun cleanData() {
            BLOCKCHAIN = newBlockchainWithGenesisBlock()
        }
    }
    init {
        if (BLOCKCHAIN == null) {
            BLOCKCHAIN = newBlockchainWithGenesisBlock()
            cleanServerSet(SERVER_SET, PORT)
            MY_SERVER_SET = HashSet(SERVER_SET)
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        log("Some component want to bind with service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        if (intent != null) {
            log("using an intent with action ${intent.action}")
            when (intent.action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        }
        else {
            log(
                "with a null intent. It has been probably restarted by the system."
            )
        }

        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created.".uppercase())
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed".uppercase())
    }


    private fun continuousDataSync() {
        GlobalScope.launch {
            while (isServiceStarted) {
                MY_SERVER_SET = myServerSet(PORT)
                SERVER_SET.addAll(MY_SERVER_SET)
                collectAndMergeServer(SERVER_SET)
                mergeBlockchain(SERVER_SET, BLOCKCHAIN!!)
                delay(200)
            }
        }
    }

    private fun startService() {
        if (isServiceStarted) return
        Toast.makeText(this, "Mobile Chain is starting", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileChain::lock").apply {
                acquire()
            }
        }

        GlobalScope.launch {
            while (isServiceStarted) {
                try {
                    KtorServer.start()
                } catch (e: java.lang.Exception) {
                    println("Address already in use")
                }
                delay(1*60*1000)
            }
        }
        continuousDataSync()
    }

    private fun stopService() {
        Toast.makeText(this, "Mobile Chain is stopping", Toast.LENGTH_SHORT).show()
        GlobalScope.launch {
            KtorServer.stop()
        }
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {

        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }


    private fun createNotification(): Notification {
        val notificationChannelId = "MOBILE CHAIN SERVICE CHANNEL"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "Mobile Chain Service notification channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Mobile Chain Service channel"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0 or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder: Notification.Builder = Notification.Builder(this, notificationChannelId)


        return builder
            .setContentTitle("Mobile Chain")
            .setContentText("Service is running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setTicker("Mobile Chain Ticker text")
            .build()
    }
}