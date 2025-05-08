package com.amazon.connect.chat.sdk.provider

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConnectionDetailsProviderImplTest {

    private lateinit var connectionDetailsProvider: ConnectionDetailsProviderImpl

    @Mock
    private lateinit var mockConnectionDetails: ConnectionDetails

    @Mock
    private lateinit var mockChatDetails: ChatDetails

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        connectionDetailsProvider = ConnectionDetailsProviderImpl()
    }

    @Test
    fun `updateConnectionDetails stores connection details`() {
        connectionDetailsProvider.updateConnectionDetails(mockConnectionDetails)
        
        val result = connectionDetailsProvider.getConnectionDetails()
        assertSame(mockConnectionDetails, result)
    }

    @Test
    fun `updateChatDetails stores chat details`() {
        connectionDetailsProvider.updateChatDetails(mockChatDetails)
        
        val result = connectionDetailsProvider.getChatDetails()
        assertSame(mockChatDetails, result)
    }

    @Test
    fun `getConnectionDetails returns null when not set`() {
        val result = connectionDetailsProvider.getConnectionDetails()
        
        assertNull(result)
    }

    @Test
    fun `getChatDetails returns null when not set`() {
        val result = connectionDetailsProvider.getChatDetails()
        
        assertNull(result)
    }

    @Test
    fun `isChatSessionActive returns false by default`() {
        val result = connectionDetailsProvider.isChatSessionActive()
        
        assertFalse(result)
    }

    @Test
    fun `setChatSessionState updates chat session state`() {
        connectionDetailsProvider.setChatSessionState(true)
        
        val result = connectionDetailsProvider.isChatSessionActive()
        assertTrue(result)
    }

    @Test
    fun `chatSessionState flow emits current state`() = runBlocking {
        connectionDetailsProvider.setChatSessionState(true)
        
        val result = connectionDetailsProvider.chatSessionState.first()
        
        assertTrue(result)
    }

    @Test
    fun `reset clears all data`() {
        connectionDetailsProvider.updateConnectionDetails(mockConnectionDetails)
        connectionDetailsProvider.updateChatDetails(mockChatDetails)
        connectionDetailsProvider.setChatSessionState(true)
        
        connectionDetailsProvider.reset()
        
        assertNull(connectionDetailsProvider.getConnectionDetails())
        assertNull(connectionDetailsProvider.getChatDetails())
        assertFalse(connectionDetailsProvider.isChatSessionActive())
    }

    @Test
    fun `updateConnectionDetails replaces previous value`() {
        val initialConnectionDetails = ConnectionDetails(
            websocketUrl = "initial-url",
            connectionToken = "initial-token",
            expiry = "2023-12-31T23:59:59Z"
        )
        val newConnectionDetails = ConnectionDetails(
            websocketUrl = "new-url",
            connectionToken = "new-token",
            expiry = "2024-12-31T23:59:59Z"
        )
        
        connectionDetailsProvider.updateConnectionDetails(initialConnectionDetails)
        connectionDetailsProvider.updateConnectionDetails(newConnectionDetails)
        
        val result = connectionDetailsProvider.getConnectionDetails()
        assertEquals("new-url", result?.websocketUrl)
        assertEquals("new-token", result?.connectionToken)
    }

    @Test
    fun `updateChatDetails replaces previous value`() {
        val initialChatDetails = ChatDetails(
            contactId = "initial-contact-id",
            participantId = "initial-participant-id",
            participantToken = "initial-token"
        )
        val newChatDetails = ChatDetails(
            contactId = "new-contact-id",
            participantId = "new-participant-id",
            participantToken = "new-token"
        )
        
        connectionDetailsProvider.updateChatDetails(initialChatDetails)
        connectionDetailsProvider.updateChatDetails(newChatDetails)
        
        val result = connectionDetailsProvider.getChatDetails()
        assertEquals("new-contact-id", result?.contactId)
        assertEquals("new-participant-id", result?.participantId)
        assertEquals("new-token", result?.participantToken)
    }
}