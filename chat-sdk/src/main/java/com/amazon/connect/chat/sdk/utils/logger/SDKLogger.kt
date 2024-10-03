package com.amazon.connect.chat.sdk.utils.logger

object SDKLogger {
    var logger: ChatSDKLogger = SDKLoggerImpl()

    fun configureLogger(logger: ChatSDKLogger) {
        this.logger = logger
    }
}