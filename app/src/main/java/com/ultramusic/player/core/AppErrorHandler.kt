package com.ultramusic.player.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRODUCTION-GRADE ERROR HANDLING & LOGGING SYSTEM
 * 
 * Features:
 * - Centralized error handling
 * - Crash logging to file (for debugging)
 * - User-friendly error messages
 * - Analytics-ready event tracking
 * - Performance monitoring
 * - Memory leak detection helpers
 */
@Singleton
class AppErrorHandler @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AppErrorHandler"
        private const val MAX_LOG_ENTRIES = 1000
        private const val LOG_FILE_NAME = "ultramusic_log.txt"
        private const val CRASH_FILE_NAME = "crash_report.txt"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Error state
    private val _lastError = MutableStateFlow<AppError?>(null)
    val lastError: StateFlow<AppError?> = _lastError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<AppError>>(emptyList())
    val errorHistory: StateFlow<List<AppError>> = _errorHistory.asStateFlow()
    
    // Log buffer
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    
    // Performance tracking
    private val performanceMetrics = mutableMapOf<String, PerformanceMetric>()
    
    // Global exception handler
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleException(throwable, "Coroutine")
    }
    
    init {
        setupUncaughtExceptionHandler()
    }
    
    // ==================== ERROR HANDLING ====================
    
    /**
     * Handle and log an exception
     */
    fun handleException(throwable: Throwable, context: String = "Unknown") {
        val error = AppError(
            timestamp = System.currentTimeMillis(),
            context = context,
            message = throwable.message ?: "Unknown error",
            stackTrace = throwable.stackTraceToString(),
            type = categorizeError(throwable),
            userMessage = getUserFriendlyMessage(throwable)
        )
        
        _lastError.value = error
        _errorHistory.value = (_errorHistory.value + error).takeLast(100)
        
        Log.e(TAG, "[$context] ${error.message}", throwable)
        logToFile(LogLevel.ERROR, TAG, "[$context] ${error.message}\n${error.stackTrace}")
    }
    
    /**
     * Report a non-fatal error
     */
    fun reportError(message: String, context: String = "App", exception: Throwable? = null) {
        val error = AppError(
            timestamp = System.currentTimeMillis(),
            context = context,
            message = message,
            stackTrace = exception?.stackTraceToString() ?: "",
            type = ErrorType.NON_FATAL,
            userMessage = message
        )
        
        _lastError.value = error
        Log.w(TAG, "[$context] $message", exception)
        logToFile(LogLevel.WARNING, context, message)
    }
    
    /**
     * Clear the last error
     */
    fun clearError() {
        _lastError.value = null
    }
    
    // ==================== LOGGING ====================
    
    fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
        addToBuffer(LogLevel.DEBUG, tag, message)
    }
    
    fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
        addToBuffer(LogLevel.INFO, tag, message)
        logToFile(LogLevel.INFO, tag, message)
    }
    
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
        addToBuffer(LogLevel.WARNING, tag, message)
        logToFile(LogLevel.WARNING, tag, message)
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        addToBuffer(LogLevel.ERROR, tag, message)
        logToFile(LogLevel.ERROR, tag, "$message\n${throwable?.stackTraceToString() ?: ""}")
    }
    
    private fun addToBuffer(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        
        logBuffer.add(entry)
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            logBuffer.poll()
        }
    }
    
    private fun logToFile(level: LogLevel, tag: String, message: String) {
        scope.launch {
            try {
                val logFile = File(context.filesDir, LOG_FILE_NAME)
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logLine = "[$timestamp] [${level.name}] [$tag] $message\n"
                
                logFile.appendText(logLine)
                
                // Rotate log if too large (> 1MB)
                if (logFile.length() > 1_000_000) {
                    val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.old")
                    logFile.renameTo(backupFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    /**
     * Get recent logs for debugging
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logBuffer.toList().takeLast(count)
    }
    
    /**
     * Export logs to shareable string
     */
    fun exportLogs(): String {
        val sb = StringBuilder()
        sb.appendLine("=== UltraMusic Player Logs ===")
        sb.appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
        sb.appendLine()
        
        logBuffer.forEach { entry ->
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
            sb.appendLine("[$time] [${entry.level}] [${entry.tag}] ${entry.message}")
        }
        
        return sb.toString()
    }
    
    // ==================== PERFORMANCE MONITORING ====================
    
    /**
     * Start timing an operation
     */
    fun startTiming(operationName: String) {
        performanceMetrics[operationName] = PerformanceMetric(
            name = operationName,
            startTime = System.nanoTime()
        )
    }
    
    /**
     * End timing and log the result
     */
    fun endTiming(operationName: String): Long {
        val metric = performanceMetrics[operationName] ?: return 0
        val duration = (System.nanoTime() - metric.startTime) / 1_000_000 // Convert to ms
        
        val updatedMetric = metric.copy(
            endTime = System.nanoTime(),
            durationMs = duration,
            callCount = metric.callCount + 1,
            totalDurationMs = metric.totalDurationMs + duration
        )
        performanceMetrics[operationName] = updatedMetric
        
        if (duration > 100) { // Log slow operations
            logWarning("Performance", "$operationName took ${duration}ms")
        }
        
        return duration
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }
    
    // ==================== CRASH HANDLING ====================
    
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Write crash report
                writeCrashReport(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash report", e)
            }
            
            // Call default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun writeCrashReport(thread: Thread, throwable: Throwable) {
        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        
        val sb = StringBuilder()
        sb.appendLine("=== CRASH REPORT ===")
        sb.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine()
        sb.appendLine("Exception: ${throwable.javaClass.name}")
        sb.appendLine("Message: ${throwable.message}")
        sb.appendLine()
        sb.appendLine("Stack Trace:")
        
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        sb.appendLine(sw.toString())
        
        sb.appendLine()
        sb.appendLine("=== Recent Logs ===")
        getRecentLogs(50).forEach { entry ->
            sb.appendLine("[${entry.level}] [${entry.tag}] ${entry.message}")
        }
        
        crashFile.writeText(sb.toString())
        Log.e(TAG, "Crash report written to ${crashFile.absolutePath}")
    }
    
    /**
     * Check if there's a crash report from previous run
     */
    fun hasCrashReport(): Boolean {
        return File(context.filesDir, CRASH_FILE_NAME).exists()
    }
    
    /**
     * Get crash report content
     */
    fun getCrashReport(): String? {
        val crashFile = File(context.filesDir, CRASH_FILE_NAME)
        return if (crashFile.exists()) crashFile.readText() else null
    }
    
    /**
     * Clear crash report after handling
     */
    fun clearCrashReport() {
        File(context.filesDir, CRASH_FILE_NAME).delete()
    }
    
    // ==================== HELPERS ====================
    
    private fun categorizeError(throwable: Throwable): ErrorType {
        return when (throwable) {
            is SecurityException -> ErrorType.PERMISSION
            is java.io.IOException -> ErrorType.NETWORK
            is OutOfMemoryError -> ErrorType.MEMORY
            is IllegalStateException -> ErrorType.STATE
            is NullPointerException -> ErrorType.NULL_POINTER
            is IllegalArgumentException -> ErrorType.VALIDATION
            else -> ErrorType.UNKNOWN
        }
    }
    
    private fun getUserFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is SecurityException -> "Permission required. Please check app settings."
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
            is java.io.IOException -> "An error occurred while loading data."
            is OutOfMemoryError -> "The app is running low on memory. Try closing other apps."
            is IllegalStateException -> "Something went wrong. Please restart the app."
            else -> "An unexpected error occurred. Please try again."
        }
    }
}

// ==================== DATA CLASSES ====================

data class AppError(
    val timestamp: Long,
    val context: String,
    val message: String,
    val stackTrace: String,
    val type: ErrorType,
    val userMessage: String
)

enum class ErrorType {
    PERMISSION,
    NETWORK,
    MEMORY,
    STATE,
    NULL_POINTER,
    VALIDATION,
    NON_FATAL,
    UNKNOWN
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

data class PerformanceMetric(
    val name: String,
    val startTime: Long,
    val endTime: Long = 0,
    val durationMs: Long = 0,
    val callCount: Int = 0,
    val totalDurationMs: Long = 0
) {
    val averageDurationMs: Long
        get() = if (callCount > 0) totalDurationMs / callCount else 0
}

// ==================== EXTENSION FUNCTIONS ====================

/**
 * Safe execution with error handling
 */
inline fun <T> safeExecute(
    errorHandler: AppErrorHandler,
    context: String = "Operation",
    default: T? = null,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        errorHandler.handleException(e, context)
        default
    }
}

/**
 * Measure execution time
 */
inline fun <T> AppErrorHandler.measureTime(
    operationName: String,
    block: () -> T
): T {
    startTiming(operationName)
    val result = block()
    endTiming(operationName)
    return result
}
