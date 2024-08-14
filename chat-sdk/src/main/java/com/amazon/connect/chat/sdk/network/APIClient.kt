package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import retrofit2.Call
import retrofit2.Callback

import retrofit2.Response
import javax.inject.Inject

class APIClient @Inject constructor(
    private val metricsInterface: MetricsInterface
) {
    fun sendMetrics(metricRequestBody: MetricRequestBody, callback: (Response<Any>?) -> Unit) {
        val call = metricsInterface.sendMetrics(metricRequestBody)

        call.enqueue(object: Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                callback(response)
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                callback(null)
            }
        })
    }
}
