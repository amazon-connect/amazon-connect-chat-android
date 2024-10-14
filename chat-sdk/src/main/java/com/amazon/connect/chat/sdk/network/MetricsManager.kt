package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import javax.inject.Inject
import java.util.*
import kotlin.concurrent.timer
import com.amazon.connect.chat.sdk.utils.MetricsUtils
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.Metric
import com.amazon.connect.chat.sdk.model.Dimension
import com.amazon.connect.chat.sdk.model.GlobalConfig

class MetricsManager @Inject constructor(
    private var apiClient: APIClient
) {
    private var metricList: MutableList<Metric> = mutableListOf()
    private var isMonitoring: Boolean = false
    private var timer: Timer? = null
    private var shouldRetry: Boolean = true
    private var _isCsmDisabled: Boolean = false

    init {
        if (!_isCsmDisabled) {
            monitorAndSendMetrics()
        }
    }

    fun configure(config: GlobalConfig) {
        _isCsmDisabled = config.disableCsm
    }

    @Synchronized
    private fun monitorAndSendMetrics() {
        if (isMonitoring) return
        isMonitoring = true

        timer = timer(initialDelay = 10000, period = 10000) {
            if (metricList.isNotEmpty()) {
                val metricRequestBody = createMetricRequestBody()
                apiClient.sendMetrics(metricRequestBody) { response ->
                    if (response != null && response.isSuccessful) {
                        metricList = mutableListOf()
                        isMonitoring = false
                        timer?.cancel()
                    } else {
                        // We should retry once after 10s delay, otherwise we will send the missed
                        // payload with the next batch of metrics
                        if (shouldRetry) {
                            shouldRetry = false
                        } else {
                            isMonitoring = false
                            shouldRetry = true
                            timer?.cancel()
                        }
                    }
                }
            }
        }
    }

    private fun createMetricRequestBody(): MetricRequestBody {
        return MetricRequestBody(
            metricNamespace = "chat-widget",
            metricList = metricList
        )
    }

    private fun getCountMetricDimensions(): List<Dimension> {
        return listOf(
            Dimension(name = "WidgetType", value = "MobileChatSDK"),
            Dimension(name = "SDKPlatform", value = "Android"),
            Dimension(name = "Category", value = "API"),
            Dimension(name = "Metric", value = "Count")
        )
    }

    fun addCountMetric(metricName: MetricName) {
        val currentTime = MetricsUtils.getCurrentMetricTimestamp()
        val countMetricDimensions = getCountMetricDimensions()
        val countMetric = Metric(
            dimensions = countMetricDimensions,
            metricName = metricName.name,
            namespace = "chat-widget",
            optionalDimensions = emptyList(),
            timestamp = currentTime,
            unit = "Count",
            value = 1
        )

        addMetric(countMetric)
    }

    private fun addMetric(metric: Metric) {
        if (_isCsmDisabled) {
            return
        }

        metricList.add(0, metric)
        monitorAndSendMetrics()
    }
}
