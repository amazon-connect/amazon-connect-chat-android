package com.amazon.connect.chat.androidchatexample.utils

import com.amazon.connect.chat.sdk.utils.logger.ChatSDKLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.exists

class CustomLogger(
    private val externalFileDir: File
) : ChatSDKLogger {
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + Dispatchers.IO)

    private val currentTimeMillis = System.currentTimeMillis()
    private val loggerCreationDateAndTime = formatDate(currentTimeMillis, false)

    override fun logVerbose(message: () -> String) {
        // Custom logging logic
        val logMessage = "VERBOSE: ${message()}"
        println(logMessage)
        coroutineScope.launch {
            writeToAppTempFile(logMessage)
        }
    }

    override fun logInfo(message: () -> String) {
        // Custom logging logic
        val logMessage = "INFO: ${message()}"
        println(logMessage)
        coroutineScope.launch {
            writeToAppTempFile(logMessage)
        }
    }

    override fun logDebug(message: () -> String) {
        // Custom logging logic
        val logMessage = "DEBUG: ${message()}"
        println(logMessage)
        coroutineScope.launch {
            writeToAppTempFile(logMessage)
        }

    }

    override fun logWarn(message: () -> String) {
        // Custom logging logic
        val logMessage = "WARN: ${message()}"
        println(logMessage)
        coroutineScope.launch {
            writeToAppTempFile(logMessage)
        }
    }

    override fun logError(message: () -> String) {
        // Custom logging logic
        val logMessage = "ERROR: ${message()}"
        println(logMessage)
        coroutineScope.launch {
            writeToAppTempFile(logMessage)
        }
    }

    private fun formatDate(currentTimeMillis: Long, forLogs: Boolean = false): String {
        val date = Date(currentTimeMillis)
        var utcFormatter: SimpleDateFormat? = null
        if (forLogs) {
            utcFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            utcFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        return utcFormatter.format(date)
    }

    private suspend fun writeToAppTempFile(content: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val currentTimeMillis = System.currentTimeMillis()
                val formattedDateTimeForLogs = formatDate(currentTimeMillis, true)
                val filePath = Path(externalFileDir.absolutePath, "$loggerCreationDateAndTime-amazon-connect-logs.txt")

                if (!filePath.exists()) {
                    filePath.createFile()
                }

                filePath.appendText("[$formattedDateTimeForLogs] $content \n")
                true
            }
        }
    }
}
