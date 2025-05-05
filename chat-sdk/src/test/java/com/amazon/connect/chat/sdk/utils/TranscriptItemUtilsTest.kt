package com.amazon.connect.chat.sdk.utils

import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazonaws.services.connectparticipant.model.ChatItemType
import com.amazonaws.services.connectparticipant.model.Item
import com.amazonaws.services.connectparticipant.model.ParticipantRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TranscriptItemUtilsTest {
    private val INTERACTIVE_MESSAGE_CONTENTS = arrayOf(
        // List Picker
        "{\"templateIdentifier\":\"uuid\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}}",
        // Time Picker
        "{\"templateType\":\"TimePicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Schedule appointment\",\"subtitle\":\"Tap to select option\",\"timeslots\":[{\"date\":\"2026-01-02T00:00+00:00\",\"duration\":60},{\"date\":\"2026-01-03T00:00+00:00\",\"duration\":60},{\"date\":\"2026-01-04T00:00+00:00\",\"duration\":60}],\"location\":{\"title\":\"Oscar\",\"latitude\":47.616299,\"longitude\":-122.333031,\"radius\":1},\"timeZoneOffset\":-420}}}",
        // Panel
        "{\"templateIdentifier\":\"uuid\",\"templateType\":\"Panel\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\"},{\"title\":\"Orange\"},{\"title\":\"Banana\"}]}}}",
        // Quick Reply
        "{\"templateType\":\"QuickReply\",\"version\":\"1.0\",\"data\":{\"replyMessage\":{\"title\":\"Thanks for selecting!\"},\"content\":{\"title\":\"How was your experience?\",\"elements\":[{\"title\":\"Great\"},{\"title\":\"Good\"},{\"title\":\"Ok\"},{\"title\":\"Poor\"},{\"title\":\"Terrible\"}]}}}",
        // Carousel
        "{\"templateType\":\"Carousel\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Select from produce carousel\",\"omitTitleFromCarouselResponse\":false,\"elements\":[{\"templateIdentifier\":\"interactiveCarousel001\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel002\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel003\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel004\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}},{\"templateIdentifier\":\"interactiveCarousel005\",\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"What produce would you like to buy?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/fruits.jpg\",\"elements\":[{\"title\":\"Apple\",\"subtitle\":\"$1.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/apple.jpg\"},{\"title\":\"Orange\",\"subtitle\":\"$1.50\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/orange.jpg\"},{\"title\":\"Banana\",\"subtitle\":\"$2.00\",\"imageType\":\"URL\",\"imageData\":\"https://undefined.s3.undefined.amazonaws.com/banana.jpg\"}]}}}]}}}}"
    )

    @Test
    fun test_serializeTranscriptItem() {
        INTERACTIVE_MESSAGE_CONTENTS.forEach { content ->
            val item = Item()
                .withId("e1d3bc93-fc72-421b-95f6-13b37626668b")
                .withContent(content)
                .withContentType("application/vnd.amazonaws.connect.message.interactive")
                .withParticipantId("c8528570-62e6-47d9-ab5d-b6e152a07935")
                .withParticipantRole(ParticipantRole.SYSTEM)
                .withType(ChatItemType.MESSAGE)
            val result = TranscriptItemUtils.serializeTranscriptItem(item)
            assertNotNull(result)
        }
    }

    @Test
    fun test_serializeTranscriptItem_withMinimalProperties() {
        // Create an item with minimal properties
        val item = Item()
            .withId(null)
            .withContent(null)
            .withContentType(null)
        
        // Test serialization
        val result = TranscriptItemUtils.serializeTranscriptItem(item)
        
        // Verify result is not null and contains expected empty/null values
        assertNotNull(result)
        assertTrue(result?.contains("\"Id\":\"\"") == true)
        assertTrue(result?.contains("\"ParticipantRole\":null") == true)
        assertTrue(result?.contains("\"ContentType\":\"\"") == true)
        assertTrue(result?.contains("\"Content\":\"\"") == true)
        assertTrue(result?.contains("\"Type\":null") == true)
    }

    @Test
    fun test_serializeTranscriptItem_withMalformedContent() {
        // Create an item with malformed JSON content
        val item = Item()
            .withContent("{malformed json")
            .withContentType("application/json")
        
        // Test serialization
        val result = TranscriptItemUtils.serializeTranscriptItem(item)
        
        // Verify result is not null and contains the malformed content
        assertNotNull(result)
        assertTrue(result?.contains("\"Content\":\"{malformed json\"") == true)
    }

    @Test
    fun test_createDummyEndedEvent() {
        // Test creating a dummy ended event
        val event = TranscriptItemUtils.createDummyEndedEvent()
        
        // Verify event properties
        assertNotNull(event)
        assertEquals(ContentType.ENDED.type, event.contentType)
        assertEquals("chat-ended-event", event.id)
        assertNull(event.text)
        assertNotNull(event.timeStamp)
        assertNotNull(event.serializedContent)
    }

    @Test
    fun test_createDummyMessage() {
        // Test creating a dummy message
        val content = "Test message"
        val contentType = "text/plain"
        val status = MessageStatus.Sent
        val attachmentId = "att-456"
        val displayName = "Test User"
        
        val message = TranscriptItemUtils.createDummyMessage(
            content, contentType, status, attachmentId, displayName
        )
        
        // Verify message properties
        assertNotNull(message)
        assertEquals("CUSTOMER", message.participant)
        assertEquals(content, message.text)
        assertEquals(contentType, message.contentType)
        assertEquals(MessageDirection.OUTGOING, message.messageDirection)
        assertEquals(attachmentId, message.attachmentId)
        assertEquals(displayName, message.displayName)
        assertNotNull(message.id)
        assertNotNull(message.timeStamp)
        
        // Verify metadata - use safe call operator since metadata might be null
        assertNotNull(message.metadata)
        assertEquals(message.id, message.metadata?.id)
        assertEquals(status, message.metadata?.status)
        assertEquals(message.timeStamp, message.metadata?.timeStamp)
        assertEquals(contentType, message.metadata?.contentType)
        assertEquals(MessageDirection.OUTGOING, message.metadata?.eventDirection)
    }
}