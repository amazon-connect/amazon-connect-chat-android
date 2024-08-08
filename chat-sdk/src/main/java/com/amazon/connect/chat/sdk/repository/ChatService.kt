package com.amazon.connect.chat.sdk.repository

import android.util.Log
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import javax.inject.Inject

interface ChatService {
    fun configure(config: GlobalConfig)
    /**
     * Creates a chat session with the specified chat details.
     * @param chatDetails The details of the chat.
     * @return A Result indicating whether the session creation was successful.
     */
    suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean>

    /**
     * Disconnects the current chat session.
     * @return A Result indicating whether the disconnection was successful.
     */
    suspend fun disconnectChatSession(): Result<Unit>
}

class ChatServiceImpl @Inject constructor(
    private val apiClient: APIClient,
    private val awsClient: AWSClient,
    private val connectionDetailsProvider: ConnectionDetailsProvider) : ChatService {

    override fun configure(config: GlobalConfig) {
        awsClient.configure(config)
    }

    override suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean> {
        return runCatching {
            connectionDetailsProvider.updateChatDetails(chatDetails)
            val connectionDetails = awsClient.createParticipantConnection(chatDetails.participantToken).getOrThrow()
            connectionDetailsProvider.updateConnectionDetails(connectionDetails)
            Log.d("ChatServiceImpl", "Participant Connected")
            true
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to create chat session: ${exception.message}", exception)
        }
    }

    override suspend fun disconnectChatSession(): Result<Unit> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            awsClient.disconnectParticipantConnection(connectionDetails.connectionToken).getOrThrow()
            Log.d("ChatServiceImpl", "Participant Disconnected")
            Unit
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to disconnect participant: ${exception.message}", exception)
        }
    }

}