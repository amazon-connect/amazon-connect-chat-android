package com.amazon.connect.chat.sdk.utils

import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MetricsUtilsTest {

    @Test
    fun getCurrentMetricTimestamp_returnsCorrectFormat() {
        val timestamp = MetricsUtils.getCurrentMetricTimestamp()

        // Print the timestamp to see its actual format
        SDKLogger.logger.logDebug { "Timestamp: $timestamp" }

        // Verify the timestamp can be parsed back using the same formatter
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val parsedDate = formatter.parse(timestamp)

        // Verify the parsed date is close to current time
        val currentTime = System.currentTimeMillis()
        val parsedTime = parsedDate?.time ?: 0
        assertTrue("Parsed time should be close to current time",
            Math.abs(currentTime - parsedTime) < 5000) // Within 5 seconds
    }

    @Test
    fun getMetricsEndpoint_returnsCorrectEndpoint() {
        val endpoint = MetricsUtils.getMetricsEndpoint()

        assertEquals(
            "https://ieluqbvv.telemetry.connect.us-west-2.amazonaws.com/prod/",
            endpoint
        )
    }
}
