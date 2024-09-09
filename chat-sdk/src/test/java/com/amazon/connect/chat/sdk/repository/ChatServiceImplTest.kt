package com.amazon.connect.chat.sdk.repository

import android.net.Uri
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.AttachmentsManager
import com.amazon.connect.chat.sdk.network.MetricsManager
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
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
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChatServiceImplTest {

    @Mock
    private lateinit var apiClient: APIClient

    @Mock
    private lateinit var awsClient: AWSClient

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


    private val mockUri: Uri = Uri.parse("https://example.com/dummy")

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
            awsClient,
            connectionDetailsProvider,
            webSocketManager,
            metricsManager,
            attachmentsManager
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
    fun test_sendAttachment_success() = runTest {
        val mockConnectionDetails = createMockConnectionDetails("valid_token")
        `when`(connectionDetailsProvider.getConnectionDetails()).thenReturn(mockConnectionDetails)
        `when`(attachmentsManager.sendAttachment(mockConnectionDetails.connectionToken, mockUri)).thenReturn(Unit)

        val result = chatService.sendAttachment(mockUri)

        assertTrue(result.isSuccess)
        verify(connectionDetailsProvider).getConnectionDetails()
        verify(attachmentsManager).sendAttachment(mockConnectionDetails.connectionToken, mockUri)
    }

    @Test
    fun test_sendAttachment_failure() = runTest {
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

    private fun createMockConnectionDetails(token : String): ConnectionDetails {
        return ConnectionDetails(
            connectionToken = token,
            websocketUrl = "mockedWebsocketUrl",
            expiry = "mockedExpiryTime"
        )
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

}