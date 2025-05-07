package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.model.Dimension
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Metric
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.MetricRequestBody
import com.amazon.connect.chat.sdk.network.api.APIClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class MetricsManagerTest {

    @Mock
    private lateinit var apiClient: APIClient

    private lateinit var metricsManager: MetricsManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        metricsManager = MetricsManager(apiClient)

        // Disable monitoring initially to prevent automatic timer creation
        setPrivateField(metricsManager, "_isCsmDisabled", true)
    }

    @Test
    fun `test configure sets isCsmDisabled flag`() {
        val config = GlobalConfig(disableCsm = true)

        metricsManager.configure(config)

        val isCsmDisabled = getPrivateField(metricsManager, "_isCsmDisabled") as Boolean
        assert(isCsmDisabled)
    }

    @Test
    fun `test addCountMetric adds metric to list when CSM is enabled`() {
        setPrivateField(metricsManager, "_isCsmDisabled", false)
        val metricName = MetricName.CreateParticipantConnection

        metricsManager.addCountMetric(metricName)

        val metricList = getPrivateField(metricsManager, "metricList") as MutableList<*>
        assert(metricList.size == 1)
        val metric = metricList[0] as Metric
        assert(metric.metricName == metricName.name)
        assert(metric.value == 1)
        assert(metric.unit == "Count")
    }

    @Test
    fun `test addCountMetric does not add metric when CSM is disabled`() {
        setPrivateField(metricsManager, "_isCsmDisabled", true)
        val metricName = MetricName.SendMessage

        metricsManager.addCountMetric(metricName)

        val metricList = getPrivateField(metricsManager, "metricList") as MutableList<*>
        assert(metricList.isEmpty())
    }

    @Test
    fun `test createMetricRequestBody creates correct request body`() {
        setPrivateField(metricsManager, "_isCsmDisabled", false)
        val metricName = MetricName.SendMessage

        // Add a metric to the list
        metricsManager.addCountMetric(metricName)

        val requestBody = invokePrivateMethod(metricsManager, "createMetricRequestBody") as MetricRequestBody

        assert(requestBody.metricNamespace == "chat-widget")
        assert(requestBody.metricList.size == 1)
        assert(requestBody.metricList[0].metricName == metricName.name)
    }

    // Helper methods for accessing private fields and methods
    private fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    private fun getPrivateField(obj: Any, fieldName: String): Any? {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj)
    }

    private fun invokePrivateMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
        val method = obj.javaClass.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(obj, *args)
    }
}
