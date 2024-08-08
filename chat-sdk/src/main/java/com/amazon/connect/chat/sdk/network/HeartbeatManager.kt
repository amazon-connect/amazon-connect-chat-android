package com.amazon.connect.chat.sdk.network

import kotlin.concurrent.timer
import java.util.Timer

class HeartbeatManager(
    val sendHeartbeatCallback: () -> Unit,
    val missedHeartbeatCallback: () -> Unit
) {
    private var pendingResponse: Boolean = false
    private var timer: Timer? = null

    fun startHeartbeat() {
        timer?.cancel()
        pendingResponse = false
        timer = timer(period = 10000) {
            if (!pendingResponse) {
                sendHeartbeatCallback()
                pendingResponse = true
            } else {
                timer?.cancel()
                missedHeartbeatCallback()
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