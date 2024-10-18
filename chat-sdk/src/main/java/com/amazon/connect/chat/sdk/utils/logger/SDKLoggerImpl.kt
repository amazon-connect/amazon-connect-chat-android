// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils.logger

import android.util.Log

class SDKLoggerImpl (private val tag: String = "ChatSDK"): ChatSDKLogger {
    override fun logVerbose(message: () -> String) {
        Log.v(tag, message())
    }

    override fun logInfo(message: () -> String) {
        Log.i(tag, message())
    }

    override fun logDebug(message: () -> String) {
        Log.d(tag, message())
    }

    override fun logWarn(message: () -> String) {
        Log.w(tag, message())
    }

    override fun logError(message: () -> String) {
        Log.e(tag, message())
    }
}