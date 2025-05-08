package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.ConnectionCredentials
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionRequest
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionResult
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
import com.amazonaws.services.connectparticipant.model.Websocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AWSClientTest {

    @Mock
    private lateinit var mockConnectParticipantClient: AmazonConnectParticipantClient

    private lateinit var awsClient: AWSClientImpl
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testParticipantToken = "test-participant-token"
    private val testConnectionToken = "test-connection-token"
    private val testWebsocketUrl = "wss://test-websocket-url"
    private val testExpiry = "2023-12-31T23:59:59Z"
    private val testMessage = "test-message"
    private val testContent = "test-content"
    private val testAttachmentId = "test-attachment-id"
    private val testRegion = Regions.US_WEST_2

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        awsClient = AWSClientImpl(mockConnectParticipantClient)
    }

    @Test
    fun `test configure sets region on client`() {
        val config = GlobalConfig(region = testRegion)
        
        awsClient.configure(config)
        
        verify(mockConnectParticipantClient).setRegion(Region.getRegion(testRegion.getName()))
    }

    @Test
    fun `test createParticipantConnection success`() = testScope.runTest {
        val mockWebsocket = Mockito.mock(Websocket::class.java)
        val mockConnectionCredentials = Mockito.mock(ConnectionCredentials::class.java)
        val mockResult = Mockito.mock(CreateParticipantConnectionResult::class.java)
        
        `when`(mockWebsocket.url).thenReturn(testWebsocketUrl)
        `when`(mockWebsocket.connectionExpiry).thenReturn(testExpiry)
        `when`(mockConnectionCredentials.connectionToken).thenReturn(testConnectionToken)
        `when`(mockResult.websocket).thenReturn(mockWebsocket)
        `when`(mockResult.connectionCredentials).thenReturn(mockConnectionCredentials)
        `when`(mockConnectParticipantClient.createParticipantConnection(Mockito.any())).thenReturn(mockResult)
        
        val result = awsClient.createParticipantConnection(testParticipantToken)
        
        assertTrue(result.isSuccess)
        val connectionDetails = result.getOrNull()
        assertEquals(testWebsocketUrl, connectionDetails?.websocketUrl)
        assertEquals(testConnectionToken, connectionDetails?.connectionToken)
        assertEquals(testExpiry, connectionDetails?.expiry)
        
        // Verify request
        val requestCaptor = ArgumentCaptor.forClass(CreateParticipantConnectionRequest::class.java)
        verify(mockConnectParticipantClient).createParticipantConnection(requestCaptor.capture())
        val capturedRequest = requestCaptor.value
        assertEquals(testParticipantToken, capturedRequest.participantToken)
        assertEquals(Constants.ACPS_REQUEST_TYPES, capturedRequest.type)
    }

    @Test
    fun `test createParticipantConnection failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.createParticipantConnection(Mockito.any())).thenThrow(exception)
        
        val result = awsClient.createParticipantConnection(testParticipantToken)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test disconnectParticipantConnection success`() = testScope.runTest {
        val mockResult = Mockito.mock(DisconnectParticipantResult::class.java)
        `when`(mockConnectParticipantClient.disconnectParticipant(Mockito.any())).thenReturn(mockResult)
        
        val result = awsClient.disconnectParticipantConnection(testConnectionToken)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        val requestCaptor = ArgumentCaptor.forClass(DisconnectParticipantRequest::class.java)
        verify(mockConnectParticipantClient).disconnectParticipant(requestCaptor.capture())
        val capturedRequest = requestCaptor.value
        assertEquals(testConnectionToken, capturedRequest.connectionToken)
    }

    @Test
    fun `test disconnectParticipantConnection failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.disconnectParticipant(Mockito.any())).thenThrow(exception)
        
        val result = awsClient.disconnectParticipantConnection(testConnectionToken)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test sendMessage success`() = testScope.runTest {
        val mockResult = Mockito.mock(SendMessageResult::class.java)
        `when`(mockConnectParticipantClient.sendMessage(Mockito.any())).thenReturn(mockResult)
        val contentType = ContentType.PLAIN_TEXT
        
        val result = awsClient.sendMessage(testConnectionToken, contentType, testMessage)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        // Verify request
        val requestCaptor = ArgumentCaptor.forClass(SendMessageRequest::class.java)
        verify(mockConnectParticipantClient).sendMessage(requestCaptor.capture())
        val capturedRequest = requestCaptor.value
        assertEquals(testConnectionToken, capturedRequest.connectionToken)
        assertEquals(contentType.type, capturedRequest.contentType)
        assertEquals(testMessage, capturedRequest.content)
    }

    @Test
    fun `test sendMessage failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.sendMessage(Mockito.any())).thenThrow(exception)
        val contentType = ContentType.PLAIN_TEXT
        
        val result = awsClient.sendMessage(testConnectionToken, contentType, testMessage)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test sendEvent success`() = testScope.runTest {
        val mockResult = Mockito.mock(SendEventResult::class.java)
        `when`(mockConnectParticipantClient.sendEvent(Mockito.any())).thenReturn(mockResult)
        val contentType = ContentType.TYPING
        
        val result = awsClient.sendEvent(testConnectionToken, contentType, testContent)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        // Verify request
        val requestCaptor = ArgumentCaptor.forClass(SendEventRequest::class.java)
        verify(mockConnectParticipantClient).sendEvent(requestCaptor.capture())
        val capturedRequest = requestCaptor.value
        assertEquals(testConnectionToken, capturedRequest.connectionToken)
        assertEquals(contentType.type, capturedRequest.contentType)
        assertEquals(testContent, capturedRequest.content)
    }

    @Test
    fun `test sendEvent failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.sendEvent(Mockito.any())).thenThrow(exception)
        val contentType = ContentType.TYPING
        
        val result = awsClient.sendEvent(testConnectionToken, contentType, testContent)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test startAttachmentUpload success`() = testScope.runTest {
        val mockResult = Mockito.mock(StartAttachmentUploadResult::class.java)
        `when`(mockConnectParticipantClient.startAttachmentUpload(Mockito.any())).thenReturn(mockResult)
        val request = StartAttachmentUploadRequest().apply {
            connectionToken = testConnectionToken
            attachmentName = "test.txt"
            contentType = "text/plain"
        }
        
        val result = awsClient.startAttachmentUpload(testConnectionToken, request)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        // Verify request
        verify(mockConnectParticipantClient).startAttachmentUpload(request)
    }

    @Test
    fun `test startAttachmentUpload failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.startAttachmentUpload(Mockito.any())).thenThrow(exception)
        val request = StartAttachmentUploadRequest().apply {
            connectionToken = testConnectionToken
            attachmentName = "test.txt"
            contentType = "text/plain"
        }
        
        val result = awsClient.startAttachmentUpload(testConnectionToken, request)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test completeAttachmentUpload success`() = testScope.runTest {
        val mockResult = Mockito.mock(CompleteAttachmentUploadResult::class.java)
        `when`(mockConnectParticipantClient.completeAttachmentUpload(Mockito.any())).thenReturn(mockResult)
        val request = CompleteAttachmentUploadRequest().apply {
            connectionToken = testConnectionToken
            setAttachmentIds(listOf(testAttachmentId))
        }

        val result = awsClient.completeAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())

        // Verify request
        verify(mockConnectParticipantClient).completeAttachmentUpload(request)
    }

    @Test
    fun `test completeAttachmentUpload failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.completeAttachmentUpload(Mockito.any())).thenThrow(exception)
        val request = CompleteAttachmentUploadRequest().apply {
            connectionToken = testConnectionToken
            setAttachmentIds(listOf(testAttachmentId))
        }

        val result = awsClient.completeAttachmentUpload(testConnectionToken, request)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }


    @Test
    fun `test getAttachment success`() = testScope.runTest {
        val mockResult = Mockito.mock(GetAttachmentResult::class.java)
        `when`(mockConnectParticipantClient.getAttachment(Mockito.any())).thenReturn(mockResult)
        
        val result = awsClient.getAttachment(testConnectionToken, testAttachmentId)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        // Verify request
        val requestCaptor = ArgumentCaptor.forClass(GetAttachmentRequest::class.java)
        verify(mockConnectParticipantClient).getAttachment(requestCaptor.capture())
        val capturedRequest = requestCaptor.value
        assertEquals(testConnectionToken, capturedRequest.connectionToken)
        assertEquals(testAttachmentId, capturedRequest.attachmentId)
    }

    @Test
    fun `test getAttachment failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.getAttachment(Mockito.any())).thenThrow(exception)
        
        val result = awsClient.getAttachment(testConnectionToken, testAttachmentId)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `test getTranscript success`() = testScope.runTest {
        val mockResult = Mockito.mock(GetTranscriptResult::class.java)
        `when`(mockConnectParticipantClient.getTranscript(Mockito.any())).thenReturn(mockResult)
        val request = GetTranscriptRequest().apply {
            connectionToken = testConnectionToken
        }
        
        val result = awsClient.getTranscript(request)
        
        assertTrue(result.isSuccess)
        assertEquals(mockResult, result.getOrNull())
        
        // Verify request
        verify(mockConnectParticipantClient).getTranscript(request)
    }

    @Test
    fun `test getTranscript failure`() = testScope.runTest {
        val exception = RuntimeException("Test exception")
        `when`(mockConnectParticipantClient.getTranscript(Mockito.any())).thenThrow(exception)
        val request = GetTranscriptRequest().apply {
            connectionToken = testConnectionToken
        }
        
        val result = awsClient.getTranscript(request)
        
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}