package com.amazon.connect.chat.sdk.utils

import com.amazonaws.regions.Regions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun testConstantValues() {
        // Test string constants
        assertEquals("AWSConnectParticipant", Constants.AWS_CONNECT_PARTICIPANT_KEY)
        assertEquals("AGENT", Constants.AGENT)
        assertEquals("CUSTOMER", Constants.CUSTOMER)
        assertEquals("SYSTEM", Constants.SYSTEM)
        assertEquals("UNKNOWN", Constants.UNKNOWN)
        assertEquals("MESSAGE", Constants.MESSAGE)
        assertEquals("ATTACHMENT", Constants.ATTACHMENT)
        assertEquals("EVENT", Constants.EVENT)
        assertEquals("QuickReply", Constants.QUICK_REPLY)
        assertEquals("ListPicker", Constants.LIST_PICKER)
        assertEquals("Panel", Constants.PANEL)
        assertEquals("TimePicker", Constants.TIME_PICKER)
        assertEquals("Carousel", Constants.CAROUSEL)
    }

    @Test
    fun testNumericConstants() {
        assertEquals(5.0, Constants.MESSAGE_RECEIPT_THROTTLE_TIME, 0.0)
        assertEquals(3.0, Constants.MESSAGE_RECEIPT_DELIVERED_THROTTLE_TIME, 0.0)
    }

    @Test
    fun testListConstants() {
        val requestTypes = Constants.ACPS_REQUEST_TYPES
        assertEquals(2, requestTypes.size)
        assertTrue(requestTypes.contains("WEBSOCKET"))
        assertTrue(requestTypes.contains("CONNECTION_CREDENTIALS"))
    }

    @Test
    fun testRegionConstant() {
        assertEquals(Regions.US_WEST_2, Constants.DEFAULT_REGION)
    }

    @Test
    fun testErrorMessages() {
        val testReason = "network error"
        assertEquals("Failed to create connection: network error.", Constants.Error.connectionCreated(testReason))
        assertEquals("Failed to create connection: network error.", Constants.Error.connectionFailed(testReason))
    }

    @Test
    fun testAttachmentTypeMap() {
        // Test the size of the map
        assertEquals(17, Constants.attachmentTypeMap.size)

        // Test some key mappings
        assertEquals("text/csv", Constants.attachmentTypeMap["csv"])
        assertEquals("application/pdf", Constants.attachmentTypeMap["pdf"])
        assertEquals("image/png", Constants.attachmentTypeMap["png"])
        assertEquals("text/plain", Constants.attachmentTypeMap["txt"])

        // Test some document formats
        assertEquals("application/msword", Constants.attachmentTypeMap["doc"])
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Constants.attachmentTypeMap["docx"])

        // Test some media formats
        assertEquals("image/jpeg", Constants.attachmentTypeMap["jpg"])
        assertEquals("video/mp4", Constants.attachmentTypeMap["mp4"])
        assertEquals("audio/wav", Constants.attachmentTypeMap["wav"])
    }
}
