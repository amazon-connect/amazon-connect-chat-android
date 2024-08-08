package com.amazon.connect.chat.androidchatexample.di

import android.content.Context
import android.content.SharedPreferences
import com.amazon.connect.chat.sdk.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("ConnectChat", Context.MODE_PRIVATE)
    }

//    // Provide the Context dependency
//    @Provides
//    @Singleton
//    fun provideContext(@ApplicationContext appContext: Context): Context {
//        return appContext
//    }
//
//    @Provides
//    @Singleton
//    fun provideWebSocketManager(
//        context: Context,
//    ): WebSocketManager {
//        return WebSocketManager(context, {})
//    }
}
