package com.goeno.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet

class GoenoVpnService : VpnService() {

    private var v2rayPoint: V2RayPoint? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val configJson = intent?.getStringExtra("CONFIG_JSON") ?: return START_NOT_STICKY
        startForegroundNotification()
        startTunnel(configJson)
        return START_STICKY
    }

    private fun startTunnel(configJson: String) {
        val builder = Builder()
            .setSession("Goeno")
            .setMtu(1500)
            .addAddress("10.0.0.2", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addDisallowedApplication(packageName)

        val vpnInterface = builder.establish() ?: return

        v2rayPoint = Libv2ray.newV2RayPoint(object : V2RayVPNServiceSupportsSet {
            override fun setup(conf: String): Long = 0
            override fun prepare(): Long = 0
            override fun shutdown(): Long = 0
            override fun protect(socket: Long): Boolean = this@GoenoVpnService.protect(socket.toInt())
            override fun onEmitStatus(status: Long, msg: String?): Long = 0
        }, false)

        v2rayPoint?.configureFileContent = configJson
        v2rayPoint?.domainName = "1.1.1.1:443"
        v2rayPoint?.runLoop(false)
    }

    private fun stopVpn() {
        try {
            v2rayPoint?.stopLoop()
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val channelId = "goeno_vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Goeno VPN Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Goeno VPN Connected")
            .setContentText("Smart auto-routing active / اتصال امن و هوشمند برقرار است")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
    }
}
