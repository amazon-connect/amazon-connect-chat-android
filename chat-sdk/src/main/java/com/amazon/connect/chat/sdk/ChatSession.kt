// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk

import android.net.Uri
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptData
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.TranscriptResponse
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazonaws.services.connectparticipant.model.ScanDirection
import com.amazonaws.services.connectparticipant.model.SortKey
import com.amazonaws.services.connectparticipant.model.StartPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface ChatSession {
    fun configure(config: GlobalConfig)

    /**
     *  Returns ConnectionDetailsProvider object
     *  @return ConnectionDetailsProvider object that includes connection details.
     */
    fun getConnectionDetailsProvider(): ConnectionDetailsProvider

    /**
     * Connects to a chat session with the specified chat details.
     * @param chatDetails The details of the chat.
     * @return A Result indicating whether the connection was successful.
     */
    suspend fun connect(chatDetails: ChatDetails): Result<Boolean>

    /**
     * Disconnects the current chat session.
     * @return A Result indicating whether the disconnection was successful.
     */
    suspend fun disconnect(): Result<Boolean>


    /**
     * Disconnects the websocket and suspends reconnection attempts.
     * @return A Result indicating whether the suspension was successful.
     */
    suspend fun suspendWebSocketConnection(): Result<Boolean>

    /**
     * Resumes a suspended websocket and attempts to reconnect.
     * @return A Result indicating whether the resume was successful.
     */
    suspend fun resumeWebSocketConnection(): Result<Boolean>

    /**
     * Resets the current state which will disconnect the webSocket and remove all session related data without disconnecting the participant from the chat contact.
     * @return A Result indicating whether the reset was successful.
     */
    suspend fun reset(): Result<Boolean>

    /**
     * Sends a message.
     * @param message The message content.
     * @param contentType The content type of the message.
     * @return A Result indicating whether the message sending was successful.
     */
    suspend fun sendMessage(contentType: ContentType, message: String): Result<Boolean>

    /**
     * Retry a message that failed to be sent.
     * @param messageId The Id of the message that failed to be sent.
     * @return A Result indicating whether the message sending was successful.
     */
    suspend fun resendFailedMessage(messageId: String): Result<Boolean>

    /**
     * Sends an event.
     * @param event The event content.
     * @param contentType The content type of the event.
     * @return A Result indicating whether the event sending was successful.
     */
    suspend fun sendEvent(contentType: ContentType, event: String): Result<Boolean>

    /**
     * Checks if a chat session is currently active.
     * @return True if a chat session is active, false otherwise.
     */
    var onChatSessionStateChanged: ((Boolean) -> Unit)?

    /**
     * Sends an attachment.
     * @return A Result indicating whether sending the attachment was successful.
     */
    suspend fun sendAttachment(fileUri: Uri): Result<Boolean>

    /**
     * Downloads an attachment.
     * @param attachmentId The ID of the attachment to download.
     * @param filename The name of the file to save the attachment as.
     * @return A Result containing the URL of the downloaded attachment.
     */
    suspend fun downloadAttachment(attachmentId: String, filename: String): Result<URL>

    /**
     * Returns the S3 download URL for an attachment.
     * @param attachmentId The ID of the attachment.
     * @return A Result containing the download URL for the attachment.
     */
    suspend fun getAttachmentDownloadUrl(attachmentId: String): Result<URL>

    /**
     * Gets the transcript.
     * @param scanDirection The direction of the scan.
     * @param sortKey The sort key.
     * @param maxResults The maximum number of results.
     * @param nextToken The next token.
     * @param startPosition The start position.
     * @return A Result containing the transcript response.
     */
    suspend fun getTranscript(
        scanDirection: ScanDirection?,
        sortKey: SortKey?,
        maxResults: Int?,
        nextToken: String?,
        startPosition: String?
    ): Result<TranscriptResponse>

    /**
     * Sends a message receipt.
     * @param transcriptItem The transcript item.
     * @param receiptType The type of the receipt.
     */
    suspend fun sendMessageReceipt(transcriptItem: TranscriptItem, receiptType: MessageReceiptType): Result<Boolean>

    var onConnectionEstablished: (() -> Unit)?
    var onConnectionReEstablished: (() -> Unit)?
    var onConnectionBroken: (() -> Unit)?
    var onDeepHeartBeatFailure: (() -> Unit)?
    var onMessageReceived: ((TranscriptItem) -> Unit)?
    var onTranscriptUpdated: ((TranscriptData) -> Unit)?
    var onChatEnded: (() -> Unit)?
    var isChatSessionActive: Boolean
}

@Singleton
class ChatSessionImpl @Inject constructor(private val chatService: ChatService) : ChatSession {

    override var onConnectionEstablished: (() -> Unit)? = null
    override var onConnectionReEstablished: (() -> Unit)? = null
    override var onConnectionBroken: (() -> Unit)? = null
    override var onDeepHeartBeatFailure: (() -> Unit)? = null
    override var onMessageReceived: ((TranscriptItem) -> Unit)? = null
    override var onTranscriptUpdated: ((TranscriptData) -> Unit)? = null
    override var onChatEnded: (() -> Unit)? = null
    override var onChatSessionStateChanged: ((Boolean) -> Unit)? = null
    override var isChatSessionActive: Boolean = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var eventCollectionJob: Job? = null
    private var transcriptCollectionJob: Job? = null
    private var transcriptListCollectionJob: Job? = null
    private var chatSessionStateCollectionJob: Job? = null

