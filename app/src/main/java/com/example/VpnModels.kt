package com.example

import java.util.UUID

data class Server(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val FarsiName: String,
    val flag: String,
    val ip: String,
    val ping: Int,
    val isFavorite: Boolean = false,
    val configId: String = ""
)

enum class ConfigType {
    VLESS, VMESS, TROJAN, SHADOWSOCKS, SOCKS, HTTP, WIREGUARD
}

data class VpnConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ConfigType,
    val address: String,
    val port: Int = 443,
    val security: String = "none",
    val sni: String = "",
    val networkType: String = "tcp",
    val path: String = "/",
    val uuid: String = "",
    val host: String = "",
    val alpn: String = "",
    val remarks: String = "",
    val rawUri: String = "",
    // Location detected values
    var country: String = "United States",
    var countryCode: String = "US",
    var flag: String = "🇺🇸",
    // Connection dynamics
    var ping: Int? = null,
    var isPingLoading: Boolean = false,
    var isFavorite: Boolean = false
)

data class ConnectionHistory(
    val id: String = UUID.randomUUID().toString(),
    val serverName: String,
    val serverFlag: String,
    val timeStr: String,
    val durationStr: String,
    val dataUsedMb: Float
)
