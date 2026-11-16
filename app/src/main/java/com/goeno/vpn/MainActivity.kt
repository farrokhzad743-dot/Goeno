package com.goeno.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var isConnected = false
    private lateinit var statusText: TextView
    private lateinit var connectBtn: Button

    private val fallbackNodes = listOf(
        SmartRouter.Node(
            name = "Goeno Reality Fast",
            host = "1.1.1.1",
            port = 443,
            xrayJsonConfig = "{\"inbounds\":[{\"port\":10808,\"protocol\":\"socks\"}],\"outbounds\":[{\"protocol\":\"vless\",\"settings\":{\"vnext\":[{\"address\":\"1.1.1.1\",\"port\":443,\"users\":[{\"id\":\"b831381d-6324-4d53-ad4f-8cda48b30811\",\"encryption\":\"none\"}]}]}}]}"
        )
    )

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            connectToBestServer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        }

        val titleText = TextView(this).apply {
            text = "Goeno VPN\nفیلترشکن هوشمند گوئنو"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, 64)
        }

        statusText = TextView(this).apply {
            text = getString(R.string.status_disconnected)
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 48)
        }

        connectBtn = Button(this).apply {
            text = getString(R.string.connect)
            textSize = 16f
            setBackgroundColor(android.graphics.Color.parseColor("#0284C7"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(32, 16, 32, 16)
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(connectBtn)
        setContentView(layout)

        connectBtn.setOnClickListener {
            if (!isConnected) {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    vpnPermissionLauncher.launch(intent)
                } else {
                    connectTest()
                }
            } else {
                startService(Intent(this, GoenoVpnService::class.java).apply { action = "STOP" })
                isConnected = false
                statusText.text = getString(R.string.status_disconnected)
                connectBtn.text = getString(R.string.connect)
            }
        }
    }

    private fun connectTest() {
        statusText.text = getString(R.string.status_connecting)
        lifecycleScope.launch {
            val bestNode = SmartRouter.pickBestNode(fallbackNodes) ?: fallbackNodes.first()
            val intent = Intent(this@MainActivity, GoenoVpnService::class.java).apply {
                putExtra("CONFIG_JSON", bestNode.xrayJsonConfig)
            }
            startForegroundService(intent)
            isConnected = true
            statusText.text = getString(R.string.status_connected, bestNode.name)
            connectBtn.text = getString(R.string.disconnect)
        }
    }

    private fun connectToBestServer() {
        connectTest()
    }
}
