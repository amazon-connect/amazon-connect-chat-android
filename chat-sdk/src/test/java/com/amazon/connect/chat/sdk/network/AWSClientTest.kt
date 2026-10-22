// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import aws.sdk.kotlin.services.connectparticipant.ConnectParticipantClient
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadResponse
import aws.sdk.kotlin.services.connectparticipant.model.ConnectionCredentials
import aws.sdk.kotlin.services.connectparticipant.model.ConnectionType
import aws.sdk.kotlin.services.connectparticipant.model.CreateParticipantConnectionRequest
import aws.sdk.kotlin.services.connectparticipant.model.CreateParticipantConnectionResponse
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
import aws.sdk.kotlin.services.connectparticipant.model.View
import aws.sdk.kotlin.services.connectparticipant.model.ViewContent
import aws.sdk.kotlin.services.connectparticipant.model.Websocket
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Region
import com.amazon.connect.chat.sdk.utils.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AWSClientTest {

    private lateinit var mockConnectParticipantClient: ConnectParticipantClient

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testParticipantToken = "test-participant-token"
    private val testConnectionToken = "test-connection-token"
    private val testWebsocketUrl = "wss://test-websocket-url"
    private val testExpiry = "2023-12-31T23:59:59Z"
    private val testMessage = "test-message"
    private val testContent = "test-content"
    private val testAttachmentId = "test-attachment-id"
    private val testRegion = Region.US_WEST_2

    @Before
    fun setUp() {
        mockConnectParticipantClient = mockk(relaxed = true)
        DefaultAWSClient.setClient(mockConnectParticipantClient)
    }

    @After
    fun tearDown() {
        DefaultAWSClient.setClient(mockConnectParticipantClient)
    }

    @Test
    fun `test configure keeps an injected client`() = testScope.runTest {
        coEvery { mockConnectParticipantClient.sendEvent(any()) } returns SendEventResponse {}

        DefaultAWSClient.configure(GlobalConfig(region = Region.EU_WEST_2))
        val result = DefaultAWSClient.sendEvent(testConnectionToken, ContentType.TYPING, testContent)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockConnectParticipantClient.sendEvent(any()) }
    }

    @Test
    fun `test configure creates a client for the configured region and reuses it`() {
        val clientField = DefaultAWSClient::class.java.getDeclaredField("connectParticipantClient").apply {
            isAccessible = true
        }
        clientField.set(DefaultAWSClient, null)

        DefaultAWSClient.configure(GlobalConfig(region = Region.EU_WEST_2))
        val first = clientField.get(DefaultAWSClient) as ConnectParticipantClient
        assertEquals("eu-west-2", first.config.region)

        DefaultAWSClient.configure(GlobalConfig(region = Region.EU_WEST_2))
        assertSame(first, clientField.get(DefaultAWSClient))

        DefaultAWSClient.configure(GlobalConfig(region = Region.AP_SOUTHEAST_2))
        val second = clientField.get(DefaultAWSClient) as ConnectParticipantClient
        assertNotSame(first, second)
        assertEquals("ap-southeast-2", second.config.region)
    }

    @Test
    fun `test created client sends unsigned requests with SDK attribution and legacy timeouts`() {
        val client = DefaultAWSClient.createConnectParticipantClient(testRegion)
        val config = client.config

        assertEquals("us-west-2", config.region)
        assertEquals(Constants.USER_AGENT_APP_ID, config.applicationId)
        assertEquals(listOf(AuthSchemeId.Anonymous), config.authSchemes.map { it.schemeId })
        assertTrue(config.interceptors.any { it is UserAgentProductTokenInterceptor })
        assertEquals(15.seconds, config.httpClient.config.connectTimeout)
        assertEquals(15.seconds, config.httpClient.config.socketReadTimeout)
        client.close()
    }

    @Test
    fun `test createParticipantConnection success`() = testScope.runTest {
        val requestSlot = slot<CreateParticipantConnectionRequest>()
        coEvery { mockConnectParticipantClient.createParticipantConnection(capture(requestSlot)) } returns
            CreateParticipantConnectionResponse {
                websocket = Websocket {
                    url = testWebsocketUrl
                    connectionExpiry = testExpiry
                }
                connectionCredentials = ConnectionCredentials {
                    connectionToken = testConnectionToken
                }
            }

        val result = DefaultAWSClient.createParticipantConnection(testParticipantToken)

        assertTrue(result.isSuccess)
        val connectionDetails = result.getOrNull()
        assertEquals(testWebsocketUrl, connectionDetails?.websocketUrl)
        assertEquals(testConnectionToken, connectionDetails?.connectionToken)
        assertEquals(testExpiry, connectionDetails?.expiry)

        assertEquals(testParticipantToken, requestSlot.captured.participantToken)
        assertEquals(
            listOf(ConnectionType.Websocket, ConnectionType.ConnectionCredentials),
            requestSlot.captured.type
        )
        assertEquals(Constants.ACPS_REQUEST_TYPES, requestSlot.captured.type?.map { it.value })
    }

    @Test
    fun `test createParticipantConnection fails when the response has no connection token`() = testScope.runTest {
        coEvery { mockConnectParticipantClient.createParticipantConnection(any()) } returns
            CreateParticipantConnectionResponse {
                websocket = Websocket { url = testWebsocketUrl }
            }

        val result = DefaultAWSClient.createParticipantConnection(testParticipantToken)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `test createParticipantConnection failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.createParticipantConnection(any()) } throws exception

        val result = DefaultAWSClient.createParticipantConnection(testParticipantToken)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test disconnectParticipantConnection success`() = testScope.runTest {
        val response = DisconnectParticipantResponse {}
        val requestSlot = slot<DisconnectParticipantRequest>()
        coEvery { mockConnectParticipantClient.disconnectParticipant(capture(requestSlot)) } returns response

        val result = DefaultAWSClient.disconnectParticipantConnection(testConnectionToken)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals(testConnectionToken, requestSlot.captured.connectionToken)
    }

    @Test
    fun `test disconnectParticipantConnection failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.disconnectParticipant(any()) } throws exception

        val result = DefaultAWSClient.disconnectParticipantConnection(testConnectionToken)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test sendMessage success`() = testScope.runTest {
        val response = SendMessageResponse { id = "message-id" }
        val requestSlot = slot<SendMessageRequest>()
        coEvery { mockConnectParticipantClient.sendMessage(capture(requestSlot)) } returns response
        val contentType = ContentType.PLAIN_TEXT

        val result = DefaultAWSClient.sendMessage(testConnectionToken, contentType, testMessage)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals(testConnectionToken, requestSlot.captured.connectionToken)
        assertEquals(contentType.type, requestSlot.captured.contentType)
        assertEquals(testMessage, requestSlot.captured.content)
    }

    @Test
    fun `test sendMessage failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.sendMessage(any()) } throws exception

        val result = DefaultAWSClient.sendMessage(testConnectionToken, ContentType.PLAIN_TEXT, testMessage)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test sendEvent success`() = testScope.runTest {
        val response = SendEventResponse { id = "event-id" }
        val requestSlot = slot<SendEventRequest>()
        coEvery { mockConnectParticipantClient.sendEvent(capture(requestSlot)) } returns response
        val contentType = ContentType.TYPING

        val result = DefaultAWSClient.sendEvent(testConnectionToken, contentType, testContent)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals(testConnectionToken, requestSlot.captured.connectionToken)
        assertEquals(contentType.type, requestSlot.captured.contentType)
        assertEquals(testContent, requestSlot.captured.content)
    }

    @Test
    fun `test sendEvent failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.sendEvent(any()) } throws exception

        val result = DefaultAWSClient.sendEvent(testConnectionToken, ContentType.TYPING, testContent)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test startAttachmentUpload success`() = testScope.runTest {
        val response = StartAttachmentUploadResponse { attachmentId = testAttachmentId }
        coEvery { mockConnectParticipantClient.startAttachmentUpload(any()) } returns response
        val request = StartAttachmentUploadRequest {
            connectionToken = testConnectionToken
            attachmentName = "test.txt"
            contentType = "text/plain"
        }

        val result = DefaultAWSClient.startAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        coVerify { mockConnectParticipantClient.startAttachmentUpload(request) }
    }

    @Test
    fun `test startAttachmentUpload failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.startAttachmentUpload(any()) } throws exception
        val request = StartAttachmentUploadRequest {
            connectionToken = testConnectionToken
            attachmentName = "test.txt"
            contentType = "text/plain"
        }

        val result = DefaultAWSClient.startAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test completeAttachmentUpload success`() = testScope.runTest {
        val response = CompleteAttachmentUploadResponse {}
        coEvery { mockConnectParticipantClient.completeAttachmentUpload(any()) } returns response
        val request = CompleteAttachmentUploadRequest {
            connectionToken = testConnectionToken
            attachmentIds = listOf(testAttachmentId)
        }

        val result = DefaultAWSClient.completeAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        coVerify { mockConnectParticipantClient.completeAttachmentUpload(request) }
    }

    @Test
    fun `test completeAttachmentUpload failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.completeAttachmentUpload(any()) } throws exception
        val request = CompleteAttachmentUploadRequest {
            connectionToken = testConnectionToken
            attachmentIds = listOf(testAttachmentId)
        }

        val result = DefaultAWSClient.completeAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test getAttachment success`() = testScope.runTest {
        val response = GetAttachmentResponse {
            url = "https://attachment-url"
            attachmentSizeInBytes = 0L
        }
        val requestSlot = slot<GetAttachmentRequest>()
        coEvery { mockConnectParticipantClient.getAttachment(capture(requestSlot)) } returns response

        val result = DefaultAWSClient.getAttachment(testConnectionToken, testAttachmentId)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        assertEquals(testConnectionToken, requestSlot.captured.connectionToken)
        assertEquals(testAttachmentId, requestSlot.captured.attachmentId)
    }

    @Test
    fun `test getAttachment failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.getAttachment(any()) } throws exception

        val result = DefaultAWSClient.getAttachment(testConnectionToken, testAttachmentId)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test getTranscript success`() = testScope.runTest {
        val response = GetTranscriptResponse { initialContactId = "initial-contact-id" }
        coEvery { mockConnectParticipantClient.getTranscript(any()) } returns response
        val request = GetTranscriptRequest {
            connectionToken = testConnectionToken
        }

        val result = DefaultAWSClient.getTranscript(request)

        assertTrue(result.isSuccess)
        assertEquals(response, result.getOrNull())
        coVerify { mockConnectParticipantClient.getTranscript(request) }
    }

    @Test
    fun `test getTranscript failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        coEvery { mockConnectParticipantClient.getTranscript(any()) } throws exception
        val request = GetTranscriptRequest {
            connectionToken = testConnectionToken
        }

        val result = DefaultAWSClient.getTranscript(request)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test describeView maps the response`() = testScope.runTest {
        val requestSlot = slot<DescribeViewRequest>()
        coEvery { mockConnectParticipantClient.describeView(capture(requestSlot)) } returns
            DescribeViewResponse {
                view = View {
                    arn = "view-arn"
                    id = "view-id"
                    name = "view-name"
                    version = 3
                    content = ViewContent {
                        actions = listOf("Submit")
                        inputSchema = "{}"
                        template = "<template/>"
                    }
                }
            }

        val result = DefaultAWSClient.describeView(testConnectionToken, "view-token")

        assertTrue(result.isSuccess)
        val view = result.getOrThrow()
        assertEquals("view-arn", view.arn)
        assertEquals("view-id", view.id)
        assertEquals("view-name", view.name)
        assertEquals(3, view.version)
        assertEquals(listOf("Submit"), view.content?.actions)
        assertEquals("{}", view.content?.inputSchema)
        assertEquals("<template/>", view.content?.template)
        assertEquals(testConnectionToken, requestSlot.captured.connectionToken)
        assertEquals("view-token", requestSlot.captured.viewToken)
    }

    @Test
    fun `test describeView returns an empty view when the response has none`() = testScope.runTest {
        coEvery { mockConnectParticipantClient.describeView(any()) } returns DescribeViewResponse {}

        val result = DefaultAWSClient.describeView(testConnectionToken, "view-token")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow().id)
    }

    @Test
    fun `test calls fail before configure`() = testScope.runTest {
        val clientField = DefaultAWSClient::class.java.getDeclaredField("connectParticipantClient").apply {
            isAccessible = true
        }
        clientField.set(DefaultAWSClient, null)

        val result = DefaultAWSClient.sendMessage(testConnectionToken, ContentType.PLAIN_TEXT, testMessage)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
