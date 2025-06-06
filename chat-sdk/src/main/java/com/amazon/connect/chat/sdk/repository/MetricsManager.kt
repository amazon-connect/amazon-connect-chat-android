// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import javax.inject.Inject
import java.util.*
import kotlin.concurrent.timer
import com.amazon.connect.chat.sdk.utils.MetricsUtils
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.Metric
import com.amazon.connect.chat.sdk.model.Dimension
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.network.api.APIClient

class MetricsManager @Inject constructor(
    private var apiClient: APIClient
) {
    private var metricList: MutableList<Metric> = mutableListOf()
    private var isMonitoring: Boolean = false
    private var timer: Timer? = null
    private var retryCount: Int = 0
    private var _isCsmDisabled: Boolean = false
    private val maxRetries = 3
    private val monitoringPeriod = 10000L // 10 seconds
    private val initialDelay = 1000L // 1 second

    companion object {
        private const val METRIC_NAMESPACE = "chat-widget"
    }

    init {
        if (!_isCsmDisabled) {
            startMonitoring()
        }
    }

    fun configure(config: GlobalConfig) {
        val wasDisabled = _isCsmDisabled
        _isCsmDisabled = config.disableCsm

        if (wasDisabled && !_isCsmDisabled) {
            startMonitoring()
        } else if (!wasDisabled && _isCsmDisabled) {
            stopMonitoring()
        }
    }

    @Synchronized
    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        retryCount = 0

        timer = timer(initialDelay = initialDelay, period = monitoringPeriod) {
            if (metricList.isNotEmpty()) {
                sendMetrics()
            }
        }
    }

    private fun stopMonitoring() {
        timer?.cancel()
        timer = null
        isMonitoring = false
    }

    private fun sendMetrics() {
        val currentMetrics = synchronized(metricList) {
            val metrics = metricList.toList()
            metricList.clear()
            metrics
        }

        if (currentMetrics.isEmpty()) return

        val metricRequestBody = createMetricRequestBody(currentMetrics)
        apiClient.sendMetrics(metricRequestBody) { response ->
            if (response != null && response.isSuccessful) {
                retryCount = 0
            } else {
                handleMetricSendFailure(currentMetrics)
            }
        }
    }

    private fun handleMetricSendFailure(failedMetrics: List<Metric>) {
        if (retryCount < maxRetries) {
            retryCount++
            synchronized(metricList) {
                metricList.addAll(0, failedMetrics)
            }
        } else {
            retryCount = 0
        }
    }

    private fun createMetricRequestBody(metrics: List<Metric>): MetricRequestBody {
        return MetricRequestBody(
            metricNamespace = METRIC_NAMESPACE,
            metricList = metrics
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
        if (_isCsmDisabled) return

        val currentTime = MetricsUtils.getCurrentMetricTimestamp()
        val countMetricDimensions = getCountMetricDimensions()
        val countMetric = Metric(
            dimensions = countMetricDimensions,
            metricName = metricName.name,
            namespace = METRIC_NAMESPACE,
            optionalDimensions = emptyList(),
            timestamp = currentTime,
            unit = "Count",
            value = 1
        )

        synchronized(metricList) {
            metricList.add(countMetric)
        }

        if (!isMonitoring) {
            startMonitoring()
        }
    }
}
