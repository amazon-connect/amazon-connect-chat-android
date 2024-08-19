package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.MetricsInterface
import com.amazon.connect.chat.sdk.network.MetricsManager
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
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

    private lateinit var chatService: ChatService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        chatService = ChatServiceImpl(awsClient, connectionDetailsProvider, webSocketManager, metricsManager)
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

    private fun createMockConnectionDetails(token : String): ConnectionDetails {
        return ConnectionDetails(
            connectionToken = token,
            websocketUrl = "mockedWebsocketUrl",
            expiry = "mockedExpiryTime"
        )
    }

}