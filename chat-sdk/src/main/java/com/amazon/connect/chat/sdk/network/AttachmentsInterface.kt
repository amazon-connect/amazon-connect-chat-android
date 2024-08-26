package com.amazon.connect.chat.sdk.network

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.Url

interface AttachmentsInterface {

    @PUT
    fun uploadAttachment(@Url url: String, @Body file: RequestBody, @HeaderMap headers: Map<String, String>): Call<Unit>
}