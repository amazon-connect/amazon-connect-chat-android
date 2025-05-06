package com.amazon.connect.chat.androidchatexample.utils

import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageStatus
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CommonUtilsTest {

    @Test
    fun formatTime_validTimestamp_returnsFormattedTime() {
        val timestamp = "2023-05-15T14:30:45.123Z"
        
        val result = CommonUtils.formatTime(timestamp)
        
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun formatTime_invalidTimestamp_throwsParseException() {
        val invalidTimestamp = "invalid-timestamp"
        
        try {
            CommonUtils.formatTime(invalidTimestamp)
            fail("Expected a ParseException to be thrown")
        } catch (e: java.text.ParseException) {
            // Test passes if exception is thrown
            assertTrue(e.message?.contains("Unparseable date") ?: false)
        }
    }

    @Test
    fun formatDate_forLogsTrue_returnsCorrectFormat() {
        val timeMillis = 1684159845123L // 2023-05-15T14:30:45.123Z
        
        val result = CommonUtils.formatDate(timeMillis, true)
        
        // Create the same formatter to verify the expected result
        val utcFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val expected = utcFormatter.format(Date(timeMillis))
        assertEquals(expected, result)
    }

    @Test
    fun formatDate_forLogsFalse_returnsCorrectFormat() {
        val timeMillis = 1684159845123L // 2023-05-15T14:30:45.123Z
        
        val result = CommonUtils.formatDate(timeMillis, false)
        
        // Create the same formatter to verify the expected result
        val utcFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val expected = utcFormatter.format(Date(timeMillis))
        assertEquals(expected, result)
    }

    @Test
    fun customMessageStatus_delivered_returnsCorrectString() {
        val status = MessageStatus.Delivered
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertEquals("Delivered", result)
    }

    @Test
    fun customMessageStatus_read_returnsCorrectString() {
        val status = MessageStatus.Read
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertEquals("Read", result)
    }

    @Test
    fun customMessageStatus_sending_returnsCorrectString() {
        val status = MessageStatus.Sending
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertEquals("Sending", result)
    }

    @Test
    fun customMessageStatus_failed_returnsDefaultMessage() {
        val status = MessageStatus.Failed
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun customMessageStatus_sent_returnsCorrectString() {
        val status = MessageStatus.Sent
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertEquals("Sent", result)
    }

    @Test
    fun customMessageStatus_null_returnsEmptyString() {
        val status: MessageStatus? = null
        
        val result = CommonUtils.customMessageStatus(status)
        
        assertEquals("", result)
    }

    @Test
    fun retryButtonEnabled_failedStatus_returnsTrue() {
        val status = MessageStatus.Failed
        
        val result = CommonUtils.retryButtonEnabled(status)
        
        assertTrue(result)
    }

    @Test
    fun retryButtonEnabled_otherStatus_returnsFalse() {
        // Test with various non-failed statuses
        assertFalse(CommonUtils.retryButtonEnabled(MessageStatus.Delivered))
        assertFalse(CommonUtils.retryButtonEnabled(MessageStatus.Read))
        assertFalse(CommonUtils.retryButtonEnabled(MessageStatus.Sending))
        assertFalse(CommonUtils.retryButtonEnabled(MessageStatus.Sent))
        assertFalse(CommonUtils.retryButtonEnabled(null))
    }
}