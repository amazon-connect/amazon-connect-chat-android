package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.MetricRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback

import retrofit2.Response
import java.io.File
import javax.inject.Inject

class APIClient @Inject constructor(
    private val metricsInterface: MetricsInterface,
    private val attachmentsInterface: AttachmentsInterface
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

    fun uploadAttachment(url: String, headerMap: Map<String, String>, inputStream: File, callback: (Response<Unit>?) -> Unit) {
        val requestBody = inputStream.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val call = attachmentsInterface.uploadAttachment(url, requestBody, headerMap)

        call.enqueue(object: Callback<Unit> {
            override fun onResponse(
                call: Call<Unit>,
                response: Response<Unit>
            ) {
                callback(response)
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback(null)
            }
        })
    }
}
