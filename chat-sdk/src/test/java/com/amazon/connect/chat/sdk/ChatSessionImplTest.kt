package com.amazon.connect.chat.sdk

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazonaws.regions.Regions
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner


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
        `when`(chatService.createChatSession(chatDetails)).thenThrow(RuntimeException("Network error"))

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
        `when`(chatService.disconnectChatSession()).thenThrow(RuntimeException("Network error"))

        val result = chatSession.disconnect()

        assertTrue(result.isFailure)
        verify(chatService).disconnectChatSession()
    }

}
