package com.amazon.connect.chat.sdk.model

import kotlinx.serialization.Serializable

@Serializable
enum class MetricName {
    CreateParticipantConnection,
    SendMessage
}

data class Metric(
    val dimensions: List<Dimension>,
    val metricName: String,
    val namespace: String,
    val optionalDimensions: List<Dimension>,
    val timestamp: String,
    val unit: String,
    val value: Int
)

data class Dimension(
    val name: String,
    val value: String
)

data class MetricRequestBody(
    val metricList: List<Metric>,
    val metricNamespace: String
)
