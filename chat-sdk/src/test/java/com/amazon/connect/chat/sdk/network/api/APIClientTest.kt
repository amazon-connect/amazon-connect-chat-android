package com.amazon.connect.chat.sdk.network.api

import com.amazon.connect.chat.sdk.model.Dimension
import com.amazon.connect.chat.sdk.model.Metric
import com.amazon.connect.chat.sdk.model.MetricRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class APIClientTest {

    @Mock
    private lateinit var mockMetricsInterface: MetricsInterface

    @Mock
    private lateinit var mockAttachmentsInterface: AttachmentsInterface

    @Mock
    private lateinit var mockMetricsCall: Call<Any>

    private lateinit var apiClient: APIClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        apiClient = APIClient(mockMetricsInterface, mockAttachmentsInterface)
    }

    @Test
    fun `sendMetrics calls metricsInterface with correct parameters and handles success`() {
        // Given
        val dimension = Dimension("contactId", "contact-123")
        val metric = Metric(
            dimensions = listOf(dimension),
            metricName = "TestMetric",
            namespace = "TestNamespace",
            optionalDimensions = emptyList(),
            timestamp = "2023-01-01T12:00:00Z",
            unit = "Count",
            value = 1
        )
        val metricRequestBody = MetricRequestBody(
            metricList = listOf(metric),
            metricNamespace = "TestNamespace"
        )
        val successResponse = Response.success<Any>(Unit)
        
        // Set up the mock to return our call for any MetricRequestBody
        Mockito.`when`(mockMetricsInterface.sendMetrics(metricRequestBody)).thenReturn(mockMetricsCall)
        
        // Set up the enqueue behavior
        Mockito.doAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<Any>
            callback.onResponse(mockMetricsCall, successResponse)
            null
        }.`when`(mockMetricsCall).enqueue(Mockito.any())
        
        var capturedResponse: Response<Any>? = null
        apiClient.sendMetrics(metricRequestBody) { response ->
            capturedResponse = response
        }
        
        // Verify metricsInterface was called with the correct parameters
        Mockito.verify(mockMetricsInterface).sendMetrics(metricRequestBody)
        
        // Verify the callback was called with the correct response
        assertEquals(successResponse, capturedResponse)
    }

    @Test
    fun `sendMetrics handles failure correctly`() {
        val dimension = Dimension("contactId", "contact-123")
        val metric = Metric(
            dimensions = listOf(dimension),
            metricName = "TestMetric",
            namespace = "TestNamespace",
            optionalDimensions = emptyList(),
            timestamp = "2023-01-01T12:00:00Z",
            unit = "Count",
            value = 1
        )
        val metricRequestBody = MetricRequestBody(
            metricList = listOf(metric),
            metricNamespace = "TestNamespace"
        )
        val throwable = Throwable("Network error")
        
        // Set up the mock to return our call for any MetricRequestBody
        Mockito.`when`(mockMetricsInterface.sendMetrics(metricRequestBody)).thenReturn(mockMetricsCall)
        
        // Set up the enqueue behavior
        Mockito.doAnswer { invocation ->
            val callback = invocation.arguments[0] as Callback<Any>
            callback.onFailure(mockMetricsCall, throwable)
            null
        }.`when`(mockMetricsCall).enqueue(Mockito.any())
        
        var capturedResponse: Response<Any>? = null
        apiClient.sendMetrics(metricRequestBody) { response ->
            capturedResponse = response
        }
        
        // Verify the callback was called with null response
        assertNull(capturedResponse)
    }
}