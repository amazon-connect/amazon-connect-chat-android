package com.amazon.connect.chat.sdk.network

import android.util.Log
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageReceipts
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
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
        deliveredReceiptMessageId = null
        readReceiptMessageId = null
    }

    fun checkAndRemoveDuplicateReceipt() {
        if (deliveredReceiptMessageId == readReceiptMessageId) {
            deliveredReceiptMessageId = null
        }
    }
}

class MessageReceiptsManagerImpl : MessageReceiptsManager {
    override var timer: Timer? = null
    override var throttleTime: Double = MessageReceipts.defaultReceipts.throttleTime
    override var deliveredThrottleTime: Double = MessageReceipts.defaultReceipts.deliveredThrottleTime
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

        if (!shouldSendMessageReceipts) {
            SDKLogger.logger.logDebug { "Sending message receipts is disabled." }
            continuation.resume(Result.failure(Exception("Sending message receipts is disabled")))
            return@suspendCancellableCoroutine
        }

        handleMessageReceipt(event, messageId)

        // Cancel the previous job if it's still active
        throttleJob?.cancel()

        if (pendingMessageReceipts.readReceiptMessageId == null && pendingMessageReceipts.deliveredReceiptMessageId == null && numPendingDeliveredReceipts == 0) {
            return@suspendCancellableCoroutine
        }

        // Launch a new coroutine for throttling
        throttleJob = CoroutineScope(Dispatchers.Default).launch {
            delay((throttleTime * 1000).toLong())
            try {
                pendingMessageReceipts.checkAndRemoveDuplicateReceipt()
                continuation.resume(Result.success(pendingMessageReceipts))
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    override fun invalidateTimer() {
        timer?.cancel()
        timer = null
    }

    override fun handleMessageReceipt(event: MessageReceiptType, messageId: String) {

        when (event) {
            MessageReceiptType.MESSAGE_DELIVERED -> {
                if (deliveredReceiptSet.contains(messageId) || readReceiptSet.contains(messageId)) {
                    return
                }
                deliveredReceiptSet.add(messageId)

                CoroutineScope(Dispatchers.Default).launch {
                    numPendingDeliveredReceipts++
                    delay((deliveredThrottleTime * 1000).toLong())
                    if (!readReceiptSet.contains(messageId)) {
                        pendingMessageReceipts.deliveredReceiptMessageId = messageId
                    }
                    numPendingDeliveredReceipts--
                }
            }
            MessageReceiptType.MESSAGE_READ -> {
                if (readReceiptSet.contains(messageId)) {
                    return
                }
                readReceiptSet.add(messageId)
                pendingMessageReceipts.readReceiptMessageId = messageId
            }
        }
    }

    override fun clearPendingMessageReceipts(): Unit {
        pendingMessageReceipts.clear()
    }
}