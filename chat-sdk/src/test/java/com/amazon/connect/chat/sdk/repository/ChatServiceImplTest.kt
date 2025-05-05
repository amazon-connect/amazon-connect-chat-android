package com.amazon.connect.chat.sdk.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.TranscriptResponse
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import com.amazonaws.services.connectparticipant.model.GetTranscriptResult
import com.amazonaws.services.connectparticipant.model.Item
import com.amazonaws.services.connectparticipant.model.ScanDirection
import com.amazonaws.services.connectparticipant.model.SendEventResult
import com.amazonaws.services.connectparticipant.model.SendMessageResult
import com.amazonaws.services.connectparticipant.model.SortKey
import com.amazonaws.services.connectparticipant.model.StartPosition
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.times;
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.whenever
import java.util.UUID
import java.net.URL

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChatServiceImplTest {

    @Mock
    private lateinit var awsClient: AWSClient

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var connectionDetailsProvider: ConnectionDetailsProvider

    @Mock
    private lateinit var webSocketManager: WebSocketManager

    @Mock
    private lateinit var metricsManager: MetricsManager

    @Mock
    private lateinit var attachmentsManager: AttachmentsManager

    @Mock
    private lateinit var messageReceiptsManager: MessageReceiptsManager

    private lateinit var chatService: ChatServiceImpl
    private lateinit var eventSharedFlow: MutableSharedFlow<ChatEvent>
    private lateinit var transcriptSharedFlow: MutableSharedFlow<TranscriptItem>
    private lateinit var chatSessionStateFlow: MutableStateFlow<Boolean>
    private lateinit var newWsUrlFlow: MutableSharedFlow<Unit>

    private val mockUri: Uri = Uri.parse("https://example.com/dummy.pdf")
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        eventSharedFlow = MutableSharedFlow()
        transcriptSharedFlow = MutableSharedFlow()
        chatSessionStateFlow = MutableStateFlow(false)
        newWsUrlFlow = MutableSharedFlow()

        whenever(webSocketManager.eventPublisher).thenReturn(eventSharedFlow)
        whenever(webSocketManager.transcriptPublisher).thenReturn(transcriptSharedFlow)
        whenever(webSocketManager.requestNewWsUrlFlow).thenReturn(newWsUrlFlow)
        whenever(connectionDetailsProvider.chatSessionState).thenReturn(chatSessionStateFlow)

        chatService = ChatServiceImpl(
            context,
            awsClient,
            connectionDetailsProvider,
            webSocketManager,
            metricsManager,
            attachmentsManager,
            messageReceiptsManager
        )
    }

    @Test
    fun test_configure(){
        val config = GlobalConfig(region = Regions.US_WEST_2)
        chatService.configure(config)
        verify(awsClient).configure(config)
    }

    @Test
    fun test_getConnectionDetailsProvider(){
        val result = chatService.getConnectionDetailsProvider()
        assertEquals(result, connectionDetailsProvider)
    }

    @Test
    fun test_createParticipantConnection_success() = runTest {
        val chatDetails = ChatDetails(participantToken = "token")
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(awsClient.createParticipantConnection(chatDetails.participantToken)).thenReturn(Result.success(mockConnectionDetails))
        `when`(connectionDetailsProvider.updateChatDetails(chatDetails)).then { /**/ }

        val result = chatService.createChatSession(chatDetails)

        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider).updateChatDetails(chatDetails)
        verify(connectionDetailsProvider).updateConnectionDetails(mockConnectionDetails)
        verify(awsClient).createParticipantConnection(chatDetails.participantToken)
    }

    @Test
    fun test_createParticipantConnection_failure() = runTest {
        val chatDetails = ChatDetails(participantToken = "invalid_token")
        `when`(awsClient.createParticipantConnection(chatDetails.participantToken)).thenReturn(
            Result.failure(Exception("Network error"))
        )
        val result = chatService.createChatSession(chatDetails)
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).updateChatDetails(chatDetails)
        verify(awsClient).createParticipantConnection(chatDetails.participantToken)
    }

    @Test
    fun test_disconnectParticipantConnection_success() = runTest {
        // Simulate that the chat session is active.
        `when`(connectionDetailsProvider.isChatSessionActive()).thenReturn(true)
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(awsClient.disconnectParticipantConnection(mockConnectionDetails.connectionToken))
            .thenReturn(Result.success(DisconnectParticipantResult()))

        // Invoke the disconnection logic.
        val result = chatService.disconnectChatSession()

        // Verify that the overall result is successful.
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())

        // Verify that the proper calls were made.
        verify(connectionDetailsProvider).isChatSessionActive()
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient).disconnectParticipantConnection(mockConnectionDetails.connectionToken)
        verify(webSocketManager).disconnect("Customer ended the chat")
        verify(connectionDetailsProvider).setChatSessionState(false)
    }

    @Test
    fun test_disconnectParticipantConnection_failure() = runTest {
        // Simulate that the chat session is active.
        `when`(connectionDetailsProvider.isChatSessionActive()).thenReturn(true)
        val mockConnectionDetails = createMockConnectionDetails("invalid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        // Simulate an error during AWS disconnect.
        `when`(awsClient.disconnectParticipantConnection(mockConnectionDetails.connectionToken))
            .thenThrow(RuntimeException("Network error"))

        // Invoke the disconnection logic.
        val result = chatService.disconnectChatSession()

        // Verify that the overall result is a failure.
        assertTrue(result.isFailure)

        // Verify that the calls were attempted.
        verify(connectionDetailsProvider).isChatSessionActive()
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient).disconnectParticipantConnection(mockConnectionDetails.connectionToken)

        // Because AWS disconnect failed, the subsequent steps should not be executed.
        verify(webSocketManager, never()).disconnect("Customer ended the chat")
        verify(connectionDetailsProvider, never()).setChatSessionState(false)
    }

    @Test
    fun test_disconnectParticipantConnection_noConnectionDetails() = runTest {
        // Simulate that the chat session is active.
        `when`(connectionDetailsProvider.isChatSessionActive()).thenReturn(true)
        // No connection details available.
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)

        // Invoke the disconnection logic.
        val result = chatService.disconnectChatSession()

        // Verify that the overall result is a failure.
        assertTrue(result.isFailure)

        // Verify that after checking the session state, it tried to get connection details.
        verify(connectionDetailsProvider).isChatSessionActive()
        verify(connectionDetailsProvider).getConnectionDetails()
        // Because there are no connection details, AWS disconnect should never be called.
        verify(awsClient, never()).disconnectParticipantConnection(anyString())
        verify(webSocketManager, never()).disconnect("Customer ended the chat")
    }

    @Test
    fun test_sendMessages_success() = runTest {
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        val mockMessage = "Hello"
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        val contentType = ContentType.PLAIN_TEXT

        `when`(awsClient.sendMessage(mockConnectionDetails.connectionToken, contentType, mockMessage))
            .thenReturn(Result.success(SendMessageResult()))

        val result = chatService.sendMessage(contentType, mockMessage)

        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient).sendMessage(mockConnectionDetails.connectionToken, contentType, mockMessage)
    }

    @Test
    fun test_sendMessages_failure() = runTest {
        val mockConnectionDetails = createMockConnectionDetails("invalid_token")
        val mockMessage = "Hello"
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)

        `when`(awsClient.sendMessage(mockConnectionDetails.connectionToken, ContentType.PLAIN_TEXT, mockMessage))
            .thenThrow(RuntimeException("Network error"))

        val result = chatService.sendMessage(ContentType.PLAIN_TEXT, mockMessage)

        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
    }

    @Test
    fun test_sendMessages_noConnectionDetails() = runTest {
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)
        val result = chatService.sendMessage(ContentType.PLAIN_TEXT, "Hello")
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
    }

    @Test
    fun test_resendFailedMessage_success() = runTest {
        val mockMessage = "Hello"
        val contentType = ContentType.PLAIN_TEXT
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)

        `when`(awsClient.sendMessage(mockConnectionDetails.connectionToken, contentType, mockMessage))
            .thenThrow(RuntimeException("Network error")).thenReturn(Result.success(SendMessageResult()))

        val messageId = "123e4567-e89b-12d3-a456-426614174000"
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns UUID.fromString(messageId)

        chatService.sendMessage(ContentType.PLAIN_TEXT, mockMessage)

        val result = chatService.resendFailedMessage(messageId)
        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider, times(2)).getConnectionDetails()
        verify(awsClient, times(2)).sendMessage(mockConnectionDetails.connectionToken, contentType, mockMessage)
        unmockkStatic(UUID::class)
    }

    @Test
    fun test_sendEvent_success() = runTest {
        val contentType = ContentType.TYPING
        val eventContent = "Typing event"
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(awsClient.sendEvent(mockConnectionDetails.connectionToken, contentType, eventContent))
            .thenReturn(Result.success(
            SendEventResult()
        ))

        val result = chatService.sendEvent(contentType, eventContent)

        assertTrue(result.isSuccess)
        verify(awsClient).sendEvent(mockConnectionDetails.connectionToken, contentType, eventContent)
    }

    @Test
    fun test_sendEvent_failure() = runTest {
        val contentType = ContentType.TYPING
        val eventContent = "Typing event"
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(awsClient.sendEvent(mockConnectionDetails.connectionToken, contentType, eventContent)).thenThrow(RuntimeException("Event error"))

        val result = chatService.sendEvent(contentType, eventContent)

        assertTrue(result.isFailure)
        verify(awsClient).sendEvent(mockConnectionDetails.connectionToken, contentType, eventContent)
    }

    @Test
    fun test_sendEvent_noConnectionDetails() = runTest {
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)
        val result = chatService.sendEvent(ContentType.TYPING, "Typing event")
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
    }

    @Test
    fun test_sendAttachment_noUri() = runTest {
        val result = chatService.sendAttachment(Uri.EMPTY)
        assertTrue(result.isFailure)
    }

    @Test
    fun test_sendAttachment_success() = runTest {
        // Mock Context and ContentResolver
        val mockContentResolver = mock(ContentResolver::class.java)

        // Mock URI and file name retrieval
        `when`(context.contentResolver).thenReturn(mockContentResolver)

        // Mock connection details and attachment manager
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        val mockAttachmentId = "attachment123"
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.sendAttachment(mockConnectionDetails.connectionToken, mockUri)).thenReturn(Result.success(mockAttachmentId))

        // Call the sendAttachment method
        val result = chatService.sendAttachment(mockUri)

        // Verify the result
        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(attachmentsManager).sendAttachment(mockConnectionDetails.connectionToken, mockUri)
    }


    @Test
    fun test_sendAttachment_failure() = runTest {
        // Mock Context and ContentResolver
        val mockContentResolver = mock(ContentResolver::class.java)

        // Mock URI and file name retrieval
        `when`(context.contentResolver).thenReturn(mockContentResolver)

        val mockConnectionDetails = createMockConnectionDetails("invalid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)

        `when`(attachmentsManager.sendAttachment(mockConnectionDetails.connectionToken, mockUri)).thenThrow(RuntimeException("Network error"))

        val result = chatService.sendAttachment(mockUri)

        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(attachmentsManager).sendAttachment(mockConnectionDetails.connectionToken, mockUri)
    }

    @Test
    fun test_sendAttachment_noConnectionDetails() = runTest {
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)
        val result = chatService.sendAttachment(mockUri)
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(attachmentsManager, never()).sendAttachment(anyString(), anyOrNull())
    }

    @Test
    fun test_resendFailedAttachment_success() = runTest {
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        // Mock Context and ContentResolver
        val mockContentResolver = mock(ContentResolver::class.java)
        val mockAttachmentId = "attachment123"
        val messageId = "123e4567-e89b-12d3-a456-426614174000"
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns UUID.fromString(messageId)

        // Mock URI and file name retrieval
        `when`(context.contentResolver).thenReturn(mockContentResolver)
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.sendAttachment(mockConnectionDetails.connectionToken, mockUri)).thenThrow(RuntimeException("Network error")).thenReturn(Result.success(mockAttachmentId))

        chatService.sendAttachment(mockUri)

        val result = chatService.resendFailedMessage(messageId)
        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider, times(2)).getConnectionDetails()
        verify(attachmentsManager, times(2)).sendAttachment(mockConnectionDetails.connectionToken, mockUri)
        unmockkStatic(UUID::class)
    }

    @Test
    fun test_downloadAttachment_success() = runTest {
        val attachmentId = "attachment123"
        val fileName = "file.pdf"
        val mockUrl = URL("https://example.com/file")
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.downloadAttachment(mockConnectionDetails.connectionToken, attachmentId, fileName)).thenReturn(Result.success(mockUrl))

        val result = chatService.downloadAttachment(attachmentId, fileName)

        assertTrue(result.isSuccess)
        assertEquals(mockUrl, result.getOrNull())
        verify(attachmentsManager).downloadAttachment(mockConnectionDetails.connectionToken, attachmentId, fileName)
    }

    @Test
    fun test_downloadAttachment_failure() = runTest {
        val attachmentId = "attachment123"
        val fileName = "file.pdf"
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.downloadAttachment(mockConnectionDetails.connectionToken, attachmentId, fileName)).thenThrow(RuntimeException("Download error"))

        val result = chatService.downloadAttachment(attachmentId, fileName)

        assertTrue(result.isFailure)
        verify(attachmentsManager).downloadAttachment(mockConnectionDetails.connectionToken, attachmentId, fileName)
    }

    @Test
    fun test_downloadAttachment_noConnectionDetails() = runTest {
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)
        val result = chatService.downloadAttachment("attachment123", "file.pdf")
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
    }

    @Test
    fun test_downloadAttachment_noFileName() = runTest {
        val result = chatService.downloadAttachment("attachment123", "")
        assertTrue(result.isFailure)
    }

    @Test
    fun test_downloadAttachment_noAttachmentId() = runTest {
        val result = chatService.downloadAttachment("", "file.pdf")
        assertTrue(result.isFailure)
    }

    @Test
    fun test_getTranscript_success() = runTest {
        val scanDirection = ScanDirection.BACKWARD
        val sortKey = SortKey.ASCENDING
        val maxResults = 10
        val nextToken = "nextToken123"
        val startPosition = StartPosition().apply { id = "startId" }
        val mockTranscriptResponse = TranscriptResponse("", nextToken, listOf())

        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)

        // Create a mock GetTranscriptResult and configure it to return expected values
        val mockGetTranscriptResult = mock<GetTranscriptResult>()
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf())
        `when`(mockGetTranscriptResult.initialContactId).thenReturn("")
        `when`(mockGetTranscriptResult.nextToken).thenReturn(nextToken)

        `when`(awsClient.getTranscript(anyOrNull())).thenReturn(Result.success(mockGetTranscriptResult))

        val result = chatService.getTranscript(scanDirection, sortKey, maxResults, nextToken, startPosition)

        assertTrue(result.isSuccess)
        assertEquals(mockTranscriptResponse, result.getOrNull())
        verify(awsClient).getTranscript(anyOrNull())
    }

    @Test
    fun test_getTranscript_failure() = runTest {
        val scanDirection = ScanDirection.BACKWARD
        val sortKey = SortKey.ASCENDING
        val maxResults = 10
        val nextToken = "nextToken123"
        val startPosition = StartPosition().apply { id = "startId" }

        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)

        `when`(awsClient.getTranscript(anyOrNull())).thenThrow(RuntimeException("Transcript error"))

        val result = chatService.getTranscript(scanDirection, sortKey, maxResults, nextToken, startPosition)

        assertTrue(result.isFailure)
        assertEquals("Transcript error", result.exceptionOrNull()?.message)

        verify(awsClient).getTranscript(anyOrNull())
    }

    private fun createMockItem(id: String, timestamp: String): Item {
        val item = Item()
        item.absoluteTime = timestamp
        item.content = "test${id}"
        item.contentType = "text/plain"
        item.id = id
        item.type = "MESSAGE"
        item.participantId = id
        item.displayName = "test${id}"
        item.participantRole = "CUSTOMER"
        return item
    }

    @Test
    fun test_getTranscript_previousTranscriptNextToken() = runTest {
        val chatServiceInstance = chatService as ChatServiceImpl

        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        val chatDetails = ChatDetails(participantToken = "token")
        chatServiceInstance.createChatSession(chatDetails)
        advanceUntilIdle()

        // Create a mock GetTranscriptResult and configure it to return expected values
        val mockGetTranscriptResult = mock<GetTranscriptResult>()
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf())
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken1")
        `when`(mockGetTranscriptResult.initialContactId).thenReturn("")
        `when`(awsClient.getTranscript(anyOrNull())).thenReturn(Result.success(mockGetTranscriptResult))

        chatServiceInstance.getTranscript(ScanDirection.BACKWARD, SortKey.ASCENDING, 10, null, null)
        advanceUntilIdle()

        // Expect previousTranscriptNextToken to be set when internal transcript is empty.
        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken1")

        // Add items to the internal transcript
        val transcriptItem1 = Message(id = "1", timeStamp = "2024-01-01T00:00:00Z", participant = "user", contentType = "text/plain", text = "Hello")
        val transcriptItem2 = Message(id = "2", timeStamp = "2025-01-01T00:01:00Z", participant = "agent", contentType = "text/plain", text = "Hi")
        transcriptSharedFlow.emit(transcriptItem1)
        transcriptSharedFlow.emit(transcriptItem2)
        advanceUntilIdle()

        // Expect previousTranscriptNextToken to persist when items are added
        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken1")

        // Expect previousTranscriptNextToken to be set when getTranscript returns an empty list.
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf())
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken2")
        `when`(awsClient.getTranscript(anyOrNull())).thenReturn(Result.success(mockGetTranscriptResult))

        chatServiceInstance.getTranscript(ScanDirection.BACKWARD, SortKey.ASCENDING, 10, null, null)
        advanceUntilIdle()

        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken2")

        // Expect previousTranscriptNextToken to be set when getTranscript returns an older message.
        val item1 = createMockItem("1", "2023-01-01T00:00:00Z")
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf(item1))
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken3")

        chatServiceInstance.getTranscript(ScanDirection.BACKWARD, SortKey.ASCENDING, 10, null, null)
        advanceUntilIdle()

        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken3")

        // Expect previousTranscriptNextToken to be the same when getTranscript returns a newer message.
        val item2 = createMockItem("2", "2025-01-01T00:00:00Z")
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf(item2))
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken4")

        chatServiceInstance.getTranscript(ScanDirection.BACKWARD, SortKey.ASCENDING, 10, null, null)
        advanceUntilIdle()

        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken3")

        // Expect previousTranscriptNextToken to be the same when receiving an empty array using startPosition.
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf())
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken5")
        val mockStartPosition = StartPosition()
        mockStartPosition.id = "1234"

        chatServiceInstance.getTranscript(ScanDirection.BACKWARD, SortKey.ASCENDING, 10, null, mockStartPosition)
        advanceUntilIdle()

        assertEquals(chatServiceInstance.previousTranscriptNextToken, "nextToken3")
    }

    @Test
    fun test_fetchReconnectedTranscript_success() = runTest {
        val chatDetails = ChatDetails(participantToken = "token")
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        chatService.createChatSession(chatDetails)
        advanceUntilIdle()

        // Create a mock GetTranscriptResult and configure it to return expected values
        val mockGetTranscriptResult = mock<GetTranscriptResult>()

        `when`(mockGetTranscriptResult.initialContactId).thenReturn("")
        `when`(awsClient.getTranscript(anyOrNull())).thenReturn(Result.success(mockGetTranscriptResult))

        // Add items to the internal transcript and emit reconnection event.
        // This scenario should call getTranscript once since the empty transcript response.
        `when`(mockGetTranscriptResult.transcript).thenReturn(listOf())
        `when`(mockGetTranscriptResult.nextToken).thenReturn("nextToken1")

        val transcriptItem1 = Message(id = "1", timeStamp = "2024-01-01T00:00:00Z", participant = "user", contentType = "text/plain", text = "Hello")
        val transcriptItem2 = Message(id = "2", timeStamp = "2025-01-01T00:01:00Z", participant = "agent", contentType = "text/plain", text = "Hi")
        chatService.internalTranscript.add(transcriptItem1)
        chatService.internalTranscript.add(transcriptItem2)
        val chatEvent = ChatEvent.ConnectionReEstablished
        eventSharedFlow.emit(chatEvent)
        advanceUntilIdle()
        verify(awsClient, times(1)).getTranscript(anyOrNull())
    }

    @Test
    fun test_sendMessageReceipt_success() = runTest {
        val messageId = "messageId123"
        val receiptType = MessageReceiptType.MESSAGE_READ

        `when`(messageReceiptsManager.throttleAndSendMessageReceipt(receiptType, messageId)).thenReturn(Result.success(
            PendingMessageReceipts()
        ))

        val result = chatService.sendMessageReceipt(receiptType, messageId)

        assertTrue(result.isSuccess)
        verify(messageReceiptsManager).throttleAndSendMessageReceipt(receiptType, messageId)
    }

    @Test
    fun test_sendMessageReceipt_failure() = runTest {
        val messageId = "messageId123"
        val receiptType = MessageReceiptType.MESSAGE_READ

        `when`(messageReceiptsManager.throttleAndSendMessageReceipt(receiptType, messageId)).thenReturn(Result.failure(Exception("Receipt error")))

        val result = chatService.sendMessageReceipt(receiptType, messageId)

        assertTrue(result.isFailure)
        verify(messageReceiptsManager).throttleAndSendMessageReceipt(receiptType, messageId)
    }

    @Test
    fun test_eventPublisher_emitsCorrectEvent() = runTest {
        var assertCalled = false
        val chatDetails = ChatDetails(participantToken = "token")
        chatService.createChatSession(chatDetails)
        advanceUntilIdle()

        val chatEvent = ChatEvent.ChatEnded

        // Launch the flow collection within the test's coroutine scope
        val job = chatService.eventPublisher
            .onEach { event ->
                assertEquals(chatEvent, event)
                assertCalled = true
            }
            .launchIn(this)

        // Emit the event
        eventSharedFlow.emit(chatEvent)
        advanceUntilIdle()

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()

        if (!assertCalled) {
            fail("chatService.eventPublisher.onEach was not triggered")
        }
    }

    @Test
    fun test_transcriptPublisher_emitsCorrectTranscriptItem() = runTest {
        var assertCalled = false
        val chatDetails = ChatDetails(participantToken = "token")
        chatService.createChatSession(chatDetails)
        advanceUntilIdle()

        val transcriptItem = Message(id = "1", timeStamp = "mockedTimestamp", participant = "user",
            contentType = "text/plain", text = "Hello")

        val job = chatService.transcriptPublisher
            .onEach { item ->
                assertEquals(transcriptItem, item)
                assertCalled = true
            }
            .launchIn(this)

        // Emit the transcript item
        transcriptSharedFlow.emit(transcriptItem)
        advanceUntilIdle()
        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()

        if (!assertCalled) {
            fail("chatService.transcriptPublisher.onEach was not triggered")
        }
    }

    @Test
    fun test_transcriptListPublisher_emitsTranscriptList() = runTest {
        var assertCalled = false
        val chatDetails = ChatDetails(participantToken = "token")
        chatService.createChatSession(chatDetails)
        advanceUntilIdle()

        val transcriptItem1 = Message(id = "1", timeStamp = "2024-01-01T00:00:00Z", participant = "user", contentType = "text/plain", text = "Hello")
        val transcriptItem2 = Message(id = "2", timeStamp = "2024-01-01T00:01:00Z", participant = "agent", contentType = "text/plain", text = "Hi")

        // Launch the flow collection within the test's coroutine scope
        val job = chatService.transcriptListPublisher
            .onEach { transcriptData ->
                assertEquals(transcriptData.transcriptList.size, 2)
                assertEquals(transcriptData.transcriptList[0], transcriptItem1)
                assertEquals(transcriptData.transcriptList[1], transcriptItem2)
                assertCalled = true
            }
            .launchIn(this)

        // Emit the transcript list
        transcriptSharedFlow.emit(transcriptItem1)
        transcriptSharedFlow.emit(transcriptItem2)
        advanceUntilIdle()

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()

        if (!assertCalled) {
            fail("chatService.transcriptPublisher.onEach was not triggered")
        }
    }

    @Test
    fun test_chatSessionStatePublisher_emitsSessionState() = runTest {
        // Launch the flow collection within the test's coroutine scope
        val job = chatService.chatSessionStatePublisher
            .onEach { isActive ->
                assertTrue(isActive)
            }
            .launchIn(this)

        // Emit the session state
        chatSessionStateFlow.emit(true)

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()
    }

    @Test
    fun test_reset() = runTest {
        // Add message in internal transcript
        val transcriptItem = Message(id = "1", timeStamp = "mockedTimestamp", participant = "user",
            contentType = "text/plain", text = "Hello")
        chatService.internalTranscript.add(0, transcriptItem)

        // Execute reset
        chatService.reset()

        // Validate that websocket disconnected, tokens are reset and internal transcript is deleted
        verify(webSocketManager).disconnect("Resetting ChatService")
        verify(connectionDetailsProvider).reset()
        assertEquals(0, chatService.internalTranscript.size)
    }

    private fun createMockConnectionDetails(token : String): ConnectionDetails {
        return ConnectionDetails(
            connectionToken = token,
            websocketUrl = "mockedWebsocketUrl",
            expiry = "mockedExpiryTime"
        )
    }

}