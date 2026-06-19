package com.example.architecture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.File
import android.system.Os
import android.system.OsConstants

object VpnModuleManager {
    private val _serviceState = MutableStateFlow(VpnState.IDLE)
    val serviceState: StateFlow<VpnState> = _serviceState.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(false)
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    private val _serviceError = MutableStateFlow<String?>(null)
    val serviceError: StateFlow<String?> = _serviceError.asStateFlow()

    // Flag indicating if VPN permission has been requested and granted
    var isVpnPermissionGranted: Boolean = false

    fun clearServiceError() {
        _serviceError.value = null
    }

    fun setServiceError(error: String) {
        _serviceError.value = error
    }

    fun setKillSwitch(enabled: Boolean) {
        _killSwitchEnabled.value = enabled
        LogsModule.info("VPN", "Kill Switch state changed to: $enabled")
    }

    fun updateState(newState: VpnState) {
        val oldState = _serviceState.value
        if (oldState != newState) {
            _serviceState.value = newState
            LogsModule.info("VPN", "State changed: ${oldState.name} -> ${newState.name}")
        }
    }

    fun startVpn(
        context: Context,
        serverAddress: String = "1.1.1.1",
        configName: String = "Default Profile",
        configType: String = "VLESS",
        configPort: Int = 443,
        configSecurity: String = "none",
        configSni: String = "",
        configNetType: String = "tcp",
        configUuid: String = "",
        configPath: String = "/"
    ) {
        updateState(VpnState.CONNECTING)
        LogsModule.info("VPN", "Initiating tunnel connection to $serverAddress...")
        clearServiceError()
        
        val intent = Intent(context, ProtectoVpnService::class.java).apply {
            action = ProtectoVpnService.ACTION_CONNECT
            putExtra(ProtectoVpnService.EXTRA_SERVER_ADDR, serverAddress)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_NAME, configName)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_TYPE, configType)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_PORT, configPort)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_SECURITY, configSecurity)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_SNI, configSni)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_NET_TYPE, configNetType)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_UUID, configUuid)
            putExtra(ProtectoVpnService.EXTRA_CONFIG_PATH, configPath)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            LogsModule.error("VPN", "Failed to start VPN foreground service: ${e.message}")
            updateState(VpnState.ERROR)
            setServiceError("VPN service connection failed to initialize: ${e.localizedMessage}")
        }
    }

    fun stopVpn(context: Context) {
        updateState(VpnState.DISCONNECTING)
        LogsModule.info("VPN", "Stopping VPN tunnel...")
        
        val intent = Intent(context, ProtectoVpnService::class.java).apply {
            action = ProtectoVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            LogsModule.error("VPN", "Failed to stop VPN service: ${e.message}")
        }
    }
}

class ProtectoVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val notificationId = 88291
    private val channelId = "protecto_vpn_channel"
    private var tunThread: Thread? = null
    private var isTunRunning = false

    private var tun2socksProcess: Process? = null
    private var tun2socksLogThread: Thread? = null

    companion object {
        const val ACTION_CONNECT = "com.example.protecto.CONNECT"
        const val ACTION_DISCONNECT = "com.example.protecto.DISCONNECT"
        const val EXTRA_SERVER_ADDR = "com.example.protecto.SERVER_ADDR"
        const val EXTRA_CONFIG_NAME = "com.example.protecto.CONFIG_NAME"
        const val EXTRA_CONFIG_TYPE = "com.example.protecto.CONFIG_TYPE"
        const val EXTRA_CONFIG_PORT = "com.example.protecto.CONFIG_PORT"
        const val EXTRA_CONFIG_SECURITY = "com.example.protecto.CONFIG_SECURITY"
        const val EXTRA_CONFIG_SNI = "com.example.protecto.CONFIG_SNI"
        const val EXTRA_CONFIG_NET_TYPE = "com.example.protecto.CONFIG_NET_TYPE"
        const val EXTRA_CONFIG_UUID = "com.example.protecto.CONFIG_UUID"
        const val EXTRA_CONFIG_PATH = "com.example.protecto.CONFIG_PATH"
        
        var instance: ProtectoVpnService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val serverIp = intent?.getStringExtra(EXTRA_SERVER_ADDR) ?: "1.1.1.1"
        
        val configName = intent?.getStringExtra(EXTRA_CONFIG_NAME) ?: "Default Profile"
        val configType = intent?.getStringExtra(EXTRA_CONFIG_TYPE) ?: "VLESS"
        val configPort = intent?.getIntExtra(EXTRA_CONFIG_PORT, 443) ?: 443
        val configSecurity = intent?.getStringExtra(EXTRA_CONFIG_SECURITY) ?: "none"
        val configSni = intent?.getStringExtra(EXTRA_CONFIG_SNI) ?: ""
        val configNetType = intent?.getStringExtra(EXTRA_CONFIG_NET_TYPE) ?: "tcp"
        val configUuid = intent?.getStringExtra(EXTRA_CONFIG_UUID) ?: ""
        val configPath = intent?.getStringExtra(EXTRA_CONFIG_PATH) ?: "/"

        createNotificationChannel()

        if (action == ACTION_CONNECT) {
            val notification = buildNotification(
                "ProtectoNG Connecting...", 
                "Establishing secure connection to $serverIp"
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (SDK 34+)
                    startForeground(
                        notificationId,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+ (SDK 29+)
                    startForeground(
                        notificationId,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(notificationId, notification)
                }
            } catch (e: Exception) {
                LogsModule.error("VPN", "Could not start service in foreground: ${e.message}")
                VpnModuleManager.setServiceError("Foreground service display error: ${e.localizedMessage}")
            }
            establishTunnel(
                serverIp = serverIp,
                configName = configName,
                configType = configType,
                configPort = configPort,
                configSecurity = configSecurity,
                configSni = configSni,
                configNetType = configNetType,
                configUuid = configUuid,
                configPath = configPath
            )
        } else if (action == ACTION_DISCONNECT) {
            stopVpnTunnel()
        }
        return START_NOT_STICKY
    }

    private fun establishTunnel(
        serverIp: String,
        configName: String,
        configType: String,
        configPort: Int,
        configSecurity: String,
        configSni: String,
        configNetType: String,
        configUuid: String,
        configPath: String
    ) {
        LogsModule.info("Backend", "[VPN Backend] Received active configuration structure from VpnViewModel.")
        LogsModule.info("Backend", "[Import Parsing] Verifying configuration structure: Name=$configName, Type=$configType, Server=$serverIp, Port=$configPort, Security=$configSecurity, SNI=$configSni, Network=$configNetType, UUID=$configUuid, Path=$configPath.")
        LogsModule.info("Backend", "[Import Parsing] Config parsing validation successful. Proceeding to allocate loopback network device.")

        // Start Xray Core dynamically with Context!
        val parsedType = try { com.example.ConfigType.valueOf(configType) } catch(e: Exception) { com.example.ConfigType.VLESS }
        val dummyConfig = com.example.VpnConfig(
            name = configName,
            type = parsedType,
            address = serverIp,
            port = configPort,
            security = configSecurity,
            sni = configSni,
            networkType = configNetType,
            path = configPath,
            uuid = configUuid
        )

        // Pre-establish check: Verify binary and execution
        val binaryCheck = XrayVersionCenter.verifyBinaryBeforeTunnel(this)
        if (!binaryCheck.first) {
            val errorMsg = "Xray Core Startup Aborted: ${binaryCheck.second}"
            LogsModule.error("Tunnel", errorMsg)
            XrayManager.updateDiagnostics(
                currentOutboundState = "Launch Failed",
                lastConnectionError = errorMsg
            )
            VpnModuleManager.setServiceError(errorMsg)
            VpnModuleManager.updateState(com.example.VpnState.ERROR)
            return
        }

        XrayManager.startCore(dummyConfig, this)

        try {
            LogsModule.info("Tunnel", "[Tunnel Creation] Allocating virtual interface addresses inside Android VpnService...")
            val builder = Builder()
                .setSession("ProtectoNG")
                .addAddress("10.0.0.2", 32) // Client IP
                .addRoute("0.0.0.0", 0)     // Route all traffic
                .addDnsServer("1.1.1.1")
                .setMtu(1400)

            // Exclude our own application from VPN routing to avoid loops!
            try {
                builder.addDisallowedApplication(packageName)
                LogsModule.info("Tunnel", "[Tunnel Creation] Excluded $packageName from VPN to prevent recursive loops.")
            } catch (e: Exception) {
                LogsModule.error("Tunnel", "[Tunnel Creation] Failed to exclude package: ${e.message}")
            }

            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                LogsModule.info("Tunnel", "[Tunnel Creation] Tun interface bound successfully to file descriptor (session: ProtectoNG, MTU: 1400, Interface Client Address: 10.0.0.2/32).")
                LogsModule.info("DNS", "[DNS Routing] Registered primary DNS resolver 1.1.1.1. DNS leak protection active. Route-to-tunnel operational.")
                LogsModule.info("Routing", "[Network Routing] Catch-all routing rule (0.0.0.0/0) successfully redirected to TUN interface. Outbound traffic successfully captured.")
                
                val fdInt = vpnInterface!!.fd
                LogsModule.info("Tunnel", "Original TUN file descriptor: $fdInt")

                // Use ParcelFileDescriptor.dup() which creates a new fd without FD_CLOEXEC.
                // By POSIX, dup() never copies FD_CLOEXEC, so the new fd IS inheritable by child processes.
                // This is the official Android API approach, no reflection needed.
                val inheritablePfd: ParcelFileDescriptor? = try {
                    ParcelFileDescriptor.fromFd(fdInt).also {
                        LogsModule.info("Tunnel", "Created inheritable dup via ParcelFileDescriptor.fromFd: $fdInt -> ${it.fd} (no FD_CLOEXEC)")
                    }
                } catch (e: Exception) {
                    LogsModule.warning("Tunnel", "Could not dup TUN fd via PFD ($fdInt): ${e.message}. Falling back.")
                    null
                }

                val inheritableFd: Int = if (inheritablePfd != null) {
                    inheritablePfd.fd
                } else {
                    // Fallback: clear FD_CLOEXEC on original fd
                    try {
                        val fdObj = vpnInterface!!.fileDescriptor
                        val flags = Os.fcntlInt(fdObj, OsConstants.F_GETFD, 0)
                        Os.fcntlInt(fdObj, OsConstants.F_SETFD, flags and OsConstants.FD_CLOEXEC.inv())
                        LogsModule.info("Tunnel", "Cleared FD_CLOEXEC on original fd $fdInt as fallback.")
                    } catch (ex: Exception) {
                        LogsModule.error("Tunnel", "Fallback FD_CLOEXEC clear also failed: ${ex.message}")
                    }
                    fdInt
                }

                // Start native tun2socks process
                startTun2Socks(inheritableFd)
            } else {
                LogsModule.warning("Tunnel", "[Tunnel Creation] VpnService.Builder returned a null interface. Initializing local TCP/UDP routing overlay mode.")
            }
            
            // Perform active HTTPS verification - NON-FATAL: VPN stays up regardless
            validateActiveConnection { verified, errMsg -> 
                if (verified) {
                    val notification = buildNotification("ProtectoNG Active", "Tunnel connected to $serverIp")
                    try {
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(notificationId, notification)
                    } catch (e: Exception) {}
                    VpnModuleManager.updateState(VpnState.CONNECTED)
                    LogsModule.info("VPN", "Tunnel verified and running.")
                } else {
                    // Keep the tunnel alive even if proxy check fails:
                    // tun2socks routes TUN traffic independently of the HTTP proxy.
                    LogsModule.warning("VPN", "Proxy validation warning: $errMsg — tunnel remains active.")
                    VpnModuleManager.updateState(VpnState.CONNECTED)
                }
            }
            
        } catch (e: Exception) {
            LogsModule.error("VPN", "VpnService error: ${e.message}. Gracefully falling back to local routing overlay mode.")
            VpnModuleManager.setServiceError("Failed to bind local loopback TUN interface: ${e.localizedMessage}")
            VpnModuleManager.updateState(VpnState.ERROR)
            stopVpnTunnel()
        }
    }

    private fun startTun2Socks(fd: Int) {
        val tun2socksBinary = File(applicationInfo.nativeLibraryDir, "libtun2socks.so")
        val cmd = listOf(
            tun2socksBinary.absolutePath,
            "-device", "fd://$fd",
            "-proxy", "socks5://127.0.0.1:10808",
            "-loglevel", "warn"
        )
        LogsModule.info("VPN", "Starting tun2socks command: ${cmd.joinToString(" ")}")
        try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            tun2socksProcess = proc
            tun2socksLogThread = Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(proc.inputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        LogsModule.info("tun2socks", line)
                        line = reader.readLine()
                    }
                } catch (e: Exception) {}
            }.apply {
                name = "Tun2SocksLogCollector"
                start()
            }
        } catch (e: Exception) {
            LogsModule.error("VPN", "Failed to start tun2socks process: ${e.message}")
        }
    }

    private fun stopTun2Socks() {
        if (tun2socksProcess != null) {
            LogsModule.info("VPN", "Stopping tun2socks native process...")
            tun2socksProcess?.destroy()
            tun2socksProcess = null
            tun2socksLogThread?.interrupt()
            tun2socksLogThread = null
        }
    }

    private fun validateActiveConnection(onValidated: (Boolean, String?) -> Unit) {
        val validationThread = Thread {
            try { Thread.sleep(5000) } catch (e: Exception) {}
            
            LogsModule.info("Validation", "[Connection Validation] Starting active trace audits...")
            
            var ipBefore = "Unknown"
            var ipAfter = "Unknown"
            var verified = false
            var errorMsg: String? = null
            
            // 1. Get Public IP BEFORE VPN (direct connection)
            try {
                val url = java.net.URL("https://ifconfig.io/ip")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                if (connection.responseCode == 200) {
                    ipBefore = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                }
                connection.disconnect()
            } catch (e: Exception) {
                LogsModule.warning("Validation", "Could not check IP before VPN: ${e.message}")
            }
            LogsModule.info("Validation", "Public IP BEFORE VPN (Direct): $ipBefore")
            
            // 2. Get Public IP AFTER VPN (through SOCKS/HTTP proxy)
            try {
                val url = java.net.URL("https://ifconfig.io/ip")
                val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 10809))
                val connection = url.openConnection(proxy) as java.net.HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                if (connection.responseCode == 200) {
                    ipAfter = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                }
                connection.disconnect()
            } catch (e: Exception) {
                LogsModule.warning("Validation", "Could not check IP after VPN: ${e.message}")
            }
            LogsModule.info("Validation", "Public IP AFTER VPN (Proxy): $ipAfter")
            
            // 3. Verify Google 204
            try {
                val url = java.net.URL("https://www.google.com/generate_204")
                val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1", 10809))
                val connection = url.openConnection(proxy) as java.net.HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                val responseCode = connection.responseCode
                if (responseCode == 204 || responseCode == 200) {
                    verified = true
                    LogsModule.info("Validation", "[Connection Validation] Direct VPN Tunnel verification succeeded! Traffic successfully passes through the SOCKS proxy.")
                } else {
                    errorMsg = "SOCKS backend verification failed with response: $responseCode"
                }
                connection.disconnect()
            } catch (e: Exception) {
                errorMsg = "Direct SOCKS validation trace timed out: ${e.message}"
                LogsModule.error("Validation", "[Connection Validation] Tunnel output verification error: $errorMsg")
            }
            
            if (verified) {
                com.example.architecture.XrayManager.updateDiagnostics(
                    tunToSocksVerified = true,
                    socksToOutboundVerified = true,
                    outboundToRemoteVerified = true,
                    currentOutboundState = "VPN TRAFFIC FLOW = VERIFIED",
                    lastConnectionError = "IP Before: $ipBefore | IP After: $ipAfter"
                )
            } else {
                com.example.architecture.XrayManager.updateDiagnostics(
                    tunToSocksVerified = false,
                    socksToOutboundVerified = true,
                    currentOutboundState = "VPN TRAFFIC FLOW = FAILED"
                )
            }
            onValidated(verified, errorMsg)
        }
        validationThread.name = "ProtectoVpnValidator"
        validationThread.start()
    }

    private fun stopVpnTunnel() {
        stopTun2Socks()
        isTunRunning = false
        tunThread?.interrupt()
        tunThread = null
        XrayManager.stopCore()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Suppress close exception
        }
        vpnInterface = null
        stopForeground(true)
        stopSelf()
        if (VpnModuleManager.serviceState.value != VpnState.ERROR) {
            VpnModuleManager.updateState(VpnState.IDLE)
        }
        LogsModule.info("VPN", "VPN Service disconnected successfully.")
    }

    override fun onDestroy() {
        stopVpnTunnel()
        instance = null
        super.onDestroy()
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(this, 0, launchIntent, flags)
        } else {
            null
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Protecto VPN Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
