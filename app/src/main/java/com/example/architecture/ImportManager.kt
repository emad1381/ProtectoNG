package com.example.architecture

import android.util.Base64
import com.example.ConfigType
import com.example.VpnConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ImportManager {

    fun sanitizeUri(input: String): String {
        var s = input.trim()
        // Replace markdown mailto links: [some@email.com](mailto:some@email.com) -> some@email.com
        s = s.replace(Regex("\\[([^\\]]+)\\]\\(mailto:[^\\)]+\\)"), "$1")
        // Replace standard markdown links just in case: [someText](link) -> someText
        s = s.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
        return s.trim()
    }

    fun validateConfig(config: VpnConfig): String? {
        if (config.address.isBlank()) {
            return "Address (host) cannot be empty."
        }
        if (config.port <= 0 || config.port > 65535) {
            return "Invalid port: ${config.port}. Port must be between 1 and 65535."
        }
        if (config.type == ConfigType.VLESS || config.type == ConfigType.VMESS) {
            if (config.uuid.isBlank()) {
                return "${config.type.name} UUID/User ID is missing or empty."
            }
            val cleanUuid = config.uuid.trim()
            if (cleanUuid.length < 8) {
                return "Invalid UUID: '${config.uuid}'. It is too short to be a valid client credential."
            }
        }
        return null
    }

    fun parseShareUri(uriStr: String): VpnConfig? {
        val trimmed = sanitizeUri(uriStr)
        if (trimmed.isEmpty()) return null

        try {
            if (trimmed.startsWith("vmess://", ignoreCase = true)) {
                return parseVmess(trimmed)
            }
            if (trimmed.startsWith("ss://", ignoreCase = true)) {
                return parseShadowsocks(trimmed)
            }

            // Universal manual robust parsing for vless, trojan, socks, http, wireguard
            val schemeEnd = trimmed.indexOf("://")
            if (schemeEnd == -1) return null
            val scheme = trimmed.substring(0, schemeEnd).lowercase()
            val type = when (scheme) {
                "vless" -> ConfigType.VLESS
                "trojan" -> ConfigType.TROJAN
                "socks" -> ConfigType.SOCKS
                "http" -> ConfigType.HTTP
                "wireguard" -> ConfigType.WIREGUARD
                else -> return null
            }

            var remaining = trimmed.substring(schemeEnd + 3)
            
            // Extract Fragment (Remarks)
            var fragment = ""
            val hashIdx = remaining.indexOf('#')
            if (hashIdx != -1) {
                fragment = decodeUrl(remaining.substring(hashIdx + 1))
                remaining = remaining.substring(0, hashIdx)
            }

            // Extract Query parameters
            var query = ""
            val questionIdx = remaining.indexOf('?')
            if (questionIdx != -1) {
                query = remaining.substring(questionIdx + 1)
                remaining = remaining.substring(0, questionIdx)
            }

            // Now remaining is `userinfo@host:port`
            var uuid = ""
            val atIdx = remaining.lastIndexOf('@')
            if (atIdx != -1) {
                uuid = remaining.substring(0, atIdx)
                remaining = remaining.substring(atIdx + 1)
            }

            // Now remaining is `host:port`
            var host = remaining
            var port = when (type) {
                ConfigType.VLESS -> 443
                ConfigType.TROJAN -> 443
                ConfigType.SOCKS -> 1080
                ConfigType.HTTP -> 80
                else -> 443
            }

            val colonIdx = remaining.lastIndexOf(':')
            if (colonIdx != -1) {
                val portStr = remaining.substring(colonIdx + 1)
                val parsedPort = portStr.toIntOrNull()
                if (parsedPort != null) {
                    port = parsedPort
                    host = remaining.substring(0, colonIdx)
                }
            }

            // Parse Query parameters into a map
            val queryParams = parseQueryParams(query)
            
            val security = queryParams["security"] ?: queryParams["tls"] ?: "none"
            val sni = queryParams["sni"] ?: ""
            val networkType = queryParams["type"] ?: queryParams["net"] ?: "tcp"
            val path = queryParams["path"] ?: "/"
            val hostParam = queryParams["host"] ?: ""
            val alpn = queryParams["alpn"] ?: ""

            var remarkName = fragment
            if (remarkName.isEmpty()) {
                remarkName = "${type.name} - $host"
            }

            // TASK 3: Log every parsed field.
            LogsModule.info("Parser", "[Parser Audit] Successfully parsed Universal URI:")
            LogsModule.info("Parser", " - URI: $trimmed")
            LogsModule.info("Parser", " - Type: $type")
            LogsModule.info("Parser", " - Name: $remarkName")
            LogsModule.info("Parser", " - UUID (User Info): $uuid")
            LogsModule.info("Parser", " - Address (Host): $host")
            LogsModule.info("Parser", " - Port: $port")
            LogsModule.info("Parser", " - Path: $path")
            LogsModule.info("Parser", " - SNI: $sni")
            LogsModule.info("Parser", " - Host Param: $hostParam")
            LogsModule.info("Parser", " - ALPN: $alpn")
            LogsModule.info("Parser", " - Security / TLS: $security")
            LogsModule.info("Parser", " - Transport / NetworkType: $networkType")

            val config = VpnConfig(
                name = remarkName,
                type = type,
                address = host,
                port = port,
                security = security,
                sni = sni,
                networkType = networkType,
                path = path,
                uuid = uuid,
                host = hostParam,
                alpn = alpn,
                remarks = "Imported successfully via share link",
                rawUri = trimmed
            )

            val validationError = validateConfig(config)
            if (validationError != null) {
                LogsModule.error("Parser", "[Parser Error] Invalid VLESS profile parsed: $validationError")
                throw IllegalArgumentException(validationError)
            }

            return config
        } catch (e: Exception) {
            LogsModule.error("Parser", "Error parsing URI: ${e.message}")
            throw e
        }
    }

    fun parseJsonConfig(jsonStr: String): VpnConfig? {
        val trimmed = jsonStr.trim()
        try {
            val json = JSONObject(trimmed)
            
            // Check if standard VMESS JSON
            if (json.has("add") || json.has("address")) {
                val host = json.optString("add", json.optString("address", "127.0.0.1"))
                val port = json.optInt("port", 443)
                val ps = json.optString("ps", json.optString("remark", "Imported JSON Config"))
                val rawProtocol = json.optString("protocol", "VLESS").uppercase()
                val type = try { ConfigType.valueOf(rawProtocol) } catch (e: Exception) { ConfigType.VLESS }
                val net = json.optString("net", "tcp")
                val tls = json.optString("tls", "none")
                val sni = json.optString("sni", "")

                return VpnConfig(
                    name = ps,
                    type = type,
                    address = host,
                    port = port,
                    security = tls,
                    sni = sni,
                    networkType = net,
                    remarks = "Imported via JSON",
                    rawUri = jsonStr
                )
            }

            // Check if Xray / V2ray outbound format
            if (json.has("outbounds")) {
                val outbounds = json.getJSONArray("outbounds")
                if (outbounds.length() > 0) {
                    val outbound = outbounds.getJSONObject(0)
                    val protocol = outbound.optString("protocol", "vless").uppercase()
                    val type = try { ConfigType.valueOf(protocol) } catch (e: Exception) { ConfigType.VLESS }
                    val settings = outbound.optJSONObject("settings")
                    val vnext = settings?.optJSONArray("vnext")
                    if (vnext != null && vnext.length() > 0) {
                        val serverObj = vnext.getJSONObject(0)
                        val host = serverObj.optString("address", "127.0.0.1")
                        val port = serverObj.optInt("port", 443)
                        return VpnConfig(
                            name = "JSON Outbound Profile",
                            type = type,
                            address = host,
                            port = port,
                            security = "tls",
                            sni = "",
                            networkType = "tcp",
                            remarks = "Imported from Xray Client config",
                            rawUri = jsonStr
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseVmess(trimmedUri: String): VpnConfig? {
        try {
            val base64Part = trimmedUri.substring("vmess://".length)
            val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
            val jsonStr = String(decodedBytes, StandardCharsets.UTF_8)
            val json = JSONObject(jsonStr)
            
            val host = json.optString("add", "127.0.0.1")
            val portText = json.optString("port", "443")
            val port = portText.toIntOrNull() ?: 443
            val remark = json.optString("ps", "VMESS Profile")
            val net = json.optString("net", "tcp")
            val tls = json.optString("tls", "none")
            val sni = json.optString("sni", "")
            val path = json.optString("path", "/")
            val id = json.optString("id", "")

            return VpnConfig(
                name = remark,
                type = ConfigType.VMESS,
                address = host,
                port = port,
                security = tls,
                sni = sni,
                networkType = net,
                path = path,
                uuid = id,
                remarks = "Imported VMESS profile",
                rawUri = trimmedUri
            )
        } catch (e: Exception) {
            // In case it's a standard URL scheme instead of base64
            try {
                val uri = URI.create(trimmedUri)
                val host = uri.host ?: "127.0.0.1"
                val port = if (uri.port != -1) uri.port else 443
                var remark = ""
                if (!uri.fragment.isNullOrEmpty()) {
                    remark = decodeUrl(uri.fragment)
                }
                if (remark.isEmpty()) remark = "VMESS Link"
                return VpnConfig(
                    name = remark,
                    type = ConfigType.VMESS,
                    address = host,
                    port = port,
                    security = "none",
                    sni = "",
                    networkType = "tcp",
                    remarks = "Imported from URL",
                    rawUri = trimmedUri
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return null
    }

    private fun parseShadowsocks(trimmedUri: String): VpnConfig? {
        try {
            val uri = URI.create(trimmedUri)
            // ss://base64#remark
            val userAndHost = uri.authority
            var remark = ""
            if (!uri.fragment.isNullOrEmpty()) {
                remark = decodeUrl(uri.fragment)
            }
            if (remark.isEmpty()) remark = "Shadowsocks Connection"

            if (userAndHost != null && userAndHost.contains("@")) {
                val parts = userAndHost.split("@")
                val hostPort = parts[1].split(":")
                val host = hostPort[0]
                val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 8388 else 8388
                return VpnConfig(
                    name = remark,
                    type = ConfigType.SHADOWSOCKS,
                    address = host,
                    port = port,
                    security = "none",
                    sni = "",
                    networkType = "tcp",
                    remarks = "Imported Shadowsocks Link",
                    rawUri = trimmedUri
                )
            } else {
                // Whole host might be base64-encoded user:pass@host:port
                val base64Str = uri.host ?: ""
                if (base64Str.isNotEmpty()) {
                    val decoded = String(Base64.decode(base64Str, Base64.DEFAULT), StandardCharsets.UTF_8)
                    if (decoded.contains("@")) {
                        val parts = decoded.split("@")
                        val hostPort = parts[1].split(":")
                        val host = hostPort[0]
                        val port = if (hostPort.size > 1) hostPort[1].toInt() else 8388
                        return VpnConfig(
                            name = remark,
                            type = ConfigType.SHADOWSOCKS,
                            address = host,
                            port = port,
                            security = "none",
                            sni = "",
                            networkType = "tcp",
                            remarks = "Imported Shadowsocks Base64",
                            rawUri = trimmedUri
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseQueryParams(query: String?): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isNullOrEmpty()) return result
        try {
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx > 0) {
                    val key = decodeUrl(pair.substring(0, idx))
                    val value = decodeUrl(pair.substring(idx + 1))
                    result[key] = value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun decodeUrl(str: String): String {
        return try {
            URLDecoder.decode(str, "UTF-8")
        } catch (e: Exception) {
            str
        }
    }
}
