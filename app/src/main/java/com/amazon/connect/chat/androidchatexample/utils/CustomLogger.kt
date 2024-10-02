package com.amazon.connect.chat.androidchatexample.utils

import com.amazon.connect.chat.sdk.utils.logger.ChatSDKLogger

class CustomLogger : ChatSDKLogger {
    override fun logVerbose(message: () -> String) {
        // Custom logging logic
        println("VERBOSE: ${message()}")
    }

    override fun logInfo(message: () -> String) {
        // Custom logging logic
        println("INFO: ${message()}")
    }

    override fun logDebug(message: () -> String) {
        // Custom logging logic
        println("DEBUG: ${message()}")
    }

    override fun logWarn(message: () -> String) {
        // Custom logging logic
        println("WARN: ${message()}")
    }

    override fun logError(message: () -> String) {
        // Custom logging logic
        println("ERROR: ${message()}")
    }
}
