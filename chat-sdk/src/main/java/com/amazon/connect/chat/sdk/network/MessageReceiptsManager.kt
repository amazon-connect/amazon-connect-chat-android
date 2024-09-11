package com.amazon.connect.chat.sdk.network

import android.util.Log
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageReceipts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Timer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface MessageReceiptsManager {
    var timer: Timer?
    var throttleTime: Double
    var deliveredThrottleTime: Double
    var shouldSendMessageReceipts: Boolean
    suspend fun throttleAndSendMessageReceipt(
        event: MessageReceiptType,
        messageId: String
    ): Result<PendingMessageReceipts>
    fun invalidateTimer()
    fun handleMessageReceipt(event: MessageReceiptType, messageId: String)
    fun clearPendingMessageReceipts()
}

data class PendingMessageReceipts(
    var deliveredReceiptMessageId: String? = null,
    var readReceiptMessageId: String? = null
) {
    fun clear() {
        Log.d("PendingMessageReceipts", "Clearing pending message receipts.")
        deliveredReceiptMessageId = null
        readReceiptMessageId = null
    }

    fun checkAndRemoveDuplicateReceipt() {
        if (deliveredReceiptMessageId == readReceiptMessageId) {
            Log.d("PendingMessageReceipts", "Duplicate receipt found. Removing delivered receipt for messageId: $deliveredReceiptMessageId")
            deliveredReceiptMessageId = null
        }
    }
}

class MessageReceiptsManagerImpl : MessageReceiptsManager {
    override var timer: Timer? = null
    override var throttleTime: Double = MessageReceipts.defaultReceipts.throttleTime
    override var deliveredThrottleTime: Double = 3.0
    override var shouldSendMessageReceipts: Boolean = true

    private var readReceiptSet = mutableSetOf<String>()
    private var deliveredReceiptSet = mutableSetOf<String>()

    private var pendingMessageReceipts = PendingMessageReceipts()
    private var numPendingDeliveredReceipts: Int = 0

    private var throttleJob: Job? = null  // Job to manage the throttling coroutine

    override suspend fun throttleAndSendMessageReceipt(
        event: MessageReceiptType,
        messageId: String
    ): Result<PendingMessageReceipts> = suspendCancellableCoroutine { continuation ->

        Log.d("MessageReceiptsManager", "Attempting to send message receipt for messageId: $messageId with event: $event")

        if (!shouldSendMessageReceipts) {
            Log.d("MessageReceiptsManager", "Sending message receipts is disabled.")
            continuation.resume(Result.failure(Exception("Sending message receipts is disabled")))
            return@suspendCancellableCoroutine
        }

        handleMessageReceipt(event, messageId)

        // Cancel the previous job if it's still active
        throttleJob?.cancel()
        Log.d("MessageReceiptsManager", "Cancelled existing throttle job.")

        if (pendingMessageReceipts.readReceiptMessageId == null && pendingMessageReceipts.deliveredReceiptMessageId == null && numPendingDeliveredReceipts == 0) {
            return@suspendCancellableCoroutine
        }

        // Launch a new coroutine for throttling
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            delay((throttleTime * 1000).toLong())
            try {
                Log.d("MessageReceiptsManager", "Throttling with interval: ${throttleTime * 1000}ms")
                pendingMessageReceipts.checkAndRemoveDuplicateReceipt()
                Log.d("MessageReceiptsManager", "Resuming continuation with pending receipts.")
                continuation.resume(Result.success(pendingMessageReceipts))
            } catch (e: Exception) {
                Log.e("MessageReceiptsManager", "Error during throttling: ${e.message}", e)
                continuation.resumeWithException(e)
            }
        }
    }

    override fun invalidateTimer() {
        Log.d("MessageReceiptsManager", "Invalidating timer.")
        timer?.cancel()
        timer = null
    }

    override fun handleMessageReceipt(event: MessageReceiptType, messageId: String) {
        Log.d("MessageReceiptsManager", "Handling message receipt for messageId: $messageId with event: $event")

        when (event) {
            MessageReceiptType.MESSAGE_DELIVERED -> {
                if (deliveredReceiptSet.contains(messageId) || readReceiptSet.contains(messageId)) {
                    Log.d("MessageReceiptsManager", "Receipt already handled for messageId: $messageId")
                    return
                }
                deliveredReceiptSet.add(messageId)
                Log.d("MessageReceiptsManager", "Added messageId: $messageId to deliveredReceiptSet")

                CoroutineScope(Dispatchers.Default).launch {
                    Log.d("MessageReceiptsManager", "Scheduling delivery throttle for messageId: $messageId with interval: ${deliveredThrottleTime * 1000}ms")
                    numPendingDeliveredReceipts++
                    delay((deliveredThrottleTime * 1000).toLong())
                    if (readReceiptSet.contains(messageId)) {
                        Log.d("MessageReceiptsManager", "Read receipt already sent for messageId: $messageId")
                    } else {
                        Log.d("MessageReceiptsManager", "Setting delivered receipt to pending for messageId: $messageId")
                        pendingMessageReceipts.deliveredReceiptMessageId = messageId
                    }
                    numPendingDeliveredReceipts--
                }
            }
            MessageReceiptType.MESSAGE_READ -> {
                if (readReceiptSet.contains(messageId)) {
                    Log.d("MessageReceiptsManager", "Read receipt already sent for messageId: $messageId")
                    return
                }
                Log.d("MessageReceiptsManager", "Adding messageId: $messageId to readReceiptSet")
                readReceiptSet.add(messageId)
                pendingMessageReceipts.readReceiptMessageId = messageId
            }
        }
    }

    override fun clearPendingMessageReceipts(): Unit {
        pendingMessageReceipts.clear()
    }
}