// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Region
import com.amazon.connect.chat.sdk.model.View
import com.amazon.connect.chat.sdk.model.ViewContent
import com.amazon.connect.chat.sdk.utils.Constants
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.connectparticipant.ConnectParticipantClient
import aws.sdk.kotlin.services.connectparticipant.auth.ConnectParticipantAuthSchemeParameters
import aws.sdk.kotlin.services.connectparticipant.auth.ConnectParticipantAuthSchemeProvider
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadResponse
import aws.sdk.kotlin.services.connectparticipant.model.ConnectionType
import aws.sdk.kotlin.services.connectparticipant.model.CreateParticipantConnectionRequest
import aws.sdk.kotlin.services.connectparticipant.model.DescribeViewRequest
import aws.sdk.kotlin.services.connectparticipant.model.DescribeViewResponse
import aws.sdk.kotlin.services.connectparticipant.model.DisconnectParticipantRequest
import aws.sdk.kotlin.services.connectparticipant.model.DisconnectParticipantResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetAttachmentRequest
import aws.sdk.kotlin.services.connectparticipant.model.GetAttachmentResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptRequest
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendEventRequest
import aws.sdk.kotlin.services.connectparticipant.model.SendEventResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendMessageRequest
import aws.sdk.kotlin.services.connectparticipant.model.SendMessageResponse
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadResponse
import aws.smithy.kotlin.runtime.auth.AuthOption
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.http.auth.AnonymousAuthScheme
import kotlin.time.Duration.Companion.milliseconds
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
 *     override suspend fun sendMessage(...): Result<SendMessageResponse> {
 *         Log.d("MyClient", "Intercepting sendMessage")
 *         return super.sendMessage(...) // or route to your backend
 *     }
 * }
 *
 * val config = GlobalConfig(
 *     region = Region.US_WEST_2,
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
    open suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResponse> {
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
    ): Result<SendMessageResponse> {
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
    ): Result<SendEventResponse> {
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
    ): Result<StartAttachmentUploadResponse> {
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
    ): Result<CompleteAttachmentUploadResponse> {
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
    ): Result<GetAttachmentResponse> {
        return DefaultAWSClient.getAttachment(connectionToken, attachmentId)
    }

    /**
     * Retrieves a transcript using a get transcript request.
     * @param request The get transcript request.
     * @return A Result containing the get transcript result if successful, or an exception if an error occurred.
     */
    open suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResponse> {
        return DefaultAWSClient.getTranscript(request)
    }

    /**
     * Retrieves the view for the specified view token.
     * @param connectionToken The connection token.
     * @param viewToken The view token from an interactive message.
     * @return A Result containing the View if successful, or an exception if an error occurred.
     */
    open suspend fun describeView(
        connectionToken: String,
        viewToken: String
    ): Result<View> {
        return DefaultAWSClient.describeView(connectionToken, viewToken)
    }
}

/**
 * DefaultAWSClient is the internal implementation that makes actual AWS SDK calls.
 * This object is used by the open AWSClient class for default behavior.
 */
internal object DefaultAWSClient {
    private const val CONNECTION_TIMEOUT_MILLIS = 15_000
    private const val SOCKET_TIMEOUT_MILLIS = 15_000

    @Volatile
    private var connectParticipantClient: ConnectParticipantClient? = null

    // Region of the client created by configure(); null when the client was supplied through setClient().
    @Volatile
    private var managedClientRegion: Region? = null

    /**
     * Sets the client instance. Primarily used for testing.
     */
    @Synchronized
    internal fun setClient(client: ConnectParticipantClient) {
        connectParticipantClient = client
        managedClientRegion = null
    }

    @Synchronized
    fun configure(config: GlobalConfig) {
        val currentClient = connectParticipantClient
        if (currentClient != null && (managedClientRegion == null || managedClientRegion == config.region)) {
            return
        }
        connectParticipantClient = createConnectParticipantClient(config.region)
        managedClientRegion = config.region
        // Release the replaced client's HTTP engine and connection pool. Only a client this object
        // created is closed here; one supplied through setClient() is never replaced above.
        currentClient?.close()
    }

