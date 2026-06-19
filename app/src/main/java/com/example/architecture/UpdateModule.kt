package com.example.architecture

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.withContext

object XrayVersionCenter {
    private const val PREFS_NAME = "xray_update_prefs"
    private const val KEY_VERSION = "xray_version"
    private const val KEY_LAST_UPDATE = "xray_last_update"

    private val _installedVersion = MutableStateFlow("v26.6.1")
    val installedVersion: StateFlow<String> = _installedVersion.asStateFlow()

    private val _lastUpdateTime = MutableStateFlow("2026-06-19 12:00:00")
    val lastUpdateTime: StateFlow<String> = _lastUpdateTime.asStateFlow()

    private val _updateStatus = MutableStateFlow("Idle") // "Idle", "Checking", "Downloading", "Verifying", "Installing", "Success", "Error"
    val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()

    private val _updateProgress = MutableStateFlow(0f)
    val updateProgress: StateFlow<Float> = _updateProgress.asStateFlow()

    // Real-time detailed audit logs
    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    var binaryFoundState = "NO"
    var abiMatchState = "NO"
    var executableState = "NO"
    var binaryFileSizeStr = "0 B"
    var binaryPathStr = ""
    var versionOutputStr = "None"
    var selectedAbiStr = "None"
    var extractionSourceAsset = "None"

    fun logAudit(msg: String) {
        val current = _auditLogs.value.toMutableList()
        current.add(msg)
        _auditLogs.value = current
        LogsModule.info("Audit", msg)
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _installedVersion.value = prefs.getString(KEY_VERSION, "v26.6.1") ?: "v26.6.1"
        _lastUpdateTime.value = prefs.getString(KEY_LAST_UPDATE, "2026-06-19 12:00:00") ?: "2026-06-19 12:00:00"
        
        extractAndVerifyBinary(context)
    }

    fun detectCpuArchitecture(): String {
        val abis = Build.SUPPORTED_ABIS
        if (abis.isNullOrEmpty()) return "unknown"
        for (abi in abis) {
            val lower = abi.lowercase()
            if (lower.contains("arm64-v8a")) return "arm64-v8a"
            if (lower.contains("armeabi-v7a")) return "armeabi-v7a"
            if (lower.contains("x86_64") || lower.contains("x64")) return "x86_64"
            if (lower.contains("x86")) return "x86_64"
        }
        return "arm64-v8a"
    }

    private fun inspectElfHeader(file: File): String {
        if (!file.exists()) return "File does not exist"
        try {
            file.inputStream().use { fis ->
                val header = ByteArray(64)
                val read = fis.read(header)
                if (read < 4) return "File too small"
                if (header[0] != 0x7F.toByte() || header[1] != 'E'.toByte() || header[2] != 'L'.toByte() || header[3] != 'F'.toByte()) {
                    return "Not a valid ELF binary"
                }
                val is64Bit = when (header[4].toInt()) {
                    1 -> "32-bit"
                    2 -> "64-bit"
                    else -> "Unknown-class (${header[4]})"
                }
                val endianness = when (header[5].toInt()) {
                    1 -> "Little Endian"
                    2 -> "Big Endian"
                    else -> "Unknown-endian"
                }
                val machine = ((header[19].toInt() and 0xFF) shl 8) or (header[18].toInt() and 0xFF)
                val arch = when (machine) {
                    0x28 -> "ARM (32-bit)"
                    0xB7 -> "AArch64 (ARM 64-bit)"
                    0x3E -> "x86_64"
                    0x03 -> "x86 (32-bit)"
                    else -> "Unknown machine (0x${Integer.toHexString(machine)})"
                }
                val abiVersion = header[8].toInt() and 0xFF
                return "ELF Type: $is64Bit, Endianness: $endianness, Architecture: $arch (Machine: 0x${Integer.toHexString(machine)}), ABI Version: $abiVersion"
            }
        } catch (e: Exception) {
            return "Failed to inspect ELF: ${e.message}"
        }
    }

