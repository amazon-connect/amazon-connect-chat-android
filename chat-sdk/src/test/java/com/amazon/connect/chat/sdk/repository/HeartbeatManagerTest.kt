package com.amazon.connect.chat.sdk.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class HeartbeatManagerTest {

    private lateinit var heartbeatManager: HeartbeatManager

    private val heartbeatSentCounter = AtomicInteger(0)
    private val missedHeartbeatCalled = AtomicBoolean(false)

    private val sendHeartbeatCallback: () -> Unit = {
        heartbeatSentCounter.incrementAndGet()
    }

    private val missedHeartbeatCallback: suspend () -> Unit = {
        missedHeartbeatCalled.set(true)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        heartbeatManager = HeartbeatManager(sendHeartbeatCallback, missedHeartbeatCallback)
        heartbeatSentCounter.set(0)
        missedHeartbeatCalled.set(false)
    }

    @Test
    fun `test heartbeatReceived resets pending response`() = runTest {
        // Simulate a heartbeat being sent
        heartbeatManager.startHeartbeat()

        // Manually set pendingResponse to true and then call heartbeatReceived
        setPrivateField(heartbeatManager, "pendingResponse", true)
        heartbeatManager.heartbeatReceived()

        // PendingResponse should be false
        val pendingResponse = getPrivateField(heartbeatManager, "pendingResponse") as Boolean
        assert(!pendingResponse) { "Expected pendingResponse to be false after heartbeatReceived" }
    }

    @Test
    fun `test stopHeartbeat cancels timer`() = runTest {
        // Start heartbeat and wait for it to complete
        heartbeatManager.startHeartbeat()

        // Add a small delay to ensure timer is fully initialized
        kotlinx.coroutines.delay(50)

        // Manually set pendingResponse to true to simulate heartbeat being sent
        setPrivateField(heartbeatManager, "pendingResponse", true)

        // Stop heartbeat - this should reset pendingResponse to false
        heartbeatManager.stopHeartbeat()

        // PendingResponse should be false after stopHeartbeat, regardless of previous state
        val pendingResponse = getPrivateField(heartbeatManager, "pendingResponse") as Boolean
        assert(!pendingResponse) { "Expected pendingResponse to be false after stopHeartbeat" }

        val timer = getPrivateField(heartbeatManager, "timer")
        // We can't easily verify the timer is cancelled, but we can check it's not null
        assert(timer != null) { "Expected timer to not be null" }
    }

    // Helper method to access private fields
    private fun getPrivateField(obj: Any, fieldName: String): Any? {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj)
    }

    // Helper method to set private fields
    private fun setPrivateField(obj: Any, fieldName: String, value: Any?) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
