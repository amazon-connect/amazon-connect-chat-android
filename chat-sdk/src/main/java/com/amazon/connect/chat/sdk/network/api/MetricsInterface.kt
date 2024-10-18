// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network.api

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

@ApiUrl("https://api.aws.example.com/")
interface MetricsInterface {

    @POST("put-metrics/")
    fun sendMetrics(@Body metricRequestBody: MetricRequestBody): Call<Any>
}