    internal fun createConnectParticipantClient(region: Region): ConnectParticipantClient {
        return ConnectParticipantClient {
            this.region = region.getName()
            applicationId = Constants.USER_AGENT_APP_ID
            // The Connect Participant Service authorizes calls with the participant or connection token
            // in the X-Amz-Bearer header and does not use SigV4, so requests are sent unsigned, as they
            // were with the AWS Mobile SDK. The static placeholder credentials keep the client from
            // building the default credential chain, which would otherwise probe the device environment.
            authSchemes = listOf(AnonymousAuthScheme)
            authSchemeProvider = AnonymousAuthSchemeProvider
            credentialsProvider = StaticCredentialsProvider(Credentials(accessKeyId = "", secretAccessKey = ""))
            interceptors += UserAgentProductTokenInterceptor(Constants.USER_AGENT_PRODUCT_TOKEN)
            httpClient {
                connectTimeout = CONNECTION_TIMEOUT_MILLIS.milliseconds
                socketReadTimeout = SOCKET_TIMEOUT_MILLIS.milliseconds
            }
        }
    }

    private object AnonymousAuthSchemeProvider : ConnectParticipantAuthSchemeProvider {
        override suspend fun resolveAuthScheme(params: ConnectParticipantAuthSchemeParameters): List<AuthOption> =
            listOf(AuthOption(AuthSchemeId.Anonymous))
    }

    private fun requireClient(): ConnectParticipantClient {
        return connectParticipantClient
            ?: throw IllegalStateException("AWSClient not configured. Call configure() first.")
    }

    suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = CreateParticipantConnectionRequest {
                    type = Constants.ACPS_REQUEST_TYPES.map { ConnectionType.fromValue(it) }
                    this.participantToken = participantToken
                }
                val response = requireClient().createParticipantConnection(request)
                val websocketUrl = response.websocket?.url
                val connectionToken = response.connectionCredentials?.connectionToken
                if (websocketUrl == null || connectionToken == null) {
                    throw IllegalStateException("CreateParticipantConnection response is missing the websocket URL or connection token.")
                }
                ConnectionDetails(
                    websocketUrl = websocketUrl,
                    connectionToken = connectionToken,
                    expiry = response.websocket?.connectionExpiry.orEmpty()
                )
            }
        }
    }

    suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = DisconnectParticipantRequest {
                    this.connectionToken = connectionToken
                }
                requireClient().disconnectParticipant(request)
            }
        }
    }

    suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = SendMessageRequest {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    this.content = message
                }
                requireClient().sendMessage(request)
            }
        }
    }

    suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = SendEventRequest {
                    this.connectionToken = connectionToken
                    this.contentType = contentType.type
                    // The service rejects an empty Content string. The AWS Mobile SDK left empty strings
                    // out of the request body, so events without content, such as typing, must omit it.
                    this.content = content.ifEmpty { null }
                }
                requireClient().sendEvent(request)
            }
        }
    }

    suspend fun startAttachmentUpload(
        connectionToken: String,
        request: StartAttachmentUploadRequest
    ): Result<StartAttachmentUploadResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                requireClient().startAttachmentUpload(request)
            }
        }
    }

    suspend fun completeAttachmentUpload(
        connectionToken: String,
        request: CompleteAttachmentUploadRequest
    ): Result<CompleteAttachmentUploadResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                requireClient().completeAttachmentUpload(request)
            }
        }
    }

    suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = GetAttachmentRequest {
                    this.connectionToken = connectionToken
                    this.attachmentId = attachmentId
                }
                requireClient().getAttachment(request)
            }
        }
    }

    suspend fun getTranscript(request: GetTranscriptRequest): Result<GetTranscriptResponse> {
        return withContext(Dispatchers.IO) {
            runCatching {
                requireClient().getTranscript(request)
            }
        }
    }

    suspend fun describeView(
        connectionToken: String,
        viewToken: String
    ): Result<View> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = DescribeViewRequest {
                    this.connectionToken = connectionToken
                    this.viewToken = viewToken
                }
                val response = requireClient().describeView(request)
                mapDescribeViewResponse(response)
            }
        }
    }

    private fun mapDescribeViewResponse(response: DescribeViewResponse): View {
        val awsView = response.view ?: return View()
        val awsContent = awsView.content

        val viewContent = awsContent?.let {
            ViewContent(
                actions = it.actions,
                inputSchema = it.inputSchema,
                template = it.template
            )
        }

        return View(
            arn = awsView.arn,
            content = viewContent,
            id = awsView.id,
            name = awsView.name,
            version = awsView.version
        )
    }
}
