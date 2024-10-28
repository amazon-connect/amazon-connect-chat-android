// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Region
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionRequest
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantRequest
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import com.amazonaws.services.connectparticipant.model.GetAttachmentRequest
import com.amazonaws.services.connectparticipant.model.GetAttachmentResult
import com.amazonaws.services.connectparticipant.model.GetTranscriptRequest
import com.amazonaws.services.connectparticipant.model.GetTranscriptResult
import com.amazonaws.services.connectparticipant.model.SendEventRequest
import com.amazonaws.services.connectparticipant.model.SendEventResult
import com.amazonaws.services.connectparticipant.model.SendMessageRequest
import com.amazonaws.services.connectparticipant.model.SendMessageResult
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadResult
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

    /**
     * Sends a message using a connection token.
     * @param connectionToken The connection token.
     * @param contentType The content type of the message.
     * @param message The message content.
     * @return A Result containing the send message result if successful, or an exception if an error occurred.
     */
    suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult>

    /**
     * Sends an event using a connection token.
     * @param connectionToken The connection token.
     * @param contentType The content type of the event.
     * @param content The event content.
     * @return A Result containing the send event result if successful, or an exception if an error occurred.
     */
    suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult>

    /**
     * Starts an attachment upload using a connection token.
     * @param connectionToken The connection token.
     * @param request The start attachment upload request.
     * @return A Result containing the start attachment upload result if successful, or an exception if an error occurred.
     */
    suspend fun startAttachmentUpload(connectionToken: String, request: StartAttachmentUploadRequest): Result<StartAttachmentUploadResult>

    /**
     * Completes an attachment upload using a connection token.
     * @param connectionToken The connection token.
     * @param request The complete attachment upload request.
     * @return A Result containing the complete attachment upload result if successful, or an exception if an error occurred.
     */
    suspend fun completeAttachmentUpload(connectionToken: String, request: CompleteAttachmentUploadRequest): Result<CompleteAttachmentUploadResult>

    /**
     * Retrieves an attachment using a connection token and attachment ID.
     * @param connectionToken The connection token.
     * @param attachmentId The attachment ID.
     * @return A Result containing the get attachment result if successful, or an exception if an error occurred.
     */
    suspend fun getAttachment(connectionToken: String, attachmentId: String): Result<GetAttachmentResult>

    /**
     * Retrieves a transcript using a get transcript request.
     * @param request The get transcript request.
     * @return A Result containing the get transcript result if successful, or an exception if an error occurred.
     */
    suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResult>

}

class AWSClientImpl @Inject constructor(
    private val connectParticipantClient: AmazonConnectParticipantClient
) : AWSClient {

    companion object {
        fun create(): AWSClient {
            // Create an AmazonConnectParticipantClient
            val connectParticipantClient = AmazonConnectParticipantClient()

            return AWSClientImpl(connectParticipantClient)
        }
    }

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
                response
            }
        }
    }

    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = SendMessageRequest().apply {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    this.content = message
                }
                val response = connectParticipantClient.sendMessage(request)
                response
            }
        }
    }

    override suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = SendEventRequest().apply {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    this.content = content
                }
                val response = connectParticipantClient.sendEvent(request)
                response
            }
        }
    }

    override suspend fun startAttachmentUpload(connectionToken: String, request: StartAttachmentUploadRequest): Result<StartAttachmentUploadResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = connectParticipantClient.startAttachmentUpload(request)
                response
            }
        }
    }

    override suspend fun completeAttachmentUpload(connectionToken: String, request: CompleteAttachmentUploadRequest): Result<CompleteAttachmentUploadResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = connectParticipantClient.completeAttachmentUpload(request)
                response
            }
        }
    }

    override suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResult> {
        return withContext(Dispatchers.IO){
            runCatching {
                val request = GetAttachmentRequest().apply {
                    this.connectionToken = connectionToken
                    this.attachmentId = attachmentId
                }
                val response = connectParticipantClient.getAttachment(request)
                response
            }
        }
    }

    override suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val response = connectParticipantClient.getTranscript(request)
                response
            }
        }
    }
}