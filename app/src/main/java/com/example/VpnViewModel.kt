package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.architecture.CountryDetector
import com.example.architecture.ImportManager
import com.example.architecture.PingModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

enum class VpnState {
    IDLE, CONNECTING, CONNECTED, DISCONNECTING, ERROR, DISCONNECTED
}

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("protecto_prefs", Context.MODE_PRIVATE)

    // --- VPN Connection state ---
    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _vpnError = MutableStateFlow<String?>(null)
    val vpnError: StateFlow<String?> = _vpnError.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    fun clearVpnError() {
        _vpnError.value = null
        com.example.architecture.VpnModuleManager.clearServiceError()
    }

    // --- Active connection speeds & duration ---
    private val _downloadSpeed = MutableStateFlow("0.0 B/s")
    val downloadSpeed: StateFlow<String> = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow("0.0 B/s")
    val uploadSpeed: StateFlow<String> = _uploadSpeed.asStateFlow()

    private val _connectionDuration = MutableStateFlow("00:00")
    val connectionDuration: StateFlow<String> = _connectionDuration.asStateFlow()

    // --- Selected Server ---
    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // --- Servers List ---
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers.asStateFlow()

    // --- Configurations List ---
    private val _configList = MutableStateFlow<List<VpnConfig>>(emptyList())
    val configList: StateFlow<List<VpnConfig>> = _configList.asStateFlow()

    private val _activeConfig = MutableStateFlow<VpnConfig?>(null)
    val activeConfig: StateFlow<VpnConfig?> = _activeConfig.asStateFlow()

    // --- Connection History ---
    private val _historyList = MutableStateFlow<List<ConnectionHistory>>(emptyList())
    val historyList: StateFlow<List<ConnectionHistory>> = _historyList.asStateFlow()

    // --- App Settings States (Loaded from SharedPreferences) ---
    val isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val selectLanguage = MutableStateFlow(prefs.getString("select_language", "Persian") ?: "Persian")
    val isNotificationsEnabled = MutableStateFlow(prefs.getBoolean("is_notifications", true))
    val isAutoConnectEnabled = MutableStateFlow(prefs.getBoolean("is_autoconnect", false))

    // --- Jobs for active simulation ---
    private var connectionTimerJob: Job? = null
    private var speedsSimulationJob: Job? = null
    private var connectedSeconds = 0

    init {
        // Initialize Xray Core version management system
        com.example.architecture.XrayVersionCenter.init(application)

        // Complete clean startup: Load ONLY user added/imported configs. No hardcoded ones.
        loadConfigsFromPreferences()

        // Bind incoming system VPN connection states to update the UI & statistics trackers
        viewModelScope.launch {
            com.example.architecture.VpnModuleManager.serviceState.collect { systemState ->
                _vpnState.value = systemState
                if (systemState == VpnState.CONNECTED) {
                    startProductionStatsTracking()
                } else if (systemState == VpnState.IDLE || systemState == VpnState.ERROR) {
                    stopProductionStatsTracking()
                }
            }
        }

        // Collect incoming VPN startup/critical failure messages
        viewModelScope.launch {
            com.example.architecture.VpnModuleManager.serviceError.collect { errorMsg ->
                if (errorMsg != null) {
                    _vpnError.value = errorMsg
                }
            }
        }

        // Dynamic listeners to auto-persist settings updates instantly
        viewModelScope.launch {
            isDarkMode.collect { prefs.edit().putBoolean("is_dark_mode", it).apply() }
        }
        viewModelScope.launch {
            selectLanguage.collect { prefs.edit().putString("select_language", it).apply() }
        }
        viewModelScope.launch {
            isNotificationsEnabled.collect { prefs.edit().putBoolean("is_notifications", it).apply() }
        }
        viewModelScope.launch {
            isAutoConnectEnabled.collect { prefs.edit().putBoolean("is_autoconnect", it).apply() }
        }
    }

    private fun loadConfigsFromPreferences() {
        val rawJson = prefs.getString("saved_vpn_configs_v4", null)
        val result = mutableListOf<VpnConfig>()
        if (!rawJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val name = obj.optString("name", "Config")
                    val typeName = obj.optString("type", ConfigType.VLESS.name)
                    val type = try { ConfigType.valueOf(typeName) } catch (e: Exception) { ConfigType.VLESS }
                    val address = obj.optString("address", "127.0.0.1")
                    val port = obj.optInt("port", 443)
                    val security = obj.optString("security", "none")
                    val sni = obj.optString("sni", "")
                    val networkType = obj.optString("networkType", "tcp")
                    val remarks = obj.optString("remarks", "")
                    val rawUri = obj.optString("rawUri", "")
                    val country = obj.optString("country", "United States")
                    val countryCode = obj.optString("countryCode", "US")
                    val flag = obj.optString("flag", "🇺🇸")
                    val pingVal = obj.optInt("ping", -1)
                    val ping = if (pingVal == -1) null else pingVal
                    val isFavorite = obj.optBoolean("isFavorite", false)
                    val path = obj.optString("path", "/")
                    val uuid = obj.optString("uuid", "")
                    val hostField = obj.optString("host", "")
                    val alpnField = obj.optString("alpn", "")

                    result.add(
                        VpnConfig(
                            id = id, name = name, type = type, address = address, port = port,
                            security = security, sni = sni, networkType = networkType, remarks = remarks,
                            rawUri = rawUri, country = country, countryCode = countryCode, flag = flag,
                            ping = ping, isFavorite = isFavorite, path = path, uuid = uuid,
                            host = hostField, alpn = alpnField
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _configList.value = result
        if (result.isNotEmpty()) {
            _activeConfig.value = result.first()
        }
        syncServersFromConfigs(result)
    }

    private fun saveConfigsToPreferences(configs: List<VpnConfig>) {
        try {
            val array = JSONArray()
            for (c in configs) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("type", c.type.name)
                obj.put("address", c.address)
                obj.put("port", c.port)
                obj.put("security", c.security)
                obj.put("sni", c.sni)
                obj.put("networkType", c.networkType)
                obj.put("remarks", c.remarks)
                obj.put("rawUri", c.rawUri)
                obj.put("country", c.country)
                obj.put("countryCode", c.countryCode)
                obj.put("flag", c.flag)
                obj.put("ping", c.ping ?: -1)
                obj.put("isFavorite", c.isFavorite)
                obj.put("path", c.path)
                obj.put("uuid", c.uuid)
                obj.put("host", c.host)
                obj.put("alpn", c.alpn)
                array.put(obj)
            }
            prefs.edit().putString("saved_vpn_configs_v4", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncServersFromConfigs(configs: List<VpnConfig>) {
        val mapped = configs.map { config ->
            Server(
                id = config.id,
                name = config.name,
                FarsiName = config.name,
                flag = config.flag,
                ip = "${config.address}:${config.port}",
                ping = config.ping ?: -1,
                isFavorite = config.isFavorite,
                configId = config.id
            )
        }
        _servers.value = mapped

        // Auto selection handling
        val currentSelect = _selectedServer.value
        if (currentSelect != null) {
            val found = mapped.firstOrNull { it.id == currentSelect.id }
            if (found != null) {
                _selectedServer.value = found
            } else {
                _selectedServer.value = mapped.firstOrNull()
            }
        } else {
            _selectedServer.value = mapped.firstOrNull()
        }
    }

    // --- Action triggers ---

    fun toggleConnect() {
        val config = _activeConfig.value
        if (config == null) {
            com.example.architecture.LogsModule.warning("VPN", "No profile selected. Cannot toggle connection.")
            return
        }

        val currentState = com.example.architecture.VpnModuleManager.serviceState.value
        if (currentState == VpnState.CONNECTED || currentState == VpnState.CONNECTING) {
            // Log final usage details to persistent history session on disconnect
            val active = _activeConfig.value
            val isEnglish = selectLanguage.value == "English"
            val finalServerName = active?.name ?: (if (isEnglish) "Unknown Server" else "سرور ناشناس")
            val finalServerFlag = active?.flag ?: "🌐"
            
            val totalBytes = com.example.architecture.StatisticsModule.totalSessionUsage.value
            val totalMb = totalBytes / (1024.0f * 1024.0f)
            
            val newHistory = ConnectionHistory(
                serverName = finalServerName,
                serverFlag = finalServerFlag,
                timeStr = if (isEnglish) "Just now" else "هم‌اکنون",
                durationStr = _connectionDuration.value,
                dataUsedMb = totalMb
            )
            _historyList.value = listOf(newHistory) + _historyList.value

            com.example.architecture.VpnModuleManager.stopVpn(getApplication())
        } else {
            val validationError = com.example.architecture.ImportManager.validateConfig(config)
            if (validationError != null) {
                com.example.architecture.LogsModule.error("VPN", "Cannot establish tunnel: $validationError")
                _vpnError.value = "Validation Failed: $validationError"
                com.example.architecture.VpnModuleManager.updateState(com.example.VpnState.ERROR)
                return
            }

            com.example.architecture.VpnModuleManager.startVpn(
                context = getApplication(),
                serverAddress = config.address,
                configName = config.name,
                configType = config.type.name,
                configPort = config.port,
                configSecurity = config.security,
                configSni = config.sni,
                configNetType = config.networkType,
                configUuid = config.uuid,
                configPath = config.path
            )
        }
    }

    fun onVpnPermissionDenied() {
        com.example.architecture.VpnModuleManager.updateState(VpnState.ERROR)
        com.example.architecture.LogsModule.error("VPN", "Android VpnService system permission request denied by user.")
    }

    private var statsJob: Job? = null
    private var timerJob: Job? = null
    private var secondsElapsed = 0

    private fun startProductionStatsTracking() {
        secondsElapsed = 0
        _connectionDuration.value = "۰۰:۰۰"
        com.example.architecture.StatisticsModule.resetStats()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                secondsElapsed++
                val m = secondsElapsed / 60
                val s = secondsElapsed % 60
                val h = m / 60
                val baseTimeStr = if (h > 0) {
                    val mm = m % 60
                    String.format("%02d:%02d:%02d", h, mm, s)
                } else {
                    String.format("%02d:%02d", m, s)
                }
                _connectionDuration.value = formatPersianDigits(baseTimeStr)
            }
        }

        statsJob = viewModelScope.launch {
            while (true) {
                val speeds = com.example.architecture.StatisticsModule.consumeSpeedWindow()
                val dlSpeed = speeds.first
                val ulSpeed = speeds.second

                val isEng = selectLanguage.value == "English"
                _downloadSpeed.value = com.example.architecture.StatisticsModule.formatBytes(dlSpeed, isSpeed = true, isFarsi = !isEng)
                _uploadSpeed.value = com.example.architecture.StatisticsModule.formatBytes(ulSpeed, isSpeed = true, isFarsi = !isEng)
                delay(1000)
            }
        }
    }

    private fun stopProductionStatsTracking() {
        timerJob?.cancel()
        statsJob?.cancel()
        timerJob = null
        statsJob = null
        _downloadSpeed.value = if (selectLanguage.value == "English") "0.0 B/s" else "۰.۰ B/s"
        _uploadSpeed.value = if (selectLanguage.value == "English") "0.0 B/s" else "۰.۰ B/s"
    }

    // --- Server Operations (Legacy wrappers to keep UI intact) ---

    fun selectServer(server: Server) {
        val config = _configList.value.firstOrNull { it.id == server.id }
        if (config != null) {
            selectConfig(config)
        }
    }

    fun toggleServerFavorite(serverId: String) {
        val updated = _configList.value.map {
            if (it.id == serverId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _configList.value = updated
        saveConfigsToPreferences(updated)
        syncServersFromConfigs(updated)
    }

    fun addServer(name: String, farsiName: String, flag: String, ip: String, ping: Int) {
        // Obsolete manually adding server interface, replaced fully by config parsing.
    }

    // --- Config Operations ---

    fun selectConfig(config: VpnConfig) {
        _activeConfig.value = config
        val matchingServer = _servers.value.firstOrNull { it.id == config.id }
        if (matchingServer != null) {
            _selectedServer.value = matchingServer
        }
        val state = com.example.architecture.VpnModuleManager.serviceState.value
        if (state == VpnState.CONNECTED || state == VpnState.CONNECTING) {
            val validationError = com.example.architecture.ImportManager.validateConfig(config)
            if (validationError != null) {
                com.example.architecture.LogsModule.error("VPN", "Cannot switch to invalid profile: $validationError")
                _vpnError.value = "Validation Failed: $validationError"
                com.example.architecture.VpnModuleManager.stopVpn(getApplication())
                com.example.architecture.VpnModuleManager.updateState(com.example.VpnState.ERROR)
                return
            }

            com.example.architecture.VpnModuleManager.stopVpn(getApplication())
            viewModelScope.launch {
                delay(400)
                com.example.architecture.VpnModuleManager.startVpn(
                    context = getApplication(),
                    serverAddress = config.address,
                    configName = config.name,
                    configType = config.type.name,
                    configPort = config.port,
                    configSecurity = config.security,
                    configSni = config.sni,
                    configNetType = config.networkType,
                    configUuid = config.uuid,
                    configPath = config.path
                )
            }
        }
    }

    fun importConfigRaw(rawText: String): Boolean {
        _importError.value = null
        com.example.architecture.LogsModule.info("Import", "Initiating config parsing from scanned connection string...")
        
        var config: VpnConfig? = null
        try {
            // 1. Try URI parsing
            config = ImportManager.parseShareUri(rawText)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown parser error"
            _importError.value = errMsg
            com.example.architecture.LogsModule.error("Import", "URI parsing error: $errMsg")
            return false
        }

        if (config == null) {
            try {
                // 2. Try JSON parsing
                config = ImportManager.parseJsonConfig(rawText)
            } catch (e: Exception) {
                val errMsg = e.message ?: "Unknown JSON parser error"
                _importError.value = errMsg
                com.example.architecture.LogsModule.error("Import", "JSON parsing error: $errMsg")
                return false
            }
        }

        if (config != null) {
            val validationVal = ImportManager.validateConfig(config)
            if (validationVal != null) {
                _importError.value = validationVal
                com.example.architecture.LogsModule.error("Import", "Configuration validation failed: $validationVal")
                return false
            }

            com.example.architecture.LogsModule.info("Import", "Config parsing validation successful: Loaded '${config.name}' [Protocol: ${config.type.name}] aiming at ${config.address}:${config.port}.")
            val current = _configList.value.toMutableList()
            current.add(config)
            _configList.value = current
            saveConfigsToPreferences(current)
            syncServersFromConfigs(current)

            if (_activeConfig.value == null) {
                _activeConfig.value = config
                _selectedServer.value = _servers.value.firstOrNull { it.id == config.id }
            }

            // Asynchronously detect country
            val cid = config.id
            viewModelScope.launch {
                val detection = CountryDetector.detectCountry(config.address)
                val updated = _configList.value.map {
                    if (it.id == cid) {
                        it.copy(
                            country = detection.first,
                            countryCode = detection.second,
                            flag = CountryDetector.countryCodeToFlag(detection.second)
                        )
                    } else it
                }
                _configList.value = updated
                saveConfigsToPreferences(updated)
                syncServersFromConfigs(updated)
                
                // If it is active, refresh the active state model
                if (_activeConfig.value?.id == cid) {
                    _activeConfig.value = updated.firstOrNull { it.id == cid }
                }
            }
            return true
        }
        val fallbackError = "Failed to parse connection string. Scan payload matches no standard VLESS, VMESS, Trojan, or Shadowsocks protocol schemas."
        _importError.value = fallbackError
        com.example.architecture.LogsModule.error("Import", fallbackError)
        return false
    }

    fun testPingForConfig(configId: String) {
        // Set loading state
        val loadingList = _configList.value.map {
            if (it.id == configId) {
                it.copy(isPingLoading = true)
            } else it
        }
        _configList.value = loadingList
        syncServersFromConfigs(loadingList)

        viewModelScope.launch {
            val config = _configList.value.firstOrNull { it.id == configId }
            if (config != null) {
                val rtt = PingModule.measurePing(config.address, config.port)
                val completedList = _configList.value.map {
                    if (it.id == configId) {
                        it.copy(ping = if (rtt >= 0) rtt else -1, isPingLoading = false)
                    } else it
                }
                _configList.value = completedList
                saveConfigsToPreferences(completedList)
                syncServersFromConfigs(completedList)

                if (_activeConfig.value?.id == configId) {
                    _activeConfig.value = completedList.firstOrNull { it.id == configId }
                }
            }
        }
    }

    fun selectLanguage(lang: String) {
        selectLanguage.value = lang
    }

    fun addConfig(name: String, type: ConfigType, address: String, remarks: String) {
        val newConfig = VpnConfig(
            name = name,
            type = type,
            address = address,
            remarks = remarks
        )
        val current = _configList.value + newConfig
        _configList.value = current
        saveConfigsToPreferences(current)
        syncServersFromConfigs(current)
        if (_activeConfig.value == null) {
            _activeConfig.value = newConfig
        }
    }

    fun editConfig(id: String, name: String, type: ConfigType, address: String, remarks: String) {
        val updated = _configList.value.map {
            if (it.id == id) {
                it.copy(name = name, type = type, address = address, remarks = remarks)
            } else it
        }
        _configList.value = updated
        saveConfigsToPreferences(updated)
        syncServersFromConfigs(updated)

        if (_activeConfig.value?.id == id) {
            _activeConfig.value = updated.firstOrNull { it.id == id }
        }
    }

    fun deleteConfig(id: String) {
        val updated = _configList.value.filterNot { it.id == id }
        _configList.value = updated
        saveConfigsToPreferences(updated)
        syncServersFromConfigs(updated)

        if (_activeConfig.value?.id == id) {
            _activeConfig.value = updated.firstOrNull()
        }
    }

    // --- Utilities ---

    private fun formatPersianDigits(text: String): String {
        if (selectLanguage.value == "English") return text
        return text
            .replace('0', '۰')
            .replace('1', '۱')
            .replace('2', '۲')
            .replace('3', '۳')
            .replace('4', '۴')
            .replace('5', '۵')
            .replace('6', '۶')
            .replace('7', '۷')
            .replace('8', '۸')
            .replace('9', '۹')
    }

    fun getTrafficStats(): Triple<String, String, String> {
        val dlBytes = com.example.architecture.StatisticsModule.sessionDownload.value
        val ulBytes = com.example.architecture.StatisticsModule.sessionUpload.value
        
        val dlTotal = dlBytes / (1024.0f * 1024.0f * 1024.0f)
        val ulTotal = ulBytes / (1024.0f * 1024.0f * 1024.0f)
        val total = (dlBytes + ulBytes) / (1024.0f * 1024.0f * 1024.0f)

        val df = String.format("%.2f", dlTotal)
        val uf = String.format("%.2f", ulTotal)
        val tf = String.format("%.2f", total)

        val unitSuffix = if (selectLanguage.value == "English") " GB" else " گیگابایت"

        return Triple(
            formatPersianDigits(df) + unitSuffix,
            formatPersianDigits(uf) + unitSuffix,
            formatPersianDigits(tf) + unitSuffix
        )
    }
}
