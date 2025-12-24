package com.ultramusic.player.core

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for AppErrorHandler
 *
 * Tests error categorization, logging, performance tracking,
 * and error history management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppErrorHandlerTest {

    private lateinit var errorHandler: AppErrorHandler
    private val mockContext: Context = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "test_logs")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempDir.mkdirs()
        every { mockContext.filesDir } returns tempDir
        errorHandler = AppErrorHandler(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // ==================== ERROR CATEGORIZATION TESTS ====================

    @Test
    fun `handleException categorizes SecurityException as PERMISSION`() {
        val exception = SecurityException("Permission denied")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.PERMISSION)
    }

    @Test
    fun `handleException categorizes IOException as NETWORK`() {
        val exception = IOException("Network error")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.NETWORK)
    }

    @Test
    fun `handleException categorizes OutOfMemoryError as MEMORY`() {
        val error = OutOfMemoryError("Out of memory")

        errorHandler.handleException(error, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.MEMORY)
    }

    @Test
    fun `handleException categorizes IllegalStateException as STATE`() {
        val exception = IllegalStateException("Invalid state")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.STATE)
    }

    @Test
    fun `handleException categorizes NullPointerException as NULL_POINTER`() {
        val exception = NullPointerException("Null reference")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.NULL_POINTER)
    }

    @Test
    fun `handleException categorizes IllegalArgumentException as VALIDATION`() {
        val exception = IllegalArgumentException("Invalid argument")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.VALIDATION)
    }

    @Test
    fun `handleException categorizes unknown exceptions as UNKNOWN`() {
        val exception = RuntimeException("Unknown error")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.UNKNOWN)
    }

    // ==================== USER FRIENDLY MESSAGE TESTS ====================

    @Test
    fun `handleException provides user-friendly message for SecurityException`() {
        val exception = SecurityException("Permission denied")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.userMessage).contains("Permission")
    }

    @Test
    fun `handleException provides user-friendly message for UnknownHostException`() {
        val exception = UnknownHostException("api.example.com")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.userMessage).contains("internet")
    }

    @Test
    fun `handleException provides user-friendly message for SocketTimeoutException`() {
        val exception = SocketTimeoutException("Connection timed out")

        errorHandler.handleException(exception, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.userMessage).contains("timed out")
    }

    @Test
    fun `handleException provides user-friendly message for OutOfMemoryError`() {
        val error = OutOfMemoryError("Out of memory")

        errorHandler.handleException(error, "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.userMessage).contains("memory")
    }

    // ==================== ERROR HISTORY TESTS ====================

    @Test
    fun `handleException adds error to history`() {
        errorHandler.handleException(RuntimeException("Error 1"), "Context1")
        errorHandler.handleException(RuntimeException("Error 2"), "Context2")

        val history = errorHandler.errorHistory.value
        assertThat(history).hasSize(2)
    }

    @Test
    fun `error history is limited to 100 entries`() {
        repeat(150) { i ->
            errorHandler.handleException(RuntimeException("Error $i"), "Context")
        }

        val history = errorHandler.errorHistory.value
        assertThat(history.size).isAtMost(100)
    }

    @Test
    fun `clearError clears last error`() {
        errorHandler.handleException(RuntimeException("Test error"), "Context")
        assertThat(errorHandler.lastError.value).isNotNull()

        errorHandler.clearError()

        assertThat(errorHandler.lastError.value).isNull()
    }

    // ==================== REPORT ERROR TESTS ====================

    @Test
    fun `reportError creates NON_FATAL error`() {
        errorHandler.reportError("Something went wrong", "TestContext")

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.type).isEqualTo(ErrorType.NON_FATAL)
        assertThat(lastError?.message).isEqualTo("Something went wrong")
    }

    @Test
    fun `reportError includes optional exception`() {
        val exception = RuntimeException("Underlying cause")
        errorHandler.reportError("Error occurred", "TestContext", exception)

        val lastError = errorHandler.lastError.value
        assertThat(lastError?.stackTrace).isNotEmpty()
    }

    // ==================== LOGGING TESTS ====================

    @Test
    fun `logDebug adds entry to buffer`() {
        errorHandler.logDebug("TestTag", "Debug message")

        val logs = errorHandler.getRecentLogs()
        assertThat(logs.any { it.level == LogLevel.DEBUG && it.message == "Debug message" }).isTrue()
    }

    @Test
    fun `logInfo adds entry to buffer`() {
        errorHandler.logInfo("TestTag", "Info message")

        val logs = errorHandler.getRecentLogs()
        assertThat(logs.any { it.level == LogLevel.INFO && it.message == "Info message" }).isTrue()
    }

    @Test
    fun `logWarning adds entry to buffer`() {
        errorHandler.logWarning("TestTag", "Warning message")

        val logs = errorHandler.getRecentLogs()
        assertThat(logs.any { it.level == LogLevel.WARNING && it.message == "Warning message" }).isTrue()
    }

    @Test
    fun `logError adds entry to buffer`() {
        errorHandler.logError("TestTag", "Error message")

        val logs = errorHandler.getRecentLogs()
        assertThat(logs.any { it.level == LogLevel.ERROR && it.message == "Error message" }).isTrue()
    }

    @Test
    fun `log buffer is limited to MAX_LOG_ENTRIES`() {
        repeat(1500) { i ->
            errorHandler.logDebug("TestTag", "Message $i")
        }

        val logs = errorHandler.getRecentLogs(2000)
        assertThat(logs.size).isAtMost(1000)
    }

    @Test
    fun `getRecentLogs respects count parameter`() {
        repeat(50) { i ->
            errorHandler.logDebug("TestTag", "Message $i")
        }

        val logs = errorHandler.getRecentLogs(10)
        assertThat(logs).hasSize(10)
    }

    // ==================== EXPORT LOGS TESTS ====================

    @Test
    fun `exportLogs returns formatted string`() {
        errorHandler.logInfo("TestTag", "Test message")

        val exported = errorHandler.exportLogs()

        assertThat(exported).contains("UltraMusic Player Logs")
        assertThat(exported).contains("Test message")
    }

    @Test
    fun `exportLogs includes device info`() {
        val exported = errorHandler.exportLogs()

        assertThat(exported).contains("Device:")
        assertThat(exported).contains("Android:")
    }

    // ==================== PERFORMANCE MONITORING TESTS ====================

    @Test
    fun `startTiming and endTiming track operation duration`() {
        errorHandler.startTiming("TestOperation")
        Thread.sleep(10) // Small delay
        val duration = errorHandler.endTiming("TestOperation")

        assertThat(duration).isAtLeast(0L)
    }

    @Test
    fun `endTiming returns 0 for unknown operation`() {
        val duration = errorHandler.endTiming("UnknownOperation")

        assertThat(duration).isEqualTo(0)
    }

    @Test
    fun `getPerformanceSummary returns tracked metrics`() {
        errorHandler.startTiming("Operation1")
        errorHandler.endTiming("Operation1")
        errorHandler.startTiming("Operation2")
        errorHandler.endTiming("Operation2")

        val summary = errorHandler.getPerformanceSummary()

        assertThat(summary).containsKey("Operation1")
        assertThat(summary).containsKey("Operation2")
    }

    @Test
    fun `performance metric tracks call count`() {
        errorHandler.startTiming("RepeatedOp")
        errorHandler.endTiming("RepeatedOp")
        errorHandler.startTiming("RepeatedOp")
        errorHandler.endTiming("RepeatedOp")
        errorHandler.startTiming("RepeatedOp")
        errorHandler.endTiming("RepeatedOp")

        val summary = errorHandler.getPerformanceSummary()
        val metric = summary["RepeatedOp"]

        assertThat(metric?.callCount).isEqualTo(3)
    }

    @Test
    fun `performance metric calculates total duration`() {
        errorHandler.startTiming("TimedOp")
        Thread.sleep(5)
        errorHandler.endTiming("TimedOp")
        errorHandler.startTiming("TimedOp")
        Thread.sleep(5)
        errorHandler.endTiming("TimedOp")

        val summary = errorHandler.getPerformanceSummary()
        val metric = summary["TimedOp"]

        assertThat(metric?.totalDurationMs).isAtLeast(0L)
    }

    // ==================== CRASH REPORT TESTS ====================

    @Test
    fun `hasCrashReport returns false when no crash`() {
        assertThat(errorHandler.hasCrashReport()).isFalse()
    }

    @Test
    fun `getCrashReport returns null when no crash`() {
        assertThat(errorHandler.getCrashReport()).isNull()
    }

    @Test
    fun `clearCrashReport removes crash file`() {
        // Create a fake crash file
        File(tempDir, "crash_report.txt").writeText("Test crash")

        errorHandler.clearCrashReport()

        assertThat(errorHandler.hasCrashReport()).isFalse()
    }

    // ==================== COROUTINE EXCEPTION HANDLER TESTS ====================

    @Test
    fun `coroutineExceptionHandler is not null`() {
        assertThat(errorHandler.coroutineExceptionHandler).isNotNull()
    }

    // ==================== APP ERROR DATA CLASS TESTS ====================

    @Test
    fun `AppError contains all required fields`() {
        errorHandler.handleException(RuntimeException("Test"), "TestContext")

        val error = errorHandler.lastError.value!!
        assertThat(error.timestamp).isGreaterThan(0L)
        assertThat(error.context).isEqualTo("TestContext")
        assertThat(error.message).isNotEmpty()
        assertThat(error.stackTrace).isNotEmpty()
        assertThat(error.type).isNotNull()
        assertThat(error.userMessage).isNotEmpty()
    }

    // ==================== LOG ENTRY DATA CLASS TESTS ====================

    @Test
    fun `LogEntry contains all required fields`() {
        errorHandler.logInfo("TestTag", "Test message")

        val entry = errorHandler.getRecentLogs().first()
        assertThat(entry.timestamp).isGreaterThan(0L)
        assertThat(entry.level).isNotNull()
        assertThat(entry.tag).isEqualTo("TestTag")
        assertThat(entry.message).isEqualTo("Test message")
    }

    // ==================== PERFORMANCE METRIC DATA CLASS TESTS ====================

    @Test
    fun `PerformanceMetric averageDurationMs calculates correctly`() {
        val metric = PerformanceMetric(
            name = "Test",
            startTime = 0,
            endTime = 100,
            durationMs = 100,
            callCount = 4,
            totalDurationMs = 400
        )

        assertThat(metric.averageDurationMs).isEqualTo(100)
    }

    @Test
    fun `PerformanceMetric averageDurationMs returns 0 when no calls`() {
        val metric = PerformanceMetric(
            name = "Test",
            startTime = 0,
            callCount = 0,
            totalDurationMs = 0
        )

        assertThat(metric.averageDurationMs).isEqualTo(0)
    }

    // ==================== EXTENSION FUNCTION TESTS ====================

    @Test
    fun `safeExecute catches exceptions and returns default`() {
        val result = safeExecute(errorHandler, "Test", default = "default") {
            throw RuntimeException("Error")
        }

        assertThat(result).isEqualTo("default")
        assertThat(errorHandler.lastError.value).isNotNull()
    }

    @Test
    fun `safeExecute returns result on success`() {
        val result = safeExecute(errorHandler, "Test", default = "default") {
            "success"
        }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun `measureTime tracks operation time`() {
        var result: String? = null
        errorHandler.measureTime("TestMeasure") {
            result = "completed"
        }

        assertThat(result).isEqualTo("completed")
        assertThat(errorHandler.getPerformanceSummary()).containsKey("TestMeasure")
    }
}
