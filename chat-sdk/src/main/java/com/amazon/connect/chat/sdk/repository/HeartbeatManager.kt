// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.timer
import java.util.Timer

class HeartbeatManager(
    val sendHeartbeatCallback: () -> Unit,
    val missedHeartbeatCallback: suspend () -> Unit
) {
    private var pendingResponse: Boolean = false
    private var timer: Timer? = null

    suspend fun startHeartbeat() {
        timer?.cancel()
        pendingResponse = false
        timer = timer(period = 10000) {
            if (!pendingResponse) {
                sendHeartbeatCallback()
                pendingResponse = true
            } else {
                timer?.cancel()
                CoroutineScope(Dispatchers.IO).launch {
                    missedHeartbeatCallback()
                }
            }
        }
    }

    fun stopHeartbeat() {
        timer?.cancel()
        pendingResponse = false
    }

    fun heartbeatReceived() {
        pendingResponse = false
    }
}