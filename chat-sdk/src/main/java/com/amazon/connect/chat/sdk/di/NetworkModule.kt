package com.amazon.connect.chat.sdk.di

import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.ApiUrl
import com.amazon.connect.chat.sdk.network.MetricsInterface
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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(okHttpClient: OkHttpClient): Retrofit.Builder {
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
    }

    @Provides
    @Singleton
    fun provideMetricsInterface(retrofitBuilder: Retrofit.Builder): MetricsInterface {
        return createService(MetricsInterface::class.java, retrofitBuilder)
    }

//    @Provides
//    @Singleton
//    fun provideUploadInterface(retrofitBuilder: Retrofit.Builder): UploadInterface {
//        return createService(UploadInterface::class.java, retrofitBuilder)
//    }

    @Provides
    @Singleton
    fun provideAPIClient(
        metricsInterface: MetricsInterface
    ): APIClient {
        return APIClient(metricsInterface)
    }

    @Provides
    @Singleton
    fun provideAWSClient(): AWSClient {
        return AWSClient()
    }

    private fun <T> createService(clazz: Class<T>, retrofitBuilder: Retrofit.Builder): T {
        // Check if the service has an annotation
        val apiUrlAnnotation = clazz.annotations.find { it is ApiUrl } as ApiUrl?
        // Take the URL value, otherwise use the default
        val url = apiUrlAnnotation?.url ?: defaultApiUrl
        // Create the service using the extracted URL
        return retrofitBuilder.baseUrl(url).build().create(clazz)
    }
}
