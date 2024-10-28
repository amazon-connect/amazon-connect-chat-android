// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.network.api.ApiUrl
import retrofit2.Retrofit

object RetrofitServiceCreator {
    private const val defaultApiUrl = "https://www.example.com/v1/"

    /**
     * Creates a Retrofit service for the specified class.
     *
     * @param T The type of the service.
     * @param clazz The class of the service.
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of the specified service class.
     */
    fun <T> createService(clazz: Class<T>, retrofitBuilder: Retrofit.Builder, url: String? = null): T {
        // Check if the service has an @ApiUrl annotation
        val apiUrlAnnotation = clazz.annotations.find { it is ApiUrl } as ApiUrl?
        // Take the URL value, otherwise use the default
        val apiUrl = url ?: (apiUrlAnnotation?.url ?: defaultApiUrl)
        // Create the service using the extracted URL
        return retrofitBuilder.baseUrl(apiUrl).build().create(clazz)
    }
}
