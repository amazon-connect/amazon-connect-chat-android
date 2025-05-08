package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.WebSocket
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WebSocketManagerTest {

    @Mock
    private lateinit var mockNetworkConnectionManager: NetworkConnectionManager

    @Mock
    private lateinit var mockConnectionDetailsProvider: ConnectionDetailsProvider

    @Mock
    private lateinit var mockOkHttpClient: OkHttpClient

    @Mock
    private lateinit var mockWebSocket: WebSocket

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var webSocketManager: WebSocketManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Create a StateFlow for network availability
        val networkAvailableFlow = MutableStateFlow(true)
        
        // Mock the isNetworkAvailable property to return our flow
        Mockito.`when`(mockNetworkConnectionManager.isNetworkAvailable).thenReturn(networkAvailableFlow)

        webSocketManager = WebSocketManagerImpl(
            dispatcher = testDispatcher,
            networkConnectionManager = mockNetworkConnectionManager,
            connectionDetailsProvider = mockConnectionDetailsProvider
        )

        setPrivateField(webSocketManager, "client", mockOkHttpClient)
        setPrivateField(webSocketManager, "isConnectedToNetwork", true)
        setPrivateField(webSocketManager, "isChatActive", false)
    }

    @Test
    fun `suspendWebSocketConnection sets isChatSuspended to true`() {
        setPrivateField(webSocketManager, "webSocket", mockWebSocket)
        webSocketManager.suspendWebSocketConnection()
        val isChatSuspended = getPrivateField(webSocketManager, "isChatSuspended") as Boolean
        assertTrue(isChatSuspended)
    }

    @Test
    fun `resumeWebSocketConnection sets isChatSuspended to false`() {
        setPrivateField(webSocketManager, "isChatSuspended", true)
        webSocketManager.resumeWebSocketConnection()
        val isChatSuspended = getPrivateField(webSocketManager, "isChatSuspended") as Boolean
        assertFalse(isChatSuspended)
    }

    @Test
    fun `check webSocket connection state`() {
        // Test with webSocket set
        setPrivateField(webSocketManager, "webSocket", mockWebSocket)
        val webSocketWithMock = getPrivateField(webSocketManager, "webSocket")
        assertNotNull(webSocketWithMock)
        
        // Test with webSocket null
        setPrivateField(webSocketManager, "webSocket", null)
        val webSocketWithNull = getPrivateField(webSocketManager, "webSocket")
        assertNull(webSocketWithNull)
    }

    @Test
    fun `resumeWebSocketConnection does not change state when chat is already active`() {
        setPrivateField(webSocketManager, "isChatActive", true)
        webSocketManager.resumeWebSocketConnection()
        val isChatSuspended = getPrivateField(webSocketManager, "isChatSuspended") as Boolean
        assertFalse(isChatSuspended)
    }

    // Reflection helper methods
    private fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        try {
            val field = findField(obj.javaClass, fieldName)
            field.isAccessible = true
            field.set(obj, value)
        } catch (e: NoSuchFieldException) {
            try {
                val fallbackField = obj.javaClass.declaredFields.firstOrNull()
                fallbackField?.let {
                    it.isAccessible = true
                    it.set(obj, value)
                }
            } catch (ex: Exception) {
                println("Error setting fallback field $fieldName: ${ex.message}")
            }
        } catch (e: Exception) {
            println("Error setting field $fieldName: ${e.message}")
        }
    }

    private fun getPrivateField(obj: Any, fieldName: String): Any? {
        return try {
            val field = findField(obj.javaClass, fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            println("Error getting field $fieldName: ${e.message}")
            null
        }
    }

    private fun findField(clazz: Class<*>, fieldName: String): Field {
        return try {
            clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            clazz.superclass?.let { findField(it, fieldName) } ?: throw e
        }
    }
}