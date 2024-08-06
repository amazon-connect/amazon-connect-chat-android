package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import javax.inject.Inject


class ChatService @Inject constructor(
    private val apiClient: APIClient,
    private val awsClient: AWSClient,
    private val connectionDetailProvider: ConnectionDetailProvider) {

    private val webSocketManager: WebSocketManager by lazy {
        WebSocketManager()
    }

    fun initializeSession(userId: String) {
        // Initialize session logic
    }

    fun sendMessage(message: String) {
        // Send message logic
    }

    fun closeSession() {
        // Close session logic
    }

    fun uploadAttachment() {
        // apiClient.uploadAttachment()
    }

    fun sendMetrics() {
        // apiClient.sendMetrics()
    }
}