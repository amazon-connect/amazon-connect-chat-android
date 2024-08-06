package com.amazon.connect.chat.sdk.network


import javax.inject.Inject

class APIClient @Inject constructor(
    private val metricsInterface: MetricsInterface
) {
    fun sendMetrics(){
        // metricsInterface.sendMetrics
    }
}
