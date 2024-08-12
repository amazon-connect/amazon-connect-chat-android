package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import com.amazonaws.services.connectparticipant.model.ConnectionCredentials
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionRequest
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionResult
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantRequest
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
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
}
