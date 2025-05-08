package com.amazon.connect.chat.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class NetworkConnectionManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockConnectivityManager: ConnectivityManager

    @Mock
    private lateinit var mockNetwork: Network

    @Mock
    private lateinit var mockNetworkCapabilities: NetworkCapabilities

    @Captor
    private lateinit var networkCallbackCaptor: ArgumentCaptor<ConnectivityManager.NetworkCallback>

    private lateinit var networkConnectionManager: NetworkConnectionManager
    private lateinit var capturedCallback: ConnectivityManager.NetworkCallback

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mockConnectivityManager)
        networkConnectionManager = NetworkConnectionManager(mockContext)

        // Register and capture the callback in setup
        networkConnectionManager.registerNetworkCallback()
        verify(mockConnectivityManager).registerNetworkCallback(
            org.mockito.ArgumentMatchers.any(NetworkRequest::class.java),
            networkCallbackCaptor.capture()
        )
        capturedCallback = networkCallbackCaptor.value
    }

    @Test
    fun `test initial network state is false`() = runTest {
        // When initialized, network state should be false
        assertFalse(networkConnectionManager.isNetworkAvailable.first())
    }

    @Test
    fun `test registerNetworkCallback registers with correct request`() {
        // This is already verified in setUp()
        // Just verify that we have a callback
        assertTrue(capturedCallback is ConnectivityManager.NetworkCallback)
    }

    @Test
    fun `test onAvailable sets network state to true`() = runTest {
        capturedCallback.onAvailable(mockNetwork)

        assertTrue(networkConnectionManager.isNetworkAvailable.first())
    }

    @Test
    fun `test onLost sets network state to false`() = runTest {
        // First set it to true
        capturedCallback.onAvailable(mockNetwork)
        assertTrue(networkConnectionManager.isNetworkAvailable.first())

        capturedCallback.onLost(mockNetwork)

        assertFalse(networkConnectionManager.isNetworkAvailable.first())
    }

    @Test
    fun `test onCapabilitiesChanged with internet capability sets state to true`() = runTest {
        `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)

        capturedCallback.onCapabilitiesChanged(mockNetwork, mockNetworkCapabilities)

        assertTrue(networkConnectionManager.isNetworkAvailable.first())
    }

    @Test
    fun `test onCapabilitiesChanged without internet capability sets state to false`() = runTest {
        `when`(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(false)

        capturedCallback.onCapabilitiesChanged(mockNetwork, mockNetworkCapabilities)

        assertFalse(networkConnectionManager.isNetworkAvailable.first())
    }

    @Test
    fun `test getInstance returns singleton instance`() {
        val instance1 = NetworkConnectionManager.getInstance(mockContext)

        val instance2 = NetworkConnectionManager.getInstance(mockContext)

        assertSame(instance1, instance2)
    }

    @Test
    fun `test getInstance creates new instance when none exists`() {
        // Access the INSTANCE field via reflection to set it to null
        val field = NetworkConnectionManager::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        val instance = NetworkConnectionManager.getInstance(mockContext)

        assertTrue(instance is NetworkConnectionManager)
    }
}
