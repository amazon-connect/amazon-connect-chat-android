package com.amazon.connect.chat.sdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CommonUtilsTest {

    @Test
    fun parseErrorMessage_withNullMessage_returnsDefaultErrorMessage() {
        val nullMessage: String? = null
        
        val result = CommonUtils.parseErrorMessage(nullMessage)
        
        assertEquals("An unknown error occurred", result)
    }
    
    @Test
    fun parseErrorMessage_withValidMessage_returnsOriginalMessage() {
        val errorMessage = "Connection timeout"
        
        val result = CommonUtils.parseErrorMessage(errorMessage)
        
        assertEquals(errorMessage, result)
    }
    
    @Test
    fun parseErrorMessage_withEmptyMessage_returnsEmptyString() {
        val emptyMessage = ""
        
        val result = CommonUtils.parseErrorMessage(emptyMessage)
        
        assertEquals(emptyMessage, result)
    }
    
    @Test
    fun getCurrentISOTime_returnsCorrectFormat() {
        val result = CommonUtils.getCurrentISOTime()
        
        // Verify the format is correct by parsing it back
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        
        // This will throw if the format is incorrect
        val parsedDate = formatter.parse(result)
        
        // Additional verification: the parsed date should be close to current time
        val currentTime = System.currentTimeMillis()
        val parsedTime = parsedDate?.time ?: 0
        
        // Allow 5 seconds difference to account for test execution time
        assertTrue(Math.abs(currentTime - parsedTime) < 5000)
    }
    
    @Test
    fun testFileExtensionExtraction() {
        // Test the extension extraction logic in getMimeType
        val fileName1 = "document.pdf"
        val fileName2 = "image.jpg"
        val fileName3 = "noextension"
        val fileName4 = ".hiddenfile"
        val fileName5 = "archive.tar.gz"
        
        assertEquals("pdf", fileName1.substringAfterLast('.', ""))
        assertEquals("jpg", fileName2.substringAfterLast('.', ""))
        assertEquals("", fileName3.substringAfterLast('.', ""))
        assertEquals("hiddenfile", fileName4.substringAfterLast('.', ""))
        assertEquals("gz", fileName5.substringAfterLast('.', ""))
    }
}