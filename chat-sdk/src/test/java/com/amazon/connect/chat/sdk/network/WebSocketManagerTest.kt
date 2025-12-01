package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
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

        // Mock global chat session state (replaces local isChatActive)
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(false)
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
    fun `resumeWebSocketConnection sets isChatSuspended to false regardless of chat session state`() {
        // Mock chat session as active
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(true)

        setPrivateField(webSocketManager, "isChatSuspended", true)
        webSocketManager.resumeWebSocketConnection()
        val isChatSuspended = getPrivateField(webSocketManager, "isChatSuspended") as Boolean
        assertFalse(isChatSuspended)
    }

    @Test
    fun `reestablishConnectionIfChatActive uses global chat session state`() = runTest {
        // Set up conditions for reconnection attempt
        setPrivateField(webSocketManager, "isConnectedToNetwork", true)
        setPrivateField(webSocketManager, "isChatSuspended", false)
        setPrivateField(webSocketManager, "_isReconnecting", MutableStateFlow(false))

        // Test: Chat session inactive - should not attempt reconnection
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(false)

        // Use reflection to call private method
        val method = webSocketManager.javaClass.getDeclaredMethod("reestablishConnectionIfChatActive")
        method.isAccessible = true
        method.invoke(webSocketManager)

        // Verify reconnection state wasn't changed (no reconnection attempt)
        val isReconnecting = getPrivateField(webSocketManager, "_isReconnecting") as MutableStateFlow<*>
        assertFalse(isReconnecting.value as Boolean)
    }

    @Test
    fun `reestablishConnectionIfChatActive attempts reconnection when chat session is active`() = runTest {
        // Set up conditions for successful reconnection attempt
        setPrivateField(webSocketManager, "isConnectedToNetwork", true)
        setPrivateField(webSocketManager, "isChatSuspended", false)
        setPrivateField(webSocketManager, "_isReconnecting", MutableStateFlow(false))

        // Test: Chat session active - should attempt reconnection
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(true)

        // Use reflection to call private method
        val method = webSocketManager.javaClass.getDeclaredMethod("reestablishConnectionIfChatActive")
        method.isAccessible = true
        method.invoke(webSocketManager)

        // Verify reconnection was initiated
        val isReconnecting = getPrivateField(webSocketManager, "_isReconnecting") as MutableStateFlow<*>
        assertTrue(isReconnecting.value as Boolean)
    }

    @Test
    fun `reestablishConnectionIfChatActive respects network connectivity`() = runTest {
        // Set up conditions - chat active but no network
        setPrivateField(webSocketManager, "isConnectedToNetwork", false)
        setPrivateField(webSocketManager, "isChatSuspended", false)
        setPrivateField(webSocketManager, "_isReconnecting", MutableStateFlow(false))

        // Chat session is active but no network
        Mockito.`when`(mockConnectionDetailsProvider.isChatSessionActive()).thenReturn(true)

        // Use reflection to call private method
        val method = webSocketManager.javaClass.getDeclaredMethod("reestablishConnectionIfChatActive")
        method.isAccessible = true
        method.invoke(webSocketManager)

        // Verify no reconnection attempt due to missing network
        val isReconnecting = getPrivateField(webSocketManager, "_isReconnecting") as MutableStateFlow<*>
        assertFalse(isReconnecting.value as Boolean)
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
                SDKLogger.logger.logDebug { "Error setting fallback field $fieldName: ${ex.message}" }
            }
        } catch (e: Exception) {
            SDKLogger.logger.logDebug { "Error setting field $fieldName: ${e.message}" }
        }
    }

    private fun getPrivateField(obj: Any, fieldName: String): Any? {
        return try {
            val field = findField(obj.javaClass, fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            SDKLogger.logger.logDebug { "Error getting field $fieldName: ${e.message}" }
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