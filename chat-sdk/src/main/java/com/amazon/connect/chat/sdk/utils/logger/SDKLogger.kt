// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils.logger

object SDKLogger {
    var logger: ChatSDKLogger = SDKLoggerImpl()

    fun configureLogger(logger: ChatSDKLogger) {
        this.logger = logger
    }
}