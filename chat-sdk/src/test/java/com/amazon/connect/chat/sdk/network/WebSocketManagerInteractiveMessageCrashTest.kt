// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for loosened message property requirements in WebSocketManager.
 * Verifies all handler methods gracefully handle missing optional fields
 * instead of crashing with JSONException.
 *
 * Fixes: https://github.com/amazon-connect/amazon-connect-chat-android/issues/99
 * Related: P382365182
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WebSocketManagerInteractiveMessageCrashTest {

    @Mock
    private lateinit var mockNetworkConnectionManager: NetworkConnectionManager

    @Mock
    private lateinit var mockConnectionDetailsProvider: ConnectionDetailsProvider

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var webSocketManager: WebSocketManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockNetworkConnectionManager.isNetworkAvailable)
            .thenReturn(MutableStateFlow(true))
        webSocketManager = WebSocketManagerImpl(
            dispatcher = testDispatcher,
            networkConnectionManager = mockNetworkConnectionManager,
            connectionDetailsProvider = mockConnectionDetailsProvider
        )
    }

    // ==================== handleMessage ====================

    @Test
    fun `handleMessage - complete message parses correctly`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:36:47.251Z",
                "Content": "Hello",
                "ContentType": "text/plain",
                "Id": "msg-123",
                "Type": "MESSAGE",
                "DisplayName": "Customer",
                "ParticipantRole": "CUSTOMER"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("msg-123", result.id)
        assertEquals("Hello", result.text)
        assertEquals("Customer", result.displayName)
        assertEquals("CUSTOMER", result.participant)
        assertEquals("text/plain", result.contentType)
        assertEquals("2026-02-13T17:36:47.251Z", result.timeStamp)
    }

    @Test
    fun `handleMessage - missing optional fields defaults to empty strings`() = runTest {
        val json = """{"Type": "MESSAGE"}""".trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("", result.id)
        assertEquals("", result.text)
        assertEquals("", result.displayName)
        assertEquals("", result.participant)
        assertEquals("", result.contentType)
        assertEquals("", result.timeStamp)
    }

    @Test
    fun `handleMessage - interactive message without MessageMetadata parses`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:32:40.517Z",
                "Content": "{\"templateType\":\"ListPicker\"}",
                "ContentType": "application/vnd.amazonaws.connect.message.interactive",
                "Id": "msg-456",
                "Type": "MESSAGE",
                "DisplayName": "BOT",
                "ParticipantRole": "SYSTEM"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("msg-456", result.id)
        assertNull(result.metadata)
    }

    @Test
    fun `handleMessage - interactive message with MessageMetadata but no MessageId`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:32:40.517Z",
                "Content": "{\"templateType\":\"ListPicker\"}",
                "ContentType": "application/vnd.amazonaws.connect.message.interactive",
                "Id": "msg-789",
                "Type": "MESSAGE",
                "DisplayName": "BOT",
                "ParticipantRole": "SYSTEM",
                "MessageMetadata": {"Receipts": []}
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("msg-789", result.id)
        assertNotNull(result.metadata)
    }

    // ==================== handleMetadata (MESSAGEMETADATA) ====================

    @Test
    fun `handleMetadata - complete metadata parses correctly`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T05:30:25.900Z",
                "ContentType": "application/vnd.amazonaws.connect.event.message.metadata",
                "Type": "MESSAGEMETADATA",
                "MessageMetadata": {
                    "MessageId": "meta-123",
                    "Receipts": [{"DeliveredTimestamp": "2026-02-28T05:30:25.841Z", "ReadTimestamp": "2026-02-28T05:30:25.841Z", "RecipientParticipantId": "participant-1"}]
                }
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals("meta-123", result.id)
        assertEquals(MessageStatus.Read, result.status)
    }

    @Test
    fun `handleMetadata - missing MessageId falls back to outer Id`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:32:40.517Z",
                "ContentType": "application/vnd.amazonaws.connect.message.interactive",
                "Id": "fallback-id",
                "Type": "MESSAGEMETADATA",
                "MessageMetadata": {"Receipts": []}
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals("fallback-id", result.id)
    }

    @Test
    fun `handleMetadata - missing MessageId and no outer Id defaults to empty`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:32:40.517Z",
                "ContentType": "application/vnd.amazonaws.connect.message.interactive",
                "Type": "MESSAGEMETADATA",
                "MessageMetadata": {"Receipts": []}
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals("", result.id)
    }

    @Test
    fun `handleMetadata - missing MessageMetadata object entirely`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-13T17:32:40.517Z",
                "ContentType": "application/vnd.amazonaws.connect.message.interactive",
                "Id": "outer-id",
                "Type": "MESSAGEMETADATA"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals("outer-id", result.id)
    }

    @Test
    fun `handleMetadata - delivered status when no ReadTimestamp`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T05:30:25.900Z",
                "ContentType": "application/vnd.amazonaws.connect.event.message.metadata",
                "Type": "MESSAGEMETADATA",
                "MessageMetadata": {
                    "MessageId": "meta-456",
                    "Receipts": [{"DeliveredTimestamp": "2026-02-28T05:30:25.841Z", "RecipientParticipantId": "p-1"}]
                }
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals(MessageStatus.Delivered, result.status)
    }

    @Test
    fun `handleMetadata - empty receipts array`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T05:30:25.900Z",
                "ContentType": "application/vnd.amazonaws.connect.event.message.metadata",
                "Type": "MESSAGEMETADATA",
                "MessageMetadata": {"MessageId": "meta-789", "Receipts": []}
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as MessageMetadata
        assertEquals(MessageStatus.Delivered, result.status)
    }

    // ==================== handleParticipantEvent ====================

    @Test
    fun `handleParticipantEvent - joined event parses correctly`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T05:34:30.703Z",
                "ContentType": "application/vnd.amazonaws.connect.event.participant.joined",
                "Id": "evt-123",
                "Type": "EVENT",
                "DisplayName": "CUSTOMER",
                "ParticipantRole": "CUSTOMER"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("evt-123", result.id)
        assertEquals("CUSTOMER", result.displayName)
        assertEquals("CUSTOMER", result.participant)
    }

    @Test
    fun `handleParticipantEvent - missing fields defaults to empty`() = runTest {
        val json = """
            {
                "ContentType": "application/vnd.amazonaws.connect.event.participant.joined",
                "Type": "EVENT"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("", result.id)
        assertEquals("", result.displayName)
        assertEquals("", result.participant)
        assertEquals("", result.timeStamp)
    }

    // ==================== handleTyping ====================

    @Test
    fun `handleTyping - complete typing event parses correctly`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T05:34:33.910Z",
                "ContentType": "application/vnd.amazonaws.connect.event.typing",
                "Id": "typ-123",
                "Type": "EVENT",
                "DisplayName": "Agent",
                "ParticipantRole": "AGENT"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("typ-123", result.id)
        assertEquals("Agent", result.displayName)
        assertEquals("application/vnd.amazonaws.connect.event.typing", result.contentType)
    }

    @Test
    fun `handleTyping - missing fields defaults to empty`() = runTest {
        val json = """
            {
                "ContentType": "application/vnd.amazonaws.connect.event.typing",
                "Type": "EVENT"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("", result.id)
        assertEquals("", result.displayName)
        assertEquals("", result.participant)
    }

    // ==================== handleChatEnded ====================

    @Test
    fun `handleChatEnded - complete event parses correctly`() = runTest {
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(true)
        val json = """
            {
                "AbsoluteTime": "2026-02-28T06:00:00.000Z",
                "ContentType": "application/vnd.amazonaws.connect.event.chat.ended",
                "Id": "end-123",
                "Type": "EVENT"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("end-123", result.id)
        assertEquals("application/vnd.amazonaws.connect.event.chat.ended", result.contentType)
    }

    @Test
    fun `handleChatEnded - missing fields defaults to empty`() = runTest {
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(true)
        val json = """
            {
                "ContentType": "application/vnd.amazonaws.connect.event.chat.ended",
                "Type": "EVENT"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Event
        assertEquals("", result.id)
        assertEquals("", result.timeStamp)
    }

    // ==================== handleAttachment ====================

    @Test
    fun `handleAttachment - complete attachment parses correctly`() = runTest {
        val json = """
            {
                "AbsoluteTime": "2026-02-28T06:00:00.000Z",
                "Id": "att-123",
                "Type": "ATTACHMENT",
                "DisplayName": "Customer",
                "ParticipantRole": "CUSTOMER",
                "Attachments": [{"AttachmentName": "file.pdf", "ContentType": "application/pdf", "AttachmentId": "a-1"}]
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("att-123", result.id)
        assertEquals("file.pdf", result.text)
        assertEquals("a-1", result.attachmentId)
    }

    @Test
    fun `handleAttachment - missing fields defaults to empty`() = runTest {
        val json = """
            {
                "Type": "ATTACHMENT",
                "Attachments": [{"AttachmentName": "file.pdf", "ContentType": "application/pdf", "AttachmentId": "a-2"}]
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json) as Message
        assertEquals("", result.id)
        assertEquals("", result.displayName)
        assertEquals("", result.participant)
        assertEquals("", result.timeStamp)
    }

    @Test
    fun `handleAttachment - no attachments array returns null`() = runTest {
        val json = """
            {
                "Type": "ATTACHMENT",
                "Id": "att-456",
                "ParticipantRole": "CUSTOMER",
                "DisplayName": "Customer",
                "AbsoluteTime": "2026-02-28T06:00:00.000Z"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json)
        assertNull(result)
    }

    // ==================== Edge cases ====================

    @Test
    fun `invalid JSON returns null`() = runTest {
        val result = webSocketManager.parseTranscriptItemFromJson("not json")
        assertNull(result)
    }

    @Test
    fun `empty JSON object returns null`() = runTest {
        val result = webSocketManager.parseTranscriptItemFromJson("{}")
        assertNull(result)
    }

    @Test
    fun `unknown Type returns null`() = runTest {
        val json = """{"Type": "UNKNOWN_TYPE"}""".trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json)
        assertNull(result)
    }

    @Test
    fun `unknown event ContentType returns null`() = runTest {
        val json = """
            {
                "Type": "EVENT",
                "ContentType": "application/vnd.amazonaws.connect.event.unknown"
            }
        """.trimIndent()
        val result = webSocketManager.parseTranscriptItemFromJson(json)
        assertNull(result)
    }
}