    private fun setupEventSubscriptions() {
        // Cancel any existing subscriptions before setting up new ones
        cleanup()

        // Set up new subscriptions
        eventCollectionJob = coroutineScope.launch {
            chatService.eventPublisher.collect { event ->
                when (event) {
                    ChatEvent.ConnectionEstablished -> onConnectionEstablished?.invoke()
                    ChatEvent.ConnectionReEstablished -> onConnectionReEstablished?.invoke()
                    ChatEvent.ChatEnded -> {
                        onChatEnded?.invoke()
                        coroutineScope.launch {
                            // Provide some time for transcript to update and surface end event to user.
                            delay(500)
                            cleanup()
                        }
                    }
                    ChatEvent.ConnectionBroken -> onConnectionBroken?.invoke()
                    ChatEvent.DeepHeartBeatFailure -> onDeepHeartBeatFailure?.invoke()
                }
            }
        }

        transcriptCollectionJob = coroutineScope.launch {
            chatService.transcriptPublisher.collect { transcriptItem ->
                // Make sure it runs on main thread
                coroutineScope.launch {
                    onMessageReceived?.invoke(transcriptItem)
                }
            }
        }

        transcriptListCollectionJob = coroutineScope.launch {
            chatService.transcriptListPublisher.collect { transcriptData ->
                if (transcriptData.transcriptList.isNotEmpty()) {
                    // Make sure it runs on main thread
                    coroutineScope.launch {
                        onTranscriptUpdated?.invoke(transcriptData)
                    }
                }
            }
        }

        chatSessionStateCollectionJob = coroutineScope.launch {
            chatService.chatSessionStatePublisher.collect { isActive ->
                isChatSessionActive = isActive
                onChatSessionStateChanged?.invoke(isActive)
            }
        }
    }

    override fun configure(config: GlobalConfig) {
        chatService.configure(config)
    }

    override fun getConnectionDetailsProvider(): ConnectionDetailsProvider {
        return chatService.getConnectionDetailsProvider()
    }

    override suspend fun connect(chatDetails: ChatDetails): Result<Boolean> {
        // Establish subscriptions whenever a new chat session is initiated
        setupEventSubscriptions()
        return withContext(Dispatchers.IO) {
            chatService.createChatSession(chatDetails)
        }
    }

    override suspend fun disconnect(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.disconnectChatSession()
        }
    }

    override suspend fun suspendWebSocketConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.suspendWebSocketConnection()
        }
    }

    override suspend fun resumeWebSocketConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.resumeWebSocketConnection()
        }
    }

    override suspend fun reset(): Result<Boolean> {
        cleanup()
        isChatSessionActive = false
        return withContext(Dispatchers.IO) {
            chatService.reset()
        }
    }

    override suspend fun sendMessage(contentType: ContentType, message: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.sendMessage(contentType, message)
        }
    }

    override suspend fun resendFailedMessage(messageId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.resendFailedMessage(messageId)
        }
    }

    override suspend fun sendEvent(contentType: ContentType, content: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.sendEvent(contentType, content)
        }
    }


    override suspend fun sendAttachment(fileUri: Uri): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            chatService.sendAttachment(fileUri)
        }
    }


    override suspend fun downloadAttachment(attachmentId: String, filename: String): Result<URL> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.downloadAttachment(attachmentId, filename).getOrThrow()
            }
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: String): Result<URL> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.getAttachmentDownloadUrl(attachmentId).getOrThrow()
            }
        }
    }

    override suspend fun getTranscript(
        scanDirection: ScanDirection?,
        sortKey: SortKey?,
        maxResults: Int?,
        nextToken: String?,
        startPosition: String?
    ): Result<TranscriptResponse> {
        return withContext(Dispatchers.IO){
            runCatching {
                // Construct the start position if provided
                val awsStartPosition = if (startPosition != null) {
                    StartPosition().withId(startPosition)
                } else {
                    null
                }
                chatService.getTranscript(scanDirection, sortKey, maxResults, nextToken, awsStartPosition)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }

    override suspend fun sendMessageReceipt(transcriptItem: TranscriptItem, receiptType: MessageReceiptType): Result<Boolean> {
        return withContext(Dispatchers.IO) {
                val messageItem = transcriptItem as? Message

                // Check if the transcript item is a plain text message, is not empty, and is incoming
                if (messageItem == null || messageItem.text.isEmpty() || messageItem.participant == "CUSTOMER") {
                    return@withContext Result.failure<Boolean>(IllegalArgumentException("Invalid message item.  Cannot send message receipts for outgoing or empty messages."))
                }

                // Check if the item already has the read status when sending a read receipt
                if (receiptType == MessageReceiptType.MESSAGE_READ && messageItem.metadata?.status == MessageStatus.Read) {
                    return@withContext Result.success(true)
                }

                val sendReceiptResult = sendReceipt(event = receiptType, messageId = messageItem.id)
                if (sendReceiptResult.isSuccess) {
                    Result.success(true)
                } else {
                    Result.failure(sendReceiptResult.exceptionOrNull() ?: IllegalStateException("sendReceipt call failed"))
                }
        }
    }

    private suspend fun sendReceipt(event: MessageReceiptType, messageId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.sendMessageReceipt(event, messageId)
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }

    private fun cleanup() {
        // Cancel flow collection jobs when disconnecting or cleaning up
        eventCollectionJob?.cancel()
        transcriptCollectionJob?.cancel()
        transcriptListCollectionJob?.cancel()
        chatSessionStateCollectionJob?.cancel()

        eventCollectionJob = null
        transcriptCollectionJob = null
        transcriptListCollectionJob = null
        chatSessionStateCollectionJob = null

        // Reset active state
        isChatSessionActive = false
        onChatSessionStateChanged?.invoke(false)
    }
}