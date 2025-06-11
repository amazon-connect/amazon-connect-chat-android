// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils.logger

import android.util.Log

class SDKLoggerImpl(
    private val tag: String = "ChatSDK",
 ) : ChatSDKLogger {
    private var loggingEnabled: Boolean = false

    /**
     * Sets whether logging is enabled for this logger instance.
     * @param enabled true to enable logging, false to disable.
     */
    override fun setLoggingEnabled(enabled: Boolean) {
        this.loggingEnabled = enabled
    }

    override fun logVerbose(message: () -> String) {
        if (loggingEnabled) {
            Log.v(tag, message())
        }
    }

    override fun logInfo(message: () -> String) {
        if (loggingEnabled) {
            Log.i(tag, message())
        }
    }

    override fun logDebug(message: () -> String) {
        if (loggingEnabled) {
            Log.d(tag, message())
        }
    }

    override fun logWarn(message: () -> String) {
        if (loggingEnabled) {
            Log.w(tag, message())
        }
    }

    override fun logError(message: () -> String) {
        if (loggingEnabled) {
            Log.e(tag, message())
        }
    }
}