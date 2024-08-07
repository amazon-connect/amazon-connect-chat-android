package com.amazon.connect.chat.sdk.network

import android.util.Log
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.Constants
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazonaws.regions.Region
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionRequest
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantRequest
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AWSClient {
    fun configure(config: GlobalConfig)
    /**
     * Creates a participant connection for the given participant token.
     * @param participantToken The participant token.
     * @return A Result containing the connection details if successful, or an exception if an error occurred.
     */
    suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails>

    /**
     * Disconnects the participant connection for the given connection token.
     * @param connectionToken The connection token.
     * @return A Result containing the disconnect participant result if successful, or an exception if an error occurred.
     */
    suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResult>
}

class AWSClientImpl @Inject constructor(
    private val connectParticipantClient: AmazonConnectParticipantClient
) : AWSClient {

    override fun configure(config: GlobalConfig) {
        connectParticipantClient.setRegion(Region.getRegion(config.region))
    }

    override suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = CreateParticipantConnectionRequest().apply {
                    setType(Constants.ACPS_REQUEST_TYPES)
                    this.participantToken = participantToken
                }
                val response = connectParticipantClient.createParticipantConnection(request)
                val connectionDetails = ConnectionDetails(
                    websocketUrl = response.websocket.url,
                    connectionToken = response.connectionCredentials.connectionToken,
                    expiry = response.websocket.connectionExpiry
                )
                Log.d("AWSClientImpl", "createParticipantConnection: $connectionDetails")
                connectionDetails // Ensure this is the last expression
            }
        }
    }

    override suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = DisconnectParticipantRequest().apply {
                    this.connectionToken = connectionToken
                }
                val response = connectParticipantClient.disconnectParticipant(request)
                Log.d("AWSClientImpl", "disconnectParticipantConnection: $response")
                response
            }
        }
    }
}