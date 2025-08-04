package com.amazon.connect.chat.sdk

import android.net.Uri
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ChatEventPayload
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.TranscriptResponse
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.model.ScanDirection
import com.amazonaws.services.connectparticipant.model.SortKey
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.net.URL


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChatSessionImplTest {

    @Mock
    private lateinit var chatService: ChatService

    private lateinit var chatSession: ChatSession

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        chatSession = ChatSessionImpl(chatService)
    }

    @Test
    fun test_configure(){
        val config = GlobalConfig(region = Regions.US_WEST_2)
        chatSession.configure(config)
        verify(chatService).configure(config)
    }

    @Test
    fun test_getConnectionDetailsProvider(){
        chatSession.getConnectionDetailsProvider()
        verify(chatService).getConnectionDetailsProvider()
    }

    @Test
    fun test_connect_success() = runTest {
        val chatDetails = ChatDetails(participantToken = "participant-token")
        `when`(chatService.createChatSession(chatDetails)).thenReturn(Result.success(true))

        val result = chatSession.connect(chatDetails)

        assertTrue(result.isSuccess)
        verify(chatService).createChatSession(chatDetails)
    }

    @Test
    fun test_connect_failure() = runTest {
        val chatDetails = ChatDetails(participantToken = "invalid token")
        `when`(chatService.createChatSession(chatDetails)).thenReturn(Result.failure(Exception("Network error")))

        val result = chatSession.connect(chatDetails)

        assertTrue(result.isFailure)
        verify(chatService).createChatSession(chatDetails)
    }

    @Test
    fun test_disconnect_success() = runTest {
        `when`(chatService.disconnectChatSession()).thenReturn(Result.success(true))

        val result = chatSession.disconnect()

        assertTrue(result.isSuccess)
        verify(chatService).disconnectChatSession()
    }

    @Test
    fun test_disconnect_failure() = runTest {
        `when`(chatService.disconnectChatSession()).thenReturn(Result.failure(Exception("Network error")))

        val result = chatSession.disconnect()

        assertTrue(result.isFailure)
        verify(chatService).disconnectChatSession()
    }

    @Test
    fun test_sendMessage_success() = runTest {
        val message = "Hello"
        val contentType = ContentType.PLAIN_TEXT
        `when`(chatService.sendMessage(contentType, message)).thenReturn(Result.success(true))

        val result = chatSession.sendMessage(contentType, message)

        assertTrue(result.isSuccess)
        verify(chatService).sendMessage(contentType, message)
    }

    @Test
    fun test_sendMessage_failure() = runTest {
        val message = "Hello"
        val contentType = ContentType.PLAIN_TEXT
        `when`(chatService.sendMessage(contentType, message)).thenReturn(Result.failure(Exception("Send error")))

        val result = chatSession.sendMessage(contentType, message)

        assertTrue(result.isFailure)
        verify(chatService).sendMessage(contentType, message)
    }

    @Test
    fun test_sendEvent_success() = runTest {
        val event = "Typing Event data"
        val contentType = ContentType.TYPING
        `when`(chatService.sendEvent(contentType, event)).thenReturn(Result.success(true))

        val result = chatSession.sendEvent(contentType, event)

        assertTrue(result.isSuccess)
        verify(chatService).sendEvent(contentType, event)
    }

    @Test
    fun test_sendEvent_failure() = runTest {
        val event = "Typing Event data"
        val contentType = ContentType.TYPING
        `when`(chatService.sendEvent(contentType, event)).thenReturn(Result.failure(Exception("Event error")))

        val result = chatSession.sendEvent(contentType, event)

        assertTrue(result.isFailure)
        verify(chatService).sendEvent(contentType, event)
    }

    @Test
    fun test_sendAttachment_success() = runTest {
        val fileUri = Uri.parse("file://path/to/file")
        `when`(chatService.sendAttachment(fileUri)).thenReturn(Result.success(true))

        val result = chatSession.sendAttachment(fileUri)

        assertTrue(result.isSuccess)
        verify(chatService).sendAttachment(fileUri)
    }

    @Test
    fun test_sendAttachment_failure() = runTest {
        val fileUri = Uri.parse("file://path/to/file")
        `when`(chatService.sendAttachment(fileUri)).thenReturn(Result.failure(Exception("Attachment error")))

        val result = chatSession.sendAttachment(fileUri)

        assertTrue(result.isFailure)
        verify(chatService).sendAttachment(fileUri)
    }

    @Test
    fun test_resendFailedMessage_success() = runTest {
        val messageId = "failed_message_id"
        `when`(chatService.resendFailedMessage(messageId)).thenReturn(Result.success(true))

        val result = chatSession.resendFailedMessage(messageId)
        assertTrue(result.isSuccess)
        verify(chatService).resendFailedMessage(messageId)
    }

    @Test
    fun test_downloadAttachment_success() = runTest {
        val attachmentId = "attachmentId"
        val filename = "filename"
        val expectedUrl = URL("https://example.com/file")
        `when`(chatService.downloadAttachment(attachmentId, filename)).thenReturn(Result.success(expectedUrl))

        val result = chatSession.downloadAttachment(attachmentId, filename)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == expectedUrl)
        verify(chatService).downloadAttachment(attachmentId, filename)
    }

    @Test
    fun test_downloadAttachment_failure() = runTest {
        val attachmentId = "attachmentId"
        val filename = "filename"
        `when`(chatService.downloadAttachment(attachmentId, filename)).thenReturn(Result.failure(Exception("Download error")))

        val result = chatSession.downloadAttachment(attachmentId, filename)

        assertTrue(result.isFailure)
        verify(chatService).downloadAttachment(attachmentId, filename)
    }

    @Test
    fun test_getTranscript_success() = runTest {
        val scanDirection: ScanDirection? = null
        val sortKey: SortKey? = null
        val maxResults: Int? = 10
        val nextToken: String? = null
        val startPosition: String? = null
        val transcriptResponse = TranscriptResponse("","",listOf())
        `when`(chatService.getTranscript(scanDirection, sortKey, maxResults, nextToken, null)).thenReturn(Result.success(transcriptResponse))

        val result = chatSession.getTranscript(scanDirection, sortKey, maxResults, nextToken, startPosition)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == transcriptResponse)
        verify(chatService).getTranscript(scanDirection, sortKey, maxResults, nextToken, null)
    }

    @Test
    fun test_getTranscript_failure() = runTest {
        val scanDirection: ScanDirection? = null
        val sortKey: SortKey? = null
        val maxResults: Int? = 10
        val nextToken: String? = null
        val startPosition: String? = null
        `when`(chatService.getTranscript(scanDirection, sortKey, maxResults, nextToken, null)).thenReturn(Result.failure(Exception("Transcript error")))

        val result = chatSession.getTranscript(scanDirection, sortKey, maxResults, nextToken, startPosition)

        assertTrue(result.isFailure)
        verify(chatService).getTranscript(scanDirection, sortKey, maxResults, nextToken, null)
    }

    @Test
    fun test_sendMessageReceipt_callsSendReceipt() = runTest {
            val message = Message(
            id = "messageId",
            timeStamp = "timestamp",
            text = "dummy text",
            participant = "AGENT",
            contentType = ContentType.PLAIN_TEXT.type,
            messageDirection = MessageDirection.INCOMING,
            metadata = MessageMetadata(id = "messageId", timeStamp = "timestamp", contentType="PLAIN_TEXT", status  = MessageStatus.Delivered,)
        )
        val receiptType = MessageReceiptType.MESSAGE_READ

        `when`(chatService.sendMessageReceipt(receiptType, message.id)).thenReturn(Result.success(Unit))

        chatSession.sendMessageReceipt(message, receiptType)

        verify(chatService).sendMessageReceipt(receiptType, message.id)
    }

    // Generic test for all event callbacks that take Event? parameter
    @Test
    fun test_eventCallbacks_withEventObject() = runTest {
        val testEvent = Event(
            id = "testEventId",
            timeStamp = "2023-01-01T10:00:00.000Z",
            participant = "AGENT",
            text = "Test event",
            displayName = "Test Agent",
            contentType = "test/event"
        )

        // Test each callback by setting it and directly invoking it
        val callbackTests = listOf(
            "onTyping" to { callback: (Event?) -> Unit -> chatSession.onTyping = callback },
            "onMessageDelivered" to { callback: (Event?) -> Unit -> chatSession.onMessageDelivered = callback },
            "onMessageRead" to { callback: (Event?) -> Unit -> chatSession.onMessageRead = callback },
            "onParticipantActive" to { callback: (Event?) -> Unit -> chatSession.onParticipantActive = callback },
            "onParticipantInactive" to { callback: (Event?) -> Unit -> chatSession.onParticipantInactive = callback },
            "onParticipantIdle" to { callback: (Event?) -> Unit -> chatSession.onParticipantIdle = callback },
            "onParticipantReturned" to { callback: (Event?) -> Unit -> chatSession.onParticipantReturned = callback },
            "onParticipantInvited" to { callback: (Event?) -> Unit -> chatSession.onParticipantInvited = callback },
            "onAutoDisconnection" to { callback: (Event?) -> Unit -> chatSession.onAutoDisconnection = callback },
            "onChatRehydrated" to { callback: (Event?) -> Unit -> chatSession.onChatRehydrated = callback },
            "onParticipantJoined" to { callback: (Event?) -> Unit -> chatSession.onParticipantJoined = callback },
            "onParticipantLeft" to { callback: (Event?) -> Unit -> chatSession.onParticipantLeft = callback },
            "onChatEnded" to { callback: (Event?) -> Unit -> chatSession.onChatEnded = callback }
        )

        callbackTests.forEach { (callbackName, setter) ->
            var callbackInvoked = false
            var receivedEvent: Event? = null

            // Set up the callback
            setter { event ->
                callbackInvoked = true
                receivedEvent = event
            }

            // Directly invoke the callback to test it was set correctly
            when (callbackName) {
                "onTyping" -> chatSession.onTyping?.invoke(testEvent)
                "onMessageDelivered" -> chatSession.onMessageDelivered?.invoke(testEvent)
                "onMessageRead" -> chatSession.onMessageRead?.invoke(testEvent)
                "onParticipantActive" -> chatSession.onParticipantActive?.invoke(testEvent)
                "onParticipantInactive" -> chatSession.onParticipantInactive?.invoke(testEvent)
                "onParticipantIdle" -> chatSession.onParticipantIdle?.invoke(testEvent)
                "onParticipantReturned" -> chatSession.onParticipantReturned?.invoke(testEvent)
                "onParticipantInvited" -> chatSession.onParticipantInvited?.invoke(testEvent)
                "onAutoDisconnection" -> chatSession.onAutoDisconnection?.invoke(testEvent)
                "onChatRehydrated" -> chatSession.onChatRehydrated?.invoke(testEvent)
                "onParticipantJoined" -> chatSession.onParticipantJoined?.invoke(testEvent)
                "onParticipantLeft" -> chatSession.onParticipantLeft?.invoke(testEvent)
                "onChatEnded" -> chatSession.onChatEnded?.invoke(testEvent)
            }

            // Verify callback was invoked with correct event
            assertTrue("Callback $callbackName should be invoked", callbackInvoked)
            assertEquals("Event object should match for $callbackName", testEvent, receivedEvent)

            // Clear callback for next test
            setter { }
        }
    }

    // Test for callbacks that don't take Event parameter
    @Test
    fun test_eventCallbacks_withoutEventObject() = runTest {
        // Test each callback by setting it and directly invoking it
        val callbackTests = listOf(
            "onConnectionEstablished" to { callback: () -> Unit -> chatSession.onConnectionEstablished = callback },
            "onConnectionReEstablished" to { callback: () -> Unit -> chatSession.onConnectionReEstablished = callback },
            "onConnectionBroken" to { callback: () -> Unit -> chatSession.onConnectionBroken = callback },
            "onDeepHeartBeatFailure" to { callback: () -> Unit -> chatSession.onDeepHeartBeatFailure = callback }
        )

        callbackTests.forEach { (callbackName, setter) ->
            var callbackInvoked = false

            // Set up the callback
            setter {
                callbackInvoked = true
            }

            // Directly invoke the callback to test it was set correctly
            when (callbackName) {
                "onConnectionEstablished" -> chatSession.onConnectionEstablished?.invoke()
                "onConnectionReEstablished" -> chatSession.onConnectionReEstablished?.invoke()
                "onConnectionBroken" -> chatSession.onConnectionBroken?.invoke()
                "onDeepHeartBeatFailure" -> chatSession.onDeepHeartBeatFailure?.invoke()
            }

            // Verify callback was invoked
            assertTrue("Callback $callbackName should be invoked", callbackInvoked)

            // Clear callback for next test
            setter { }
        }
    }

    // Specific test for message receipt events triggered via metadata
    @Test
    fun test_messageReceiptCallbacks_fromMetadata() = runTest {
        var deliveredCallbackInvoked = false
        var readCallbackInvoked = false
        var deliveredEvent: Event? = null
        var readEvent: Event? = null

        // Set up callbacks
        chatSession.onMessageDelivered = { event ->
            deliveredCallbackInvoked = true
            deliveredEvent = event
        }
        
        chatSession.onMessageRead = { event ->
            readCallbackInvoked = true
            readEvent = event
        }

        // Test MessageDelivered event
        val deliveredTestEvent = Event(
            id = "messageId123",
            timeStamp = "2023-01-01T10:00:00.000Z",
            participant = "CUSTOMER",
            text = null,
            displayName = null,
            contentType = "application/vnd.amazonaws.connect.event.message.delivered"
        )
        
        chatSession.onMessageDelivered?.invoke(deliveredTestEvent)

        assertTrue("MessageDelivered callback should be invoked", deliveredCallbackInvoked)
        assertEquals("Delivered event should match", deliveredTestEvent, deliveredEvent)

        // Test MessageRead event
        val readTestEvent = Event(
            id = "messageId456",
            timeStamp = "2023-01-01T10:01:00.000Z",
            participant = "CUSTOMER",
            text = null,
            displayName = null,
            contentType = "application/vnd.amazonaws.connect.event.message.read"
        )
        
        chatSession.onMessageRead?.invoke(readTestEvent)

        assertTrue("MessageRead callback should be invoked", readCallbackInvoked)
        assertEquals("Read event should match", readTestEvent, readEvent)
    }

    // Test that callbacks can handle past session message events
    @Test
    fun test_messageReceiptCallbacks_ignorePastSession() = runTest {
        var deliveredCallbackInvoked = false

        // Set up callback
        chatSession.onMessageDelivered = { deliveredCallbackInvoked = true }

        // Create events that would be from past session (this is typically handled in WebSocketManager,
        // but we're testing that the ChatSession callbacks handle these events correctly when they do arrive)
        val pastSessionEvent = Event(
            id = "pastMessageId",
            timeStamp = "2023-01-01T09:00:00.000Z",
            participant = "CUSTOMER",
            text = null,
            displayName = null,
            contentType = "application/vnd.amazonaws.connect.event.message.delivered"
        )

        // Since the filtering happens in WebSocketManager, these events wouldn't be emitted
        // for past session messages, but we test that our callbacks handle them normally when they do arrive
        chatSession.onMessageDelivered?.invoke(pastSessionEvent)

        // These should still trigger callbacks as the filtering happens at WebSocket level
        assertTrue("Even past session events should trigger callbacks when emitted", deliveredCallbackInvoked)
    }

}
