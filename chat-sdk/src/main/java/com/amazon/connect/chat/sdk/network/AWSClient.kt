// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.utils.CommonUtils
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

/**
 * AWSClient is an open class that provides default implementations for all Amazon Connect
 * Participant Service (ACPS) API calls. Customers can subclass this to override specific
 * methods for custom routing (e.g., through a backend proxy with certificate pinning).
 *
 * Usage patterns:
 * 1. Partial override: Subclass and override only specific methods for logging/monitoring
 * 2. Complete override: Subclass and override all methods for full backend proxy routing
 *
 * Example:
 * ```kotlin
 * class MyCustomClient : AWSClient() {
 *     override suspend fun sendMessage(...): Result<SendMessageResult> {
 *         Log.d("MyClient", "Intercepting sendMessage")
 *         return super.sendMessage(...) // or route to your backend
 *     }
 * }
 *
 * val config = GlobalConfig(
 *     region = Regions.US_WEST_2,
 *     customAWSClient = MyCustomClient()
 * )
 * chatSession.configure(config)
 * ```
 */
open class AWSClient {

    companion object {
        /**
         * Shared singleton instance of AWSClient.
         * This follows the iOS SDK pattern for consistency.
         */
        val shared: AWSClient = AWSClient()
    }

    /**
     * Configures the AWS client with the given global configuration.
     * @param config The global configuration containing region and other settings.
     */
    open fun configure(config: GlobalConfig) {
        DefaultAWSClient.configure(config)
    }

    /**
     * Creates a participant connection for the given participant token.
     * @param participantToken The participant token.
     * @return A Result containing the connection details if successful, or an exception if an error occurred.
     */
    open suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        return DefaultAWSClient.createParticipantConnection(participantToken)
    }

    /**
     * Disconnects the participant connection for the given connection token.
     * @param connectionToken The connection token.
     * @return A Result containing the disconnect participant result if successful, or an exception if an error occurred.
     */
    open suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResult> {
        return DefaultAWSClient.disconnectParticipantConnection(connectionToken)
    }

    /**
     * Sends a message using a connection token.
     * @param connectionToken The connection token.
     * @param contentType The content type of the message.
     * @param message The message content.
     * @return A Result containing the send message result if successful, or an exception if an error occurred.
     */
    open suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        return DefaultAWSClient.sendMessage(connectionToken, contentType, message)
    }

    /**
     * Sends an event using a connection token.
     * @param connectionToken The connection token.
     * @param contentType The content type of the event.
     * @param content The event content.
     * @return A Result containing the send event result if successful, or an exception if an error occurred.
     */
    open suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult> {
        return DefaultAWSClient.sendEvent(connectionToken, contentType, content)
    }

    /**
     * Starts an attachment upload using a connection token.
     * @param connectionToken The connection token.
     * @param request The start attachment upload request.
     * @return A Result containing the start attachment upload result if successful, or an exception if an error occurred.
     */
    open suspend fun startAttachmentUpload(
        connectionToken: String,
        request: StartAttachmentUploadRequest
    ): Result<StartAttachmentUploadResult> {
        return DefaultAWSClient.startAttachmentUpload(connectionToken, request)
    }

    /**
     * Completes an attachment upload using a connection token.
     * @param connectionToken The connection token.
     * @param request The complete attachment upload request.
     * @return A Result containing the complete attachment upload result if successful, or an exception if an error occurred.
     */
    open suspend fun completeAttachmentUpload(
        connectionToken: String,
        request: CompleteAttachmentUploadRequest
    ): Result<CompleteAttachmentUploadResult> {
        return DefaultAWSClient.completeAttachmentUpload(connectionToken, request)
    }

    /**
     * Retrieves an attachment using a connection token and attachment ID.
     * @param connectionToken The connection token.
     * @param attachmentId The attachment ID.
     * @return A Result containing the get attachment result if successful, or an exception if an error occurred.
     */
    open suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResult> {
        return DefaultAWSClient.getAttachment(connectionToken, attachmentId)
    }

    /**
     * Retrieves a transcript using a get transcript request.
     * @param request The get transcript request.
     * @return A Result containing the get transcript result if successful, or an exception if an error occurred.
     */
    open suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResult> {
        return DefaultAWSClient.getTranscript(request)
    }
}

/**
 * DefaultAWSClient is the internal implementation that makes actual AWS SDK calls.
 * This object is used by the open AWSClient class for default behavior.
 */
internal object DefaultAWSClient {
    private var connectParticipantClient: AmazonConnectParticipantClient? = null

    /**
     * Sets the client instance. Primarily used for testing.
     */
    internal fun setClient(client: AmazonConnectParticipantClient) {
        connectParticipantClient = client
    }

    fun configure(config: GlobalConfig) {
        if (connectParticipantClient == null) {
            val clientConfiguration = CommonUtils.createConnectParticipantConfiguration()
            connectParticipantClient = AmazonConnectParticipantClient(clientConfiguration)
        }
        connectParticipantClient?.setRegion(Region.getRegion(config.region))
    }

    suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                
                val request = CreateParticipantConnectionRequest().apply {
                    setType(Constants.ACPS_REQUEST_TYPES)
                    this.participantToken = participantToken
                }
                val response = client.createParticipantConnection(request)
                ConnectionDetails(
                    websocketUrl = response.websocket.url,
                    connectionToken = response.connectionCredentials.connectionToken,
                    expiry = response.websocket.connectionExpiry
                )
            }
        }
    }

    suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                
                val request = DisconnectParticipantRequest().apply {
                    this.connectionToken = connectionToken
                }
                client.disconnectParticipant(request)
            }
        }
    }

    suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                
                val request = SendMessageRequest().apply {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    this.content = message
                }
                client.sendMessage(request)
            }
        }
    }

    suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                
                val request = SendEventRequest().apply {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    this.content = content
                }
                client.sendEvent(request)
            }
        }
    }

    suspend fun startAttachmentUpload(
        connectionToken: String,
        request: StartAttachmentUploadRequest
    ): Result<StartAttachmentUploadResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                client.startAttachmentUpload(request)
            }
        }
    }

    suspend fun completeAttachmentUpload(
        connectionToken: String,
        request: CompleteAttachmentUploadRequest
    ): Result<CompleteAttachmentUploadResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                client.completeAttachmentUpload(request)
            }
        }
    }

    suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                
                val request = GetAttachmentRequest().apply {
                    this.connectionToken = connectionToken
                    this.attachmentId = attachmentId
                }
                client.getAttachment(request)
            }
        }
    }

    suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val client = connectParticipantClient
                    ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
                client.getTranscript(request)
            }
        }
    }
}
