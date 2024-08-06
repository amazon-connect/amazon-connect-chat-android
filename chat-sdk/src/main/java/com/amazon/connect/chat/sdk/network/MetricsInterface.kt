package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.model.ConnectionDetails
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

@ApiUrl("https://api.aws.example.com/")
interface MetricsInterface {

    @GET("someAwsEndpoint")
    fun getAwsData(@Query("param") param: String): Call<ConnectionDetails>
}