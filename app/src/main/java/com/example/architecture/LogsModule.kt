package com.example.architecture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO, WARNING, ERROR, DEBUG
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: String, // "Connection", "Core", "System", "Update"
    val message: String
) {
    fun format(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val dateStr = sdf.format(Date(timestamp))
        return "[$dateStr] [${level.name}] [$category] $message"
    }
}

object LogsModule {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: LogLevel, category: String, message: String) {
        val entry = LogEntry(level = level, category = category, message = message)
        val current = _logs.value.toMutableList()
        current.add(0, entry) // Newest logs first
        // Limit to last 1000 logs to preserve memory
        if (current.size > 1000) {
            current.removeAt(current.lastIndex)
        }
        _logs.value = current
    }

    fun info(category: String, message: String) = log(LogLevel.INFO, category, message)
    fun warning(category: String, message: String) = log(LogLevel.WARNING, category, message)
    fun error(category: String, message: String) = log(LogLevel.ERROR, category, message)
    fun debug(category: String, message: String) = log(LogLevel.DEBUG, category, message)

    fun clear() {
        _logs.value = emptyList()
    }

    fun getExportText(): String {
        return _logs.value.reversed().joinToString("\n") { it.format() }
    }
}
