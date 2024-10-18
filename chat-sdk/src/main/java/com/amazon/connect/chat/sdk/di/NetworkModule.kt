// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.di

import android.content.Context
import com.amazon.connect.chat.sdk.network.api.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.AWSClientImpl
import com.amazon.connect.chat.sdk.network.api.ApiUrl
import com.amazon.connect.chat.sdk.network.api.AttachmentsInterface
import com.amazon.connect.chat.sdk.network.api.MetricsInterface
import com.amazon.connect.chat.sdk.repository.MetricsManager
import com.amazon.connect.chat.sdk.network.NetworkConnectionManager
import com.amazon.connect.chat.sdk.repository.AttachmentsManager
import com.amazon.connect.chat.sdk.repository.MessageReceiptsManager
import com.amazon.connect.chat.sdk.repository.MessageReceiptsManagerImpl
import com.amazon.connect.chat.sdk.utils.MetricsUtils.getMetricsEndpoint
import com.amazonaws.services.connectparticipant.AmazonConnectParticipantClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val defaultApiUrl = "https://www.example.com/v1/"

    /**
     * Provides a singleton instance of OkHttpClient.
     *
     * @return An instance of OkHttpClient.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    /**
     * Provides a singleton instance of Retrofit.Builder.
     *
     * @param okHttpClient The OkHttpClient instance to be used with Retrofit.
     * @return An instance of Retrofit.Builder.
     */
    @Provides
    @Singleton
    fun provideRetrofitBuilder(okHttpClient: OkHttpClient): Retrofit.Builder {
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
    }

    /**
     * Provides a singleton instance of MetricsInterface.
     *
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of MetricsInterface.
     */
    @Provides
    @Singleton
    fun provideMetricsInterface(retrofitBuilder: Retrofit.Builder): MetricsInterface {
        return createService(MetricsInterface::class.java, retrofitBuilder, url=getMetricsEndpoint())
    }

    /**
     * Provides a singleton instance of MetricsInterface.
     *
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of MetricsInterface.
     */
    @Provides
    @Singleton
    fun provideMetricsManager(apiClient: APIClient): MetricsManager {
        return MetricsManager(apiClient = apiClient)
    }

    /**
     * Provides a singleton instance of MetricsInterface.
     *
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of MetricsInterface.
     */
    @Provides
    @Singleton
    fun provideAttachmentsManager(context: Context, awsClient: AWSClient, apiClient: APIClient): AttachmentsManager {
        return AttachmentsManager(context, awsClient, apiClient)
    }

    /**
     * Provides a singleton instance of AttachmentsInterface.
     *
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of MetricsInterface.
     */
    @Provides
    @Singleton
    fun provideAttachmentsInterface(retrofitBuilder: Retrofit.Builder): AttachmentsInterface {
        return createService(AttachmentsInterface::class.java, retrofitBuilder)
    }

    /**
     * Provides a singleton instance of AmazonConnectParticipantClient.
     *
     * @return An instance of AmazonConnectParticipantClient.
     */
    @Provides
    @Singleton
    fun provideAmazonConnectParticipantClient(): AmazonConnectParticipantClient {
        return AmazonConnectParticipantClient()
    }

    /**
     * Provides a singleton instance of AWSClient.
     *
     * @param connectParticipantClient The AmazonConnectParticipantClient instance for AWS SDK calls.
     * @return An instance of AWSClientImpl.
     */
    @Provides
    @Singleton
    fun provideAWSClient(connectParticipantClient: AmazonConnectParticipantClient): AWSClient {
        return AWSClientImpl(connectParticipantClient)
    }

    /**
     * Provides a singleton instance of APIClient.
     *
     * @param metricsInterface The MetricsInterface instance for API operations.
     * @return An instance of APIClient.
     */
    @Provides
    @Singleton
    fun provideAPIClient(metricsInterface: MetricsInterface, attachmentsInterface: AttachmentsInterface): APIClient {
        return APIClient(metricsInterface, attachmentsInterface)
    }

    /**
     * Provides a singleton instance of NetworkConnectionManager.
     *
     * @param context The application context.
     * @return An instance of NetworkConnectionManager.
     */
    @Provides
    @Singleton
    fun provideNetworkConnectionManager(context: Context): NetworkConnectionManager {
        return NetworkConnectionManager.getInstance(context)
    }

    /**
     * Provides a singleton instance of MessageReceiptsManager.
     *
     * @return An instance of MessageReceiptsManager.
     */
    @Provides
    @Singleton
    fun provideMessageReceiptsManager(): MessageReceiptsManager {
        return MessageReceiptsManagerImpl()
    }

    /**
     * Creates a Retrofit service for the specified class.
     *
     * @param T The type of the service.
     * @param clazz The class of the service.
     * @param retrofitBuilder The Retrofit.Builder instance for creating the service.
     * @return An instance of the specified service class.
     */
    private fun <T> createService(clazz: Class<T>, retrofitBuilder: Retrofit.Builder, url: String? = null): T {
        // Check if the service has an annotation
        val apiUrlAnnotation = clazz.annotations.find { it is ApiUrl } as ApiUrl?
        // Take the URL value, otherwise use the default
        val apiUrl = url ?: (apiUrlAnnotation?.url ?: defaultApiUrl)
        // Create the service using the extracted URL
        return retrofitBuilder.baseUrl(apiUrl).build().create(clazz)
    }
}