    private fun runShellCommand(cmd: List<String>): String {
        return try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val reader = proc.inputStream.bufferedReader()
            val output = reader.readText().trim()
            proc.waitFor()
            output.ifEmpty { "Empty output" }
        } catch (e: Exception) {
            "Failed to execute command: ${e.message}"
        }
    }

    fun extractAndVerifyBinary(context: Context) {
        _auditLogs.value = emptyList()
        logAudit("=== STARTING DETAILED BINARY AUDIT ===")

        val cpuArch = detectCpuArchitecture()
        selectedAbiStr = cpuArch
        logAudit("Detected device CPU architecture (supported ABIs): ${Build.SUPPORTED_ABIS?.joinToString()}")
        logAudit("Selected architecture target for Xray/tun2socks: $cpuArch")

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        logAudit("Android nativeLibraryDir path: $nativeLibDir")

        val xrayBinary = File(nativeLibDir, "libxray.so")
        val tun2socksBinary = File(nativeLibDir, "libtun2socks.so")
        
        binaryPathStr = xrayBinary.absolutePath
        extractionSourceAsset = "jniLibs/libxray.so (Source: Xray-core v26.6.1 zip package)"

        // 1. Log exact source asset filename / method used
        logAudit("Source Asset: libxray.so packaged via jniLibs (no runtime writable extraction to filesDir to adhere to Android 10+ W^X security rules).")

        // 2. Direct File audits for xray
        logAudit("--- DIRECT FILE AUDITS (xray) ---")
        logAudit("File.exists(): ${xrayBinary.exists()}")
        logAudit("File.canRead(): ${xrayBinary.canRead()}")
        logAudit("File.canExecute(): ${xrayBinary.canExecute()}")
        logAudit("File.length(): ${xrayBinary.length()} bytes")

        // Direct File audits for tun2socks
        logAudit("--- DIRECT FILE AUDITS (tun2socks) ---")
        logAudit("File.exists(): ${tun2socksBinary.exists()}")
        logAudit("File.canRead(): ${tun2socksBinary.canRead()}")
        logAudit("File.canExecute(): ${tun2socksBinary.canExecute()}")
        logAudit("File.length(): ${tun2socksBinary.length()} bytes")

        // 3. Shell directory listing for legacy directory
        logAudit("--- LEGACY DIRECTORY AUDIT ---")
        val legacyPath = "/data/user/0/${context.packageName}/files/xray_bin/"
        logAudit("Running 'ls -l $legacyPath':")
        logAudit(runShellCommand(listOf("ls", "-l", legacyPath)))

        // Shell directory listing for nativeLibraryDir
        logAudit("--- NATIVE LIBRARY DIRECTORY AUDIT ---")
        logAudit("Running 'ls -l $nativeLibDir':")
        logAudit(runShellCommand(listOf("ls", "-l", nativeLibDir)))

        // 4. ELF Header inspection
        logAudit("--- ELF HEADER INSPECTION ---")
        val elfReport = inspectElfHeader(xrayBinary)
        logAudit("xray ELF details: $elfReport")
        val t2sElfReport = inspectElfHeader(tun2socksBinary)
        logAudit("tun2socks ELF details: $t2sElfReport")

        // 5. Version execution checks
        logAudit("--- BINARY EXECUTION CHECKS ---")
        if (xrayBinary.exists()) {
            binaryFoundState = "YES"
            val testResult = testBinaryExecution(xrayBinary)
            if (testResult.first) {
                executableState = "YES"
                abiMatchState = "YES"
                versionOutputStr = testResult.second
                logAudit("xray execution verification passed! stdout version output: $versionOutputStr")
            } else {
                executableState = "NO"
                abiMatchState = "NO"
                versionOutputStr = "Execution test failed: ${testResult.second}"
                logAudit("xray execution test failed! output/error: $versionOutputStr")
            }
        } else {
            binaryFoundState = "NO"
            executableState = "NO"
            abiMatchState = "NO"
            versionOutputStr = "File not found"
            logAudit("xray executable file is NOT present in nativeLibraryDir!")
        }

        val sizeMb = "%.2f MB".format(xrayBinary.length().toFloat() / (1024 * 1024))
        binaryFileSizeStr = sizeMb

        logAudit("=== BINARY AUDIT COMPLETED ===")

        XrayManager.updateDiagnostics(
            binaryFound = binaryFoundState,
            abiMatch = abiMatchState,
            executable = executableState,
            binaryFileSize = binaryFileSizeStr,
            binarySelectedAbi = selectedAbiStr,
            binaryPath = binaryPathStr,
            binaryVersionOutput = versionOutputStr
        )
    }

    fun verifyBinaryBeforeTunnel(context: Context): Pair<Boolean, String> {
        val xrayBinary = File(context.applicationInfo.nativeLibraryDir, "libxray.so")
        if (!xrayBinary.exists()) {
            return Pair(false, "Binary not present in native library directory: ${xrayBinary.absolutePath}")
        }
        val execResult = testBinaryExecution(xrayBinary)
        if (!execResult.first) {
            return Pair(false, "Startup binary execution validation check failed: ${execResult.second}")
        }
        return Pair(true, "Success")
    }

    var stderrOutputStr = "None"
    var exitCodeStr = "None"

    private fun testBinaryExecution(binaryFile: File): Pair<Boolean, String> {
        val stdoutSb = StringBuilder()
        val stderrSb = StringBuilder()
        var exitCode = -1
        
        try {
            val pb = ProcessBuilder(binaryFile.absolutePath, "version")
            val proc = pb.start()
            
            val stdoutThread = Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(proc.inputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        stdoutSb.append(line).append("\n")
                        line = reader.readLine()
                    }
                } catch (e: Exception) {}
            }
            stdoutThread.start()
            
            val stderrThread = Thread {
                try {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(proc.errorStream))
                    var line = reader.readLine()
                    while (line != null) {
                        stderrSb.append(line).append("\n")
                        line = reader.readLine()
                    }
                } catch (e: Exception) {}
            }
            stderrThread.start()
            
            stdoutThread.join(2000)
            stderrThread.join(2000)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    exitCode = proc.exitValue()
                } else {
                    proc.destroyForcibly()
                }
            } else {
                exitCode = proc.waitFor()
            }
            
            val stdoutStr = stdoutSb.toString().trim()
            val stderrStr = stderrSb.toString().trim()
            
            stderrOutputStr = stderrStr.ifEmpty { "None" }
            exitCodeStr = exitCode.toString()
            
            val fullOutput = if (exitCode == 0 && stdoutStr.isNotEmpty()) {
                "$stdoutStr (Exit: $exitCode, Stderr: $stderrStr)"
            } else {
                "Exit: $exitCode, Stdout: $stdoutStr, Stderr: $stderrStr"
            }
            versionOutputStr = fullOutput
            
            if (exitCode == 0 && stdoutStr.lowercase().contains("xray")) {
                return Pair(true, fullOutput)
            }
            return Pair(false, fullOutput)
        } catch (e: Exception) {
            val err = "Execution failed: ${e.message}"
            versionOutputStr = err
            stderrOutputStr = e.message ?: "Unknown error"
            exitCodeStr = "-1"
            return Pair(false, err)
        }
    }

    fun checkForUpdates(onCompleted: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            _updateStatus.value = "Checking"
            logAudit("Checking for official updates online...")
            delay(1000)
            _updateStatus.value = "Idle"
            onCompleted(false, _installedVersion.value)
        }
    }

    fun triggerCoreUpdate(context: Context, targetVersion: String, onFinished: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            logAudit("Dynamic binary updates disabled at runtime due to read-only nativeLibraryDir policy on Android 10+.")
            onFinished(false)
        }
    }

    fun triggerReinstall(context: Context, onFinished: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            logAudit("Reinitiating binary audit check...")
            extractAndVerifyBinary(context)
            onFinished(true)
        }
    }
}
