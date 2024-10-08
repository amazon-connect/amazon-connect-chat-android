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
import com.amazon.connect.chat.sdk.network.AttachmentsManager
import com.amazon.connect.chat.sdk.network.MessageReceiptsManager
import com.amazon.connect.chat.sdk.network.MetricsManager
import com.amazon.connect.chat.sdk.network.PendingMessageReceipts
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import com.amazonaws.services.connectparticipant.model.GetTranscriptResult
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
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
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

    private lateinit var chatService: ChatService
    private lateinit var eventSharedFlow: MutableSharedFlow<ChatEvent>
    private lateinit var transcriptSharedFlow: MutableSharedFlow<TranscriptItem>
    private lateinit var chatSessionStateFlow: MutableStateFlow<Boolean>
    private lateinit var transcriptListSharedFlow: MutableSharedFlow<List<TranscriptItem>>


    private val mockUri: Uri = Uri.parse("https://example.com/dummy.pdf")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        eventSharedFlow = MutableSharedFlow()
        transcriptSharedFlow = MutableSharedFlow()
        transcriptListSharedFlow = MutableSharedFlow()
        chatSessionStateFlow = MutableStateFlow(false)

        `when`(webSocketManager.eventPublisher).thenReturn(eventSharedFlow)
        `when`(webSocketManager.transcriptPublisher).thenReturn(transcriptSharedFlow)
        `when`(connectionDetailsProvider.chatSessionState).thenReturn(chatSessionStateFlow)

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
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(awsClient.disconnectParticipantConnection(mockConnectionDetails.connectionToken)).thenReturn(Result.success(
            DisconnectParticipantResult()
        ))

        val result = chatService.disconnectChatSession()

        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient).disconnectParticipantConnection(mockConnectionDetails.connectionToken)
    }

    @Test
    fun test_disconnectParticipantConnection_failure() = runTest {
        val mockConnectionDetails = createMockConnectionDetails("invalid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(awsClient.disconnectParticipantConnection(mockConnectionDetails.connectionToken)).thenThrow(RuntimeException("Network error"))

        val result = chatService.disconnectChatSession()
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient).disconnectParticipantConnection(mockConnectionDetails.connectionToken)
    }

    @Test
    fun test_disconnectParticipantConnection_noConnectionDetails() = runTest {
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(null)
        val result = chatService.disconnectChatSession()
        assertTrue(result.isFailure)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(awsClient, never()).disconnectParticipantConnection(anyString())
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
    fun test_downloadAttachment_success() = runTest {
        val attachmentId = "attachment123"
        val fileName = "file.pdf"
        val mockUrl = URL("https://example.com/file")
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.downloadAttachment(attachmentId, fileName, mockConnectionDetails.connectionToken)).thenReturn(Result.success(mockUrl))

        val result = chatService.downloadAttachment(attachmentId, fileName)

        assertTrue(result.isSuccess)
        assertEquals(mockUrl, result.getOrNull())
        verify(attachmentsManager).downloadAttachment(attachmentId, fileName, mockConnectionDetails.connectionToken)
    }

    @Test
    fun test_downloadAttachment_failure() = runTest {
        val attachmentId = "attachment123"
        val fileName = "file.pdf"
        val mockConnectionDetails = createMockConnectionDetails("valid_token")

        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.downloadAttachment(attachmentId, fileName, mockConnectionDetails.connectionToken)).thenThrow(RuntimeException("Download error"))

        val result = chatService.downloadAttachment(attachmentId, fileName)

        assertTrue(result.isFailure)
        verify(attachmentsManager).downloadAttachment(attachmentId, fileName, mockConnectionDetails.connectionToken)
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
        val chatEvent = ChatEvent.ConnectionEstablished

        // Launch the flow collection within the test's coroutine scope
        val job = chatService.eventPublisher
            .onEach { event ->
                assertEquals(chatEvent, event)
            }
            .launchIn(this)

        // Emit the event
        eventSharedFlow.emit(chatEvent)

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()
    }

    @Test
    fun test_transcriptPublisher_emitsCorrectTranscriptItem() = runTest {
        val transcriptItem = Message(id = "1", timeStamp = "mockedTimestamp", participant = "user",
            contentType = "text/plain", text = "Hello")

        val job = chatService.transcriptPublisher
            .onEach { item ->
                assertEquals(transcriptItem, item)
            }
            .launchIn(this)

        // Emit the transcript item
        transcriptSharedFlow.emit(transcriptItem)

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()
    }

    @Test
    fun test_transcriptListPublisher_emitsTranscriptList() = runTest {
        val transcriptItem1 = Message(id = "1", timeStamp = "2024-01-01T00:00:00Z", participant = "user", contentType = "text/plain", text = "Hello")
        val transcriptItem2 = Message(id = "2", timeStamp = "2024-01-01T00:01:00Z", participant = "agent", contentType = "text/plain", text = "Hi")
        val transcriptList = listOf(transcriptItem1, transcriptItem2)

        // Launch the flow collection within the test's coroutine scope
        val job = chatService.transcriptListPublisher
            .onEach { items ->
                assertEquals(transcriptList, items)
            }
            .launchIn(this)

        // Emit the transcript list
        transcriptListSharedFlow.emit(transcriptList)

        // Cancel the job after testing to ensure the coroutine completes
        job.cancel()
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

    private fun createMockConnectionDetails(token : String): ConnectionDetails {
        return ConnectionDetails(
            connectionToken = token,
            websocketUrl = "mockedWebsocketUrl",
            expiry = "mockedExpiryTime"
        )
    }

}