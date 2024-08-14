package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

@ApiUrl("https://api.aws.example.com/")
interface MetricsInterface {

    @POST("put-metrics/")
    fun sendMetrics(@Body metricRequestBody: MetricRequestBody): Call<Any>
}