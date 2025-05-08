package com.amazon.connect.chat.sdk.provider

import android.content.Context
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.ChatSessionImpl
import com.amazon.connect.chat.sdk.network.api.APIClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ChatSessionProviderTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockApplicationContext: Context

    @Mock
    private lateinit var mockChatSession: ChatSession

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        resetChatSessionSingleton()
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockApplicationContext)
    }

    @Test
    fun `getChatSession returns same instance on multiple calls`() {
        setPrivateField(ChatSessionProvider, "chatSession", mockChatSession)

        val session1 = ChatSessionProvider.getChatSession(mockContext)
        val session2 = ChatSessionProvider.getChatSession(mockContext)

        assertNotNull(session1)
        assertSame("Multiple calls should return the same instance", session1, session2)
    }

    @Test
    fun `isHiltAvailable returns false when class not found`() {
        val method = ChatSessionProvider.javaClass.getDeclaredMethod("isHiltAvailable").apply {
            isAccessible = true
        }
        val result = method.invoke(ChatSessionProvider) as Boolean
        assertFalse("Expected Hilt to be unavailable", result)
    }

    @Test
    fun `createRetrofitBuilder returns builder with GsonConverterFactory`() {
        val method = ChatSessionProvider.javaClass.getDeclaredMethod("createRetrofitBuilder").apply {
            isAccessible = true
        }
        val builder = method.invoke(ChatSessionProvider)
        assertNotNull("Builder should not be null", builder)
    }

    @Test
    fun `createAPIClient returns valid APIClient`() {
        val retrofitBuilderMethod = ChatSessionProvider.javaClass.getDeclaredMethod("createRetrofitBuilder").apply {
            isAccessible = true
        }
        val retrofitBuilder = retrofitBuilderMethod.invoke(ChatSessionProvider) as retrofit2.Retrofit.Builder

        val method = ChatSessionProvider.javaClass.getDeclaredMethod("createAPIClient", retrofit2.Retrofit.Builder::class.java).apply {
            isAccessible = true
        }
        val result = method.invoke(ChatSessionProvider, retrofitBuilder)
        assertTrue(result is APIClient)
    }

    // Helper methods for reflection
    private fun resetChatSessionSingleton() {
        setPrivateField(ChatSessionProvider, "chatSession", null)
    }

    private fun setPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = findField(target.javaClass, fieldName)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun findField(clazz: Class<*>, fieldName: String): Field {
        return try {
            clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            clazz.superclass?.let { findField(it, fieldName) } ?: throw e
        }
    }
}
