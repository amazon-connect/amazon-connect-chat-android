package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
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
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AWSClientImplTest {

    @Mock
    private lateinit var mockClient: AmazonConnectParticipantClient

    private lateinit var awsClient: AWSClientImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        awsClient = AWSClientImpl(mockClient)
    }

    @Test
    fun test_configure() {
        val config = GlobalConfig(region = Regions.US_WEST_2)
        awsClient.configure(config)
        verify(mockClient).setRegion(Region.getRegion(config.region))
    }

    @Test
    fun test_createParticipantConnection_success() = runTest {
        val participantToken = "token"

        val mockResponse = CreateParticipantConnectionResult().apply {
            connectionCredentials = ConnectionCredentials().apply {
                connectionToken = "mockedConnectionToken"
            }
            websocket = Websocket().apply {
                url = "mockedWebsocketUrl"
                connectionExpiry = "mockedExpiryTime"
            }
        }

        `when`(mockClient.createParticipantConnection(any(CreateParticipantConnectionRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.createParticipantConnection(participantToken)

        assertTrue("Expected successful connection creation", result.isSuccess)
        verify(mockClient).createParticipantConnection(any(CreateParticipantConnectionRequest::class.java))
    }

    @Test
    fun test_createParticipantConnection_failure() = runTest {
        val participantToken = "invalid_token"
        `when`(mockClient.createParticipantConnection(any(CreateParticipantConnectionRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.createParticipantConnection(participantToken)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_disconnectParticipantConnection_success() = runTest {
        val connectionToken = "token"
        val mockResponse = mock(DisconnectParticipantResult::class.java)
        `when`(mockClient.disconnectParticipant(any(DisconnectParticipantRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.disconnectParticipantConnection(connectionToken)

        assertTrue("Expected successful disconnection", result.isSuccess)
        verify(mockClient).disconnectParticipant(any(DisconnectParticipantRequest::class.java))
    }

    @Test
    fun test_disconnectParticipantConnection_failure() = runTest {
        val connectionToken = "invalid_token"
        `when`(mockClient.disconnectParticipant(any(DisconnectParticipantRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.disconnectParticipantConnection(connectionToken)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_sendMessage_success() = runTest {
        val connectionToken = "token"
        val contentType = ContentType.PLAIN_TEXT
        val message = "Hello, world!"

        val mockResponse = mock(SendMessageResult::class.java)
        `when`(mockClient.sendMessage(any(SendMessageRequest::class.java))).thenReturn(mockResponse)

        val result = awsClient.sendMessage(connectionToken, contentType, message)

        assertTrue("Expected successful message sending", result.isSuccess)
        verify(mockClient).sendMessage(any(SendMessageRequest::class.java))
    }

    @Test
    fun test_sendMessage_failure() = runTest {
        val connectionToken = "invalid_token"
        val contentType = ContentType.PLAIN_TEXT
        val message = "Hello, world!"

        `when`(mockClient.sendMessage(any(SendMessageRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.sendMessage(connectionToken, contentType, message)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_sendEvent_success() = runTest {
        val connectionToken = "token"
        val contentType = ContentType.PLAIN_TEXT
        val event = "typing"

        `when`(mockClient.sendEvent(any(SendEventRequest::class.java)))
            .thenReturn(mock(SendEventResult::class.java))

        val result = awsClient.sendEvent(connectionToken, contentType, event)

        assertTrue("Expected successful event sending", result.isSuccess)
        verify(mockClient).sendEvent(any(SendEventRequest::class.java))
    }

    @Test
    fun test_sendEvent_failure() = runTest {
        val connectionToken = "invalid_token"
        val contentType = ContentType.PLAIN_TEXT
        val event = "typing"

        `when`(mockClient.sendEvent(any(SendEventRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.sendEvent(connectionToken, contentType, event)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_startAttachmentUpload_success() = runTest {
        val connectionToken = "token"
        val request = mock(StartAttachmentUploadRequest::class.java)
        val mockResponse = mock(StartAttachmentUploadResult::class.java)

        `when`(mockClient.startAttachmentUpload(any(StartAttachmentUploadRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.startAttachmentUpload(connectionToken, request)

        assertTrue("Expected successful attachment upload start", result.isSuccess)
        verify(mockClient).startAttachmentUpload(any(StartAttachmentUploadRequest::class.java))
    }

    @Test
    fun test_startAttachmentUpload_failure() = runTest {
        val connectionToken = "invalid_token"
        val request = mock(StartAttachmentUploadRequest::class.java)

        `when`(mockClient.startAttachmentUpload(any(StartAttachmentUploadRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.startAttachmentUpload(connectionToken, request)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_completeAttachmentUpload_success() = runTest {
        val connectionToken = "token"
        val request = mock(CompleteAttachmentUploadRequest::class.java)
        val mockResponse = mock(CompleteAttachmentUploadResult::class.java)

        `when`(mockClient.completeAttachmentUpload(any(CompleteAttachmentUploadRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.completeAttachmentUpload(connectionToken, request)

        assertTrue("Expected successful attachment upload completion", result.isSuccess)
        verify(mockClient).completeAttachmentUpload(any(CompleteAttachmentUploadRequest::class.java))
    }

    @Test
    fun test_completeAttachmentUpload_failure() = runTest {
        val connectionToken = "invalid_token"
        val request = mock(CompleteAttachmentUploadRequest::class.java)

        `when`(mockClient.completeAttachmentUpload(any(CompleteAttachmentUploadRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.completeAttachmentUpload(connectionToken, request)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_getAttachment_success() = runTest {
        val connectionToken = "token"
        val attachmentId = "attachmentId"
        val mockResponse = mock(GetAttachmentResult::class.java)

        `when`(mockClient.getAttachment(any(GetAttachmentRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.getAttachment(connectionToken, attachmentId)

        assertTrue("Expected successful attachment retrieval", result.isSuccess)
        verify(mockClient).getAttachment(any(GetAttachmentRequest::class.java))
    }

    @Test
    fun test_getAttachment_failure() = runTest {
        val connectionToken = "invalid_token"
        val attachmentId = "attachmentId"

        `when`(mockClient.getAttachment(any(GetAttachmentRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.getAttachment(connectionToken, attachmentId)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

    @Test
    fun test_getTranscript_success() = runTest {
        val request = mock(GetTranscriptRequest::class.java)
        val mockResponse = mock(GetTranscriptResult::class.java)

        `when`(mockClient.getTranscript(any(GetTranscriptRequest::class.java)))
            .thenReturn(mockResponse)

        val result = awsClient.getTranscript(request)

        assertTrue("Expected successful transcript retrieval", result.isSuccess)
        verify(mockClient).getTranscript(any(GetTranscriptRequest::class.java))
    }

    @Test
    fun test_getTranscript_failure() = runTest {
        val request = mock(GetTranscriptRequest::class.java)

        `when`(mockClient.getTranscript(any(GetTranscriptRequest::class.java)))
            .thenThrow(RuntimeException("Network error"))

        try {
            awsClient.getTranscript(request)
        } catch (e: Exception) {
            assertTrue("Expected exception due to network error", e is RuntimeException)
            assertTrue("Expected network error message", e.message == "Network error")
        }
    }

}
