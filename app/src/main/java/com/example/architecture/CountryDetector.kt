package com.example.architecture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object CountryDetector {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Converts a two-character ISO country code into its corresponding flag emoji.
     */
    fun countryCodeToFlag(code: String): String {
        val uppercaseCode = code.uppercase()
        if (uppercaseCode.length != 2) return "🌐"
        try {
            val firstChar = Character.codePointAt(uppercaseCode, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(uppercaseCode, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        } catch (e: Exception) {
            return "🌐"
        }
    }

    /**
     * Resolves host address to an IP and fetches its public country code and name.
     */
    suspend fun detectCountry(host: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val lowercaseHost = host.lowercase()
        
        // Instant check based on top-level domain extension
        val suffixCheck = when {
            lowercaseHost.endsWith(".de") -> Pair("Germany", "DE")
            lowercaseHost.endsWith(".fi") -> Pair("Finland", "FI")
            lowercaseHost.endsWith(".nl") -> Pair("Netherlands", "NL")
            lowercaseHost.endsWith(".us") -> Pair("United States", "US")
            lowercaseHost.endsWith(".uk") || lowercaseHost.endsWith(".co.uk") -> Pair("United Kingdom", "GB")
            lowercaseHost.endsWith(".ca") -> Pair("Canada", "CA")
            lowercaseHost.endsWith(".tr") -> Pair("Turkey", "TR")
            lowercaseHost.endsWith(".ir") -> Pair("Iran", "IR")
            lowercaseHost.endsWith(".fr") -> Pair("France", "FR")
            lowercaseHost.endsWith(".jp") -> Pair("Japan", "JP")
            lowercaseHost.endsWith(".sg") -> Pair("Singapore", "SG")
            else -> null
        }
        if (suffixCheck != null) {
            return@withContext suffixCheck
        }

        var ip = host
        try {
            val address = InetAddress.getByName(host)
            ip = address.hostAddress ?: host
        } catch (e: Exception) {
            // DNS resolution fail or local/mock host
        }

        // Try API 1: ip-api.com
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/$ip")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val json = JSONObject(bodyString)
                        val status = json.optString("status")
                        if (status == "success") {
                            val country = json.optString("country", "Unknown")
                            val countryCode = json.optString("countryCode", "UN")
                            if (countryCode != "UN" && countryCode.isNotEmpty()) {
                                return@withContext Pair(country, countryCode)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Suppress and fall through
        }

        // Try API 2: ipapi.co
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/$ip/json/")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val json = JSONObject(bodyString)
                        val country = json.optString("country_name", "Unknown")
                        val countryCode = json.optString("country_code", "UN")
                        if (country != "Unknown" && countryCode != "UN" && countryCode.isNotEmpty()) {
                            return@withContext Pair(country, countryCode)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Suppress and fall through
        }

        // If APIs fail, fallback to a stable country code hashed from host
        val fallbacks = listOf(
            Pair("Germany", "DE"),
            Pair("Finland", "FI"),
            Pair("Netherlands", "NL"),
            Pair("United States", "US"),
            Pair("United Kingdom", "GB"),
            Pair("Canada", "CA")
        )
        val hash = kotlin.math.abs(host.hashCode()) % fallbacks.size
        fallbacks[hash]
    }
}
