package com.example.architecture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingModule {
    
    /**
     * Measures the TCP handshake response time to the specified server and port.
     * Returns the RTT in milliseconds, or -1 in case of timeout or connection error.
     */
    suspend fun measurePing(address: String, port: Int, timeoutMs: Int = 3000): Int = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMs)
            }
            val rtt = (System.currentTimeMillis() - startTime).toInt()
            rtt
        } catch (e: Exception) {
            e.printStackTrace()
            -1 // Fail/Timeout
        }
    }
}
