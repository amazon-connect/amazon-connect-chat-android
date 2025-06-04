// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils.logger

// This can be used externally to create a custom logger

interface ChatSDKLogger {
    fun logVerbose(message: () -> String)
    fun logInfo(message: () -> String)
    fun logDebug(message: () -> String)
    fun logWarn(message: () -> String)
    fun logError(message: () -> String)
    fun setLoggingEnabled(enabled: Boolean)
}