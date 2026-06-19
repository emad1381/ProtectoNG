package com.example.architecture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object StatisticsModule {
    private val _downloadSpeed = MutableStateFlow(0L) // bytes/sec
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0L) // bytes/sec
    val uploadSpeed: StateFlow<Long> = _uploadSpeed.asStateFlow()

    private val _sessionDownload = MutableStateFlow(0L) // bytes
    val sessionDownload: StateFlow<Long> = _sessionDownload.asStateFlow()

    private val _sessionUpload = MutableStateFlow(0L) // bytes
    val sessionUpload: StateFlow<Long> = _sessionUpload.asStateFlow()

    private val _totalSessionUsage = MutableStateFlow(0L) // bytes
    val totalSessionUsage: StateFlow<Long> = _totalSessionUsage.asStateFlow()

    fun resetStats() {
        _downloadSpeed.value = 0L
        _uploadSpeed.value = 0L
        _sessionDownload.value = 0L
        _sessionUpload.value = 0L
        _totalSessionUsage.value = 0L
    }

    private var dlBytesInWindow = 0L
    private var ulBytesInWindow = 0L

    @Synchronized
    fun tickUpTunnelTraffic(dlBytes: Long, ulBytes: Long) {
        dlBytesInWindow += dlBytes
        ulBytesInWindow += ulBytes
        
        val newDl = _sessionDownload.value + dlBytes
        val newUl = _sessionUpload.value + ulBytes
        _sessionDownload.value = newDl
        _sessionUpload.value = newUl
        _totalSessionUsage.value = newDl + newUl
    }

    @Synchronized
    fun consumeSpeedWindow(): Pair<Long, Long> {
        val speeds = Pair(dlBytesInWindow, ulBytesInWindow)
        dlBytesInWindow = 0L
        ulBytesInWindow = 0L
        _downloadSpeed.value = speeds.first
        _uploadSpeed.value = speeds.second
        return speeds
    }

    fun tickUpTraffic(dlBytes: Long, ulBytes: Long) {
        tickUpTunnelTraffic(dlBytes, ulBytes)
    }

    /**
     * Formats bytes to dynamic human readable units (B, KB, MB, GB)
     */
    fun formatBytes(bytes: Long, isSpeed: Boolean = false, isFarsi: Boolean = false): String {
        val suffix = if (isSpeed) "/s" else ""
        val resultStr = when {
            bytes < 1024 -> "$bytes B$suffix"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB$suffix", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB$suffix", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB$suffix", bytes / (1024.0 * 1024.0 * 1024.0))
        }

        if (isFarsi) {
            // Translate speed and data size keywords for robust UX
            return resultStr
                .replace("KB/s", "کیلوبایت/ثانیه")
                .replace("MB/s", "مگابایت/ثانیه")
                .replace("GB/s", "گیگابایت/ثانیه")
                .replace("KB", "کیلوبایت")
                .replace("MB", "مگابایت")
                .replace("GB", "گیگابایت")
        }
        return resultStr
    }
}
