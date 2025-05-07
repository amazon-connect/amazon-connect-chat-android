package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageReceipts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.Timer

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MessageReceiptsManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var messageReceiptsManager: MessageReceiptsManagerImpl
    private val mockTimer = Timer()
    private val testMessageId = "test-message-id"
    private val testMessageId2 = "test-message-id-2"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        messageReceiptsManager = MessageReceiptsManagerImpl()
        messageReceiptsManager.timer = mockTimer
        messageReceiptsManager.throttleTime = 5.0 // 5 seconds
        messageReceiptsManager.deliveredThrottleTime = 3.0 // 3 seconds
        messageReceiptsManager.shouldSendMessageReceipts = true
    }

    @Test
    fun `test handleMessageReceipt with MESSAGE_READ adds to readReceiptSet`() = testScope.runTest {
        messageReceiptsManager.handleMessageReceipt(MessageReceiptType.MESSAGE_READ, testMessageId)
        
        assertEquals(testMessageId, messageReceiptsManager.throttleAndSendMessageReceipt(
            MessageReceiptType.MESSAGE_READ, testMessageId).getOrNull()?.readReceiptMessageId)
    }

    @Test
    fun `test handleMessageReceipt ignores MESSAGE_DELIVERED if already read`() = testScope.runTest {
        messageReceiptsManager.handleMessageReceipt(MessageReceiptType.MESSAGE_READ, testMessageId)
        
        messageReceiptsManager.handleMessageReceipt(MessageReceiptType.MESSAGE_DELIVERED, testMessageId)
        advanceTimeBy(3000) // Advance by deliveredThrottleTime
        
        // Delivered receipt should not be added
        val result = messageReceiptsManager.throttleAndSendMessageReceipt(
            MessageReceiptType.MESSAGE_DELIVERED, testMessageId).getOrNull()
        assertNull(result?.deliveredReceiptMessageId)
        assertEquals(testMessageId, result?.readReceiptMessageId)
    }

    @Test
    fun `test throttleAndSendMessageReceipt returns failure when receipts disabled`() = testScope.runTest {
        messageReceiptsManager.shouldSendMessageReceipts = false
        
        val result = messageReceiptsManager.throttleAndSendMessageReceipt(
            MessageReceiptType.MESSAGE_READ, testMessageId)
        
        assertTrue(result.isFailure)
        assertEquals("Sending message receipts is disabled", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test checkAndRemoveDuplicateReceipt removes delivered receipt if same as read receipt`() = testScope.runTest {
        val pendingReceipts = PendingMessageReceipts(
            deliveredReceiptMessageId = testMessageId,
            readReceiptMessageId = testMessageId
        )
        
        pendingReceipts.checkAndRemoveDuplicateReceipt()
        
        assertNull(pendingReceipts.deliveredReceiptMessageId)
        assertEquals(testMessageId, pendingReceipts.readReceiptMessageId)
    }

    @Test
    fun `test invalidateTimer cancels and nullifies timer`() {
        messageReceiptsManager.invalidateTimer()
        
        assertNull(messageReceiptsManager.timer)
    }
}