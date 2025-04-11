// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.repository

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.TranscriptResponse
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.getMimeType
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.getOriginalFileName
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazon.connect.chat.sdk.utils.TranscriptItemUtils
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import com.amazonaws.services.connectparticipant.model.GetTranscriptRequest
import com.amazonaws.services.connectparticipant.model.ScanDirection
import com.amazonaws.services.connectparticipant.model.SortKey
import com.amazonaws.services.connectparticipant.model.StartPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Timer
import java.util.UUID
import javax.inject.Inject
import kotlin.concurrent.schedule

interface ChatService {

    fun configure(config: GlobalConfig)

    fun getConnectionDetailsProvider(): ConnectionDetailsProvider

    /**
     * Creates a chat session with the specified chat details.
     * @param chatDetails The details of the chat.
     * @return A Result indicating whether the session creation was successful.
     */
    suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean>

    /**
     * Disconnects the current chat session.
     * @return A Result indicating whether the disconnection was successful.
     */
    suspend fun disconnectChatSession(): Result<Boolean>

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
     * @param contentType The content type of the message.
     * @param message The message content.
     * @return A Result indicating whether the message sending was successful.
     */
    suspend fun sendMessage(contentType: ContentType, message: String): Result<Boolean>

    /**
     * Retry a text message or attachment that failed to be sent.
     * @param messageId The Id of the message that failed to be sent.
     * @return A Result indicating whether the message sending was successful.
     */
    suspend fun resendFailedMessage(messageId: String): Result<Boolean>

    /**
     * Sends an event.
     * @param contentType The content type of the message.
     * @param event The event content.
     * @return A Result indicating whether the event sending was successful.
     */
    suspend fun sendEvent(contentType: ContentType, event: String): Result<Boolean>

    /**
     * Sends an attachment.
     * @return A Result indicating whether sending the attachment was successful.
     */
    suspend fun sendAttachment(fileUri: Uri): Result<Boolean>

    /**
     * Downloads an attachment.
     * @param attachmentId The ID of the attachment.
     * @param fileName The name of the file.
     * @return A Result containing the URI of the downloaded attachment.
     */
    suspend fun downloadAttachment(attachmentId: String, fileName: String): Result<URL>


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
        startPosition: StartPosition?
    ): Result<TranscriptResponse>

    /**
     * Sends a message receipt.
     * @param messageReceiptType The type of the message receipt.
     * @param messageId The ID of the message.
     * @return A Result indicating whether the message receipt sending was successful.
     */
    suspend fun sendMessageReceipt(messageReceiptType: MessageReceiptType, messageId: String) : Result<Unit>

    val eventPublisher: SharedFlow<ChatEvent>
    val transcriptPublisher: SharedFlow<TranscriptItem>
    val transcriptListPublisher: SharedFlow<List<TranscriptItem>>
    val chatSessionStatePublisher: SharedFlow<Boolean>
}

class ChatServiceImpl @Inject constructor(
    private val context: Context,
    private val awsClient: AWSClient,
    private val connectionDetailsProvider: ConnectionDetailsProvider,
    private val webSocketManager: WebSocketManager,
    private val metricsManager: MetricsManager,
    private val attachmentsManager: AttachmentsManager,
    private val messageReceiptsManager: MessageReceiptsManager
) : ChatService, DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _eventPublisher = MutableSharedFlow<ChatEvent>()
    override val eventPublisher: SharedFlow<ChatEvent> get() = _eventPublisher
    private var eventCollectionJob: Job? = null

    private val _transcriptPublisher = MutableSharedFlow<TranscriptItem>()
    override val transcriptPublisher: SharedFlow<TranscriptItem> get() = _transcriptPublisher
    private var transcriptCollectionJob: Job? = null

    private val _transcriptListPublisher = MutableSharedFlow<List<TranscriptItem>>()
    override val transcriptListPublisher: SharedFlow<List<TranscriptItem>> get() = _transcriptListPublisher

    private val _chatSessionStatePublisher = MutableSharedFlow<Boolean>()
    override val chatSessionStatePublisher: SharedFlow<Boolean> get() = _chatSessionStatePublisher
    private var chatSessionStateCollectionJob: Job? = null

    @VisibleForTesting
    private var transcriptDict = mutableMapOf<String, TranscriptItem>()
    @VisibleForTesting
    internal var internalTranscript = mutableListOf<TranscriptItem>()

    private var typingIndicatorTimer: Timer? = null
    private var throttleTypingEventTimer: Timer? = null
    private var throttleTypingEvent: Boolean = false

    // Dictionary to map attachment IDs to temporary message IDs
    private val attachmentIdToTempMessageId = mutableMapOf<String, String>()

    // Dictionary to map temporary attachment message IDs to file urls
    private val tempMessageIdToFileUrl = mutableMapOf<String, Uri>()

    override fun configure(config: GlobalConfig) {
        awsClient.configure(config)
        metricsManager.configure(config)
    }

    override fun getConnectionDetailsProvider(): ConnectionDetailsProvider {
        return connectionDetailsProvider
    }

    init {
        registerNotificationListeners()
    }

    override suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean> {
        setupEventSubscriptions()
        return runCatching {
            connectionDetailsProvider.updateChatDetails(chatDetails)
            val connectionDetails =
                awsClient.createParticipantConnection(chatDetails.participantToken).getOrThrow()
            metricsManager.addCountMetric(MetricName.CreateParticipantConnection)
            connectionDetailsProvider.updateConnectionDetails(connectionDetails)
            setupWebSocket(connectionDetails.websocketUrl)
            SDKLogger.logger.logDebug { "Participant Connected" }
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to create chat session: ${exception.message}" }
        }
    }

    private suspend fun setupWebSocket(url: String, isReconnectFlow: Boolean = false) {
        webSocketManager.connect(
            url,
            isReconnectFlow
        )
    }

    private fun setupEventSubscriptions() {
        clearSubscriptionsAndPublishers()

        eventCollectionJob = coroutineScope.launch {
            webSocketManager.eventPublisher.collect { event ->
                when (event) {
                    ChatEvent.ConnectionEstablished -> {
                        connectionDetailsProvider.setChatSessionState(true)
                        SDKLogger.logger.logDebug { "Connection Established" }
                        getTranscript(startPosition = null,
                            scanDirection = ScanDirection.BACKWARD,
                            sortKey = SortKey.ASCENDING,
                            maxResults = 30,
                            nextToken = null)
                    }

                    ChatEvent.ConnectionReEstablished -> {
                        SDKLogger.logger.logDebug { "Connection Re-Established" }
                        connectionDetailsProvider.setChatSessionState(true)
                        removeTypingIndicators()  // Make sure to remove typing indicators if still present
                        fetchReconnectedTranscript()
                    }

                    ChatEvent.ChatEnded -> {
                        SDKLogger.logger.logDebug { "Chat Ended" }
                        connectionDetailsProvider.setChatSessionState(false)
                    }

                    ChatEvent.ConnectionBroken -> SDKLogger.logger.logDebug { "Connection Broken" }
                    ChatEvent.DeepHeartBeatFailure -> {
                        SDKLogger.logger.logDebug { "Deep Heartbeat Failure" }
                    }
                }
                _eventPublisher.emit(event)
            }
        }

        transcriptCollectionJob = coroutineScope.launch {
            webSocketManager.transcriptPublisher.collect { transcriptItem ->
                updateTranscriptDict(transcriptItem)
            }
        }

        chatSessionStateCollectionJob = coroutineScope.launch {
            connectionDetailsProvider.chatSessionState.collect { isActive ->
                _chatSessionStatePublisher.emit(isActive)
            }
        }
    }

    private fun updateTranscriptDict(item: TranscriptItem) {
        when (item) {
            is MessageMetadata -> {
                // Associate metadata with message based on its ID
                val messageItem = transcriptDict[item.id] as? Message
                messageItem?.let {
                    it.metadata = item
                    transcriptDict[item.id] = it
                }
            }
            is Message -> {
                // Remove typing indicators when a new message from the agent is received
                if (item.participant == Constants.AGENT) {
                    removeTypingIndicators()
                    coroutineScope.launch {
                        sendMessageReceipt(MessageReceiptType.MESSAGE_DELIVERED, item.id)
                    }
                }

                val tempMessageId = attachmentIdToTempMessageId[item.attachmentId]
                if (tempMessageId != null) {
                    val tempMessage = transcriptDict[tempMessageId] as? Message
                    if (tempMessage != null) {
                        updateTemporaryMessageForAttachments(tempMessage, item, transcriptDict)
                    }
                    attachmentIdToTempMessageId.remove(item.attachmentId)
                }else {
                    transcriptDict[item.id] = item
                }
            }
            is Event -> {
                handleEvent(item, transcriptDict)
            }
        }

        transcriptDict[item.id]?.let {
            handleTranscriptItemUpdate(it)
        }

    }

    private fun removeTypingIndicators() {
        typingIndicatorTimer?.cancel()

        val initialCount = transcriptDict.size

        // Remove typing indicators from both transcriptDict and internalTranscript
        transcriptDict.entries.removeIf {
            it.value is Event && it.value.contentType == ContentType.TYPING.type
        }

        internalTranscript.removeIf {
            it is Event && it.contentType == ContentType.TYPING.type
        }

        // Send the updated transcript list to subscribers if items removed
        if (transcriptDict.size != initialCount) {
            coroutineScope.launch {
                _transcriptListPublisher.emit(internalTranscript)
            }
        }
    }


    private fun resetTypingIndicatorTimer(after: Double = 0.0) {
        typingIndicatorTimer?.cancel()
        typingIndicatorTimer = Timer().apply {
            schedule(after.toLong() * 1000) {
                if (isAppInForeground()) {
                    removeTypingIndicators()
                }
            }
        }
    }

    private fun handleEvent(event: Event, currentDict: MutableMap<String, TranscriptItem>) {
        if (event.contentType == ContentType.TYPING.type) {
            resetTypingIndicatorTimer(12.0)
        }
        currentDict[event.id] = event
    }


    private fun handleTranscriptItemUpdate(item: TranscriptItem) {
        // Send out the individual transcript item to subscribers
        coroutineScope.launch {
            _transcriptPublisher.emit(item)

            // Update the internal transcript list with the new or updated item
            val existingIndex = internalTranscript.indexOfFirst { it.id == item.id }
            if (existingIndex != -1) {
                val existingItem = internalTranscript[existingIndex]

                // Reapply the metadata to the new item
                // Whenever new items comes from getTranscript, it comes with null metadata, but we
                // already have that item in our internalTranscript, so we will just apply existing
                // metadata to new item.
                if (existingItem is Message && item is Message) {
                    if (existingItem.metadata != null && item.metadata == null) {
                        item.metadata = existingItem.metadata
                    }
                }

                // If the item already exists in the internal transcript list, update it
                internalTranscript[existingIndex] = item
            } else {
                // If the item is new, determine where to insert it in the list based on its timestamp
                if (internalTranscript.isEmpty() || item.timeStamp < internalTranscript.first().timeStamp) {
                    // If the list is empty or the new item is older than the first item, add it to the beginning
                    internalTranscript.add(0, item)
                } else {
                    // Otherwise, add it to the end of the list
                    internalTranscript.add(item)
                }
            }

            _transcriptListPublisher.emit(internalTranscript)
        }
    }

    private fun sendSingleUpdateToClient(message: Message) {
        transcriptDict[message.id] = message
        handleTranscriptItemUpdate(message)
    }

    private fun updatePlaceholderMessage(oldId: String, newId: String) {
        val placeholderMessage = transcriptDict[oldId] as? Message
        if (placeholderMessage != null) {
            if (transcriptDict[newId] != null) {
                transcriptDict.remove(oldId)
                internalTranscript.removeAll { it.id == oldId }
                // Send out updated transcript
                coroutineScope.launch {
                    _transcriptListPublisher.emit(internalTranscript)
                }
            } else {
                // Update the placeholder message's ID to the new ID
                (placeholderMessage as TranscriptItem).updateId(newId)
                placeholderMessage.metadata?.status = MessageStatus.Sent
                transcriptDict.remove(oldId)
                transcriptDict[newId] = placeholderMessage
            }
            coroutineScope.launch {
                _transcriptPublisher.emit(placeholderMessage)
            }
        }
    }

    private fun updateTemporaryMessageForAttachments(tempMessage: Message,
                                                     message: Message,
                                                     currentDict: MutableMap<String, TranscriptItem>){
        tempMessage.updateId(message.id)
        tempMessage.updateTimeStamp(message.timeStamp)
        tempMessage.text = message.text
        tempMessage.contentType = message.contentType
        tempMessage.attachmentId = message.attachmentId
        currentDict.remove(tempMessage.id)
        currentDict[message.id] = tempMessage
        handleTranscriptItemUpdate(tempMessage)
    }


    override suspend fun disconnectChatSession(): Result<Boolean> {
        return runCatching {
            // Check if the chat session is active
            if (!connectionDetailsProvider.isChatSessionActive()) {
                webSocketManager.disconnect("Session inactive")
                clearSubscriptionsAndPublishers()
                SDKLogger.logger.logDebug { "Chat session is inactive. Disconnecting websocket and clearing resources." }
                return Result.success(true) // Successfully handled the inactive session case
            }

            // Proceed with the disconnection logic
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            awsClient.disconnectParticipantConnection(connectionDetails.connectionToken)
                .getOrThrow()
            SDKLogger.logger.logDebug { "Participant Disconnected" }
            webSocketManager.disconnect("Customer ended the chat")
            _eventPublisher.emit(ChatEvent.ChatEnded)
            connectionDetailsProvider.setChatSessionState(false)
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to disconnect participant: ${exception.message}" }
        }
    }

    override suspend fun suspendWebSocketConnection(): Result<Boolean> {
        return runCatching {
            webSocketManager.suspendWebSocketConnection()
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to suspend chat: ${exception.message}" }
        }
    }

    override suspend fun resumeWebSocketConnection(): Result<Boolean> {
        return runCatching {
            webSocketManager.resumeWebSocketConnection()
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to resume chat: ${exception.message}" }
        }
    }

    override suspend fun reset(): Result<Boolean> {
        return runCatching {
            webSocketManager.disconnect("Resetting ChatService")
            clearSubscriptionsAndPublishers()
            connectionDetailsProvider.reset()
            attachmentIdToTempMessageId.clear()
            tempMessageIdToFileUrl.clear()
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to reset state: ${exception.message}" }
        }
    }

    override suspend fun sendMessage(contentType: ContentType, message: String): Result<Boolean> {
        val connectionDetails = connectionDetailsProvider.getConnectionDetails()
            ?: return Result.failure(Exception("No connection details available"))

        val recentlySentMessage = TranscriptItemUtils.createDummyMessage(
            content = message,
            contentType = contentType.type,
            status = MessageStatus.Sending,
            displayName = getRecentDisplayName()
        )

        sendSingleUpdateToClient(recentlySentMessage)

        return runCatching {
            val response = awsClient.sendMessage(
                connectionToken = connectionDetails.connectionToken,
                contentType = contentType,
                message = message
            ).getOrThrow()

            metricsManager.addCountMetric(MetricName.SendMessage)

            response.id?.let { id ->
                updatePlaceholderMessage(oldId = recentlySentMessage.id, newId = id)
            }
            true
        }.onFailure { exception ->
            recentlySentMessage.metadata?.status = MessageStatus.Failed
            sendSingleUpdateToClient(recentlySentMessage)
            SDKLogger.logger.logError { "Failed to send message: ${exception.message}" }
        }
    }

    override suspend fun resendFailedMessage(messageId: String): Result<Boolean> {
        val oldMessage = transcriptDict[messageId] as? Message

        // cannot retry if old message didn't exist or fail to be sent
        if (oldMessage == null || MessageStatus.Failed != oldMessage.metadata?.status) {
            return Result.failure(Exception("Unable to find the failed message"))
        }

        // remove failed message from transcript & transcript dict
        internalTranscript.removeAll { it.id == messageId }
        transcriptDict.remove(messageId)
        // Send out updated transcript with old message removed
        coroutineScope.launch {
            _transcriptListPublisher.emit(internalTranscript)
        }

        // as the next step, attempt to resend the message based on its type
        val attachmentUrl = tempMessageIdToFileUrl[messageId]
        // if old message is an attachment
        if (attachmentUrl != null) {
            return sendAttachment(attachmentUrl)
        } else {
            (ContentType.fromType(oldMessage.contentType))?.let { contentType ->
                return sendMessage(contentType, oldMessage.text)
            } ?:
                return Result.failure(Exception("Unable to find the failed message"))
        }
    }

    override suspend fun sendEvent(contentType: ContentType, content: String): Result<Boolean> {
        // Check if it's a typing event and throttle if necessary
        if (contentType == ContentType.TYPING && throttleTypingEvent) {
            // Skip sending if throttled
            return Result.success(true)
        }

        // Set up throttling for typing events
        if (contentType == ContentType.TYPING) {
            throttleTypingEvent = true
            throttleTypingEventTimer = Timer().apply {
                schedule(10000) {
                    throttleTypingEvent = false
                }
            }
        }

        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            awsClient.sendEvent(connectionDetails.connectionToken, contentType, content).getOrThrow()
            true
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to send event: ${exception.message}" }
        }
    }

    private fun registerNotificationListeners() {
        // Observe lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        coroutineScope.launch {
            webSocketManager.requestNewWsUrlFlow.collect {
                handleNewWsUrlRequest()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Called when the app goes to the background
        typingIndicatorTimer?.cancel()
        removeTypingIndicators()
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }


    private fun getRecentDisplayName(): String {
        val recentCustomerMessage = transcriptDict.values
            .filterIsInstance<Message>()
            .filter { it.participant == "CUSTOMER" }.maxByOrNull { it.timeStamp }
        return recentCustomerMessage?.displayName ?: ""
    }

    private suspend fun handleNewWsUrlRequest() {
        webSocketManager.isReconnecting.value = true

        val chatDetails = connectionDetailsProvider.getChatDetails()
        chatDetails?.let {
            val result = awsClient.createParticipantConnection(it.participantToken)
            if (result.isSuccess) {
                val connectionDetails = result.getOrNull()
                connectionDetailsProvider.updateConnectionDetails(connectionDetails!!)
                setupWebSocket(connectionDetails.websocketUrl, true)
            } else {
                val error = result.exceptionOrNull()
                if (error?.message == "Access denied") {
                    // Handle chat ended scenario
                    val endedEvent = TranscriptItemUtils.createDummyEndedEvent()
                    updateTranscriptDict(endedEvent)
                    _eventPublisher.emit(ChatEvent.ChatEnded)
                }
                SDKLogger.logger.logError { "CreateParticipantConnection failed: $error" }
            }
            webSocketManager.isReconnecting.value = false
        } ?: run {
            // No chat details available, mark reconnection as failed
            webSocketManager.isReconnecting.value = false
            SDKLogger.logger.logError { "Failed to retrieve chat details." }
        }
    }

    override suspend fun sendAttachment(fileUri: Uri): Result<Boolean> {
        var recentlySentAttachmentMessage: Message? = null

        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")

            // Create the dummy message and send it to the client UI
            recentlySentAttachmentMessage = TranscriptItemUtils.createDummyMessage(
                content = fileUri.getOriginalFileName(context) ?: "Attachment",
                contentType = getMimeType(fileUri.toString()),
                status = MessageStatus.Sending,
                attachmentId = UUID.randomUUID().toString(),  // Temporary attachmentId
                displayName = getRecentDisplayName()
            )

            recentlySentAttachmentMessage?.let { message ->
                tempMessageIdToFileUrl[message.id] = fileUri
                sendSingleUpdateToClient(message)
                // Get the attachmentId by starting the upload
                val attachmentIdResult = attachmentsManager.sendAttachment(connectionDetails.connectionToken, fileUri)

                // Get the attachmentId immediately
                val attachmentId = attachmentIdResult.getOrThrow()

                attachmentIdToTempMessageId[attachmentId] = message.id
            }

            true
        }.onFailure { exception ->
            // Update the recentlySentAttachmentMessage with a failure status if the message was created
            SDKLogger.logger.logError { "Failed to send attachment: ${exception.message}" }
            recentlySentAttachmentMessage?.let {
                it.metadata?.status =
                    MessageStatus.customFailed(exception.message ?: "Failed to send attachment")
                sendSingleUpdateToClient(it)
                SDKLogger.logger.logError { "Message status updated to Failed: $it" }
            }
        }
    }


    override suspend fun downloadAttachment(attachmentId: String, fileName: String): Result<URL> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            attachmentsManager.downloadAttachment(connectionDetails.connectionToken, attachmentId, fileName).getOrThrow()
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to download attachment for attachmentId $attachmentId. Error: ${exception.message}" }
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: String): Result<URL> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            attachmentsManager.getAttachmentDownloadUrl(attachmentId, connectionDetails.connectionToken).getOrThrow()
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to retrieve attachment download URL for attachment $attachmentId. Error: ${exception.message}" }
        }
    }

    private suspend fun fetchReconnectedTranscript() {
        val lastItem = internalTranscript.lastOrNull { (it as? Message)?.metadata?.status != MessageStatus.Failed }
            ?: return

        // Construct the start position from the last item
        val startPosition = StartPosition().apply {
            id = lastItem.id
        }

        // Fetch the transcript starting from the last item
        fetchTranscriptWith(startPosition)
    }

    private fun isItemInInternalTranscript(Id: String?): Boolean {
        if (Id == null) return false
        for (item in internalTranscript.reversed()) {
            if (item.id == Id) {
                return true
            }
        }
        return false
    }

    private suspend fun fetchTranscriptWith(startPosition: StartPosition?) {
        getTranscript(startPosition = startPosition,
            scanDirection = ScanDirection.FORWARD,
            sortKey = SortKey.ASCENDING,
            maxResults = 100,
            nextToken = null).onSuccess { transcriptResponse ->
            val lastItem = transcriptResponse.transcript.lastOrNull()
            if (transcriptResponse.nextToken?.isNotEmpty() == true && lastItem != null) {
                val newStartPosition = StartPosition().apply { id = lastItem.id }
                if (!isItemInInternalTranscript(lastItem?.id)) {
                    fetchTranscriptWith(startPosition = newStartPosition)
                }
            }
        }.onFailure { error ->
            SDKLogger.logger.logError { "Error fetching transcript with startPosition $startPosition: ${error.localizedMessage}" }
        }
    }

    override suspend fun getTranscript(
        scanDirection: ScanDirection?,
        sortKey: SortKey?,
        maxResults: Int?,
        nextToken: String?,
        startPosition: StartPosition?
    ): Result<TranscriptResponse> {

        val connectionDetails = connectionDetailsProvider.getConnectionDetails()
            ?: throw Exception("No connection details available")

        val request = GetTranscriptRequest().apply {
            connectionToken = connectionDetails.connectionToken
            this.scanDirection = (scanDirection ?: ScanDirection.BACKWARD).toString()
            this.sortOrder = (sortKey ?: SortKey.ASCENDING).toString()
            this.maxResults = maxResults ?: 30
            this.startPosition = startPosition
            if (!nextToken.isNullOrEmpty()) {
                this.nextToken = nextToken
            }
        }

        return runCatching {
            val response = awsClient.getTranscript(request).getOrThrow()
            val transcriptItems = response.transcript
            // Format and process transcript items
            val formattedItems = transcriptItems.mapNotNull { transcriptItem ->
                TranscriptItemUtils.serializeTranscriptItem(transcriptItem)?.let { serializedItem ->
                    webSocketManager.parseTranscriptItemFromJson(serializedItem)?.also { parsedItem ->
                        updateTranscriptDict(parsedItem)
                    }
                }
            }

            SDKLogger.logger.logDebug { "Transcript fetched successfully" }
            SDKLogger.logger.logDebug { "Transcript Items: $formattedItems" }

            // Create and return the TranscriptResponse
            TranscriptResponse(
                initialContactId = response.initialContactId.orEmpty(),
                nextToken = response.nextToken.orEmpty(),
                transcript = formattedItems
            )
        }.onFailure { exception ->
            SDKLogger.logger.logError { "Failed to get transcript: ${exception.message}" }
        }
    }

    override suspend fun sendMessageReceipt(
        messageReceiptType: MessageReceiptType,
        messageId: String,
    ): Result<Unit> {
        return try {
            val receiptResult = messageReceiptsManager.throttleAndSendMessageReceipt(messageReceiptType, messageId)
            receiptResult.fold(
                onSuccess = { pendingMessageReceipts ->
                    sendPendingMessageReceipts(pendingMessageReceipts)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            SDKLogger.logger.logError { "Error in sendMessageReceipt: ${e.message}" }
            Result.failure(e)
        }
    }

    private suspend fun sendPendingMessageReceipts(
        pendingMessageReceipts: PendingMessageReceipts,
    ): Result<Unit> = coroutineScope {
        var lastError: Throwable? = null
        val readId = pendingMessageReceipts.readReceiptMessageId
        val deliveredId = pendingMessageReceipts.deliveredReceiptMessageId
        messageReceiptsManager.clearPendingMessageReceipts()
        val readJob = readId?.let { messageId ->
            async {
                val content = "{\"messageId\":\"$messageId\"}"
                val result = sendEvent(contentType = MessageReceiptType.MESSAGE_READ.toContentType(), content = content)
                if (!result.isSuccess) {
                    val error = result.exceptionOrNull()
                    SDKLogger.logger.logError { "Failed to send message read receipt: ${error?.message}, messageId: $messageId" }
                    lastError = error ?: Exception("Unknown error sending read receipt")
                }
            }
        }
        val deliveredJob = deliveredId?.let { messageId ->
            async {
                val content = "{\"messageId\":\"$messageId\"}"
                val result = sendEvent(contentType = MessageReceiptType.MESSAGE_DELIVERED.toContentType(), content = content)
                if (!result.isSuccess) {
                    val error = result.exceptionOrNull()
                    SDKLogger.logger.logError { "Failed to send message delivered receipt: ${error?.message}, messageId: $messageId" }
                    lastError = error ?: Exception("Unknown error sending delivered receipt")
                }
            }
        }
        // Await all async tasks
        readJob?.await()
        deliveredJob?.await()
        // Return success or the last encountered error
        return@coroutineScope if (lastError != null) {
            Result.failure(lastError!!)
        } else {
            Result.success(Unit)
        }
    }

    private fun clearSubscriptionsAndPublishers() {
        transcriptCollectionJob?.cancel()
        eventCollectionJob?.cancel()
        chatSessionStateCollectionJob?.cancel()

        transcriptCollectionJob = null
        eventCollectionJob = null
        chatSessionStateCollectionJob = null

        transcriptDict = mutableMapOf()
        internalTranscript = mutableListOf()

        typingIndicatorTimer?.cancel()
        throttleTypingEventTimer?.cancel()
    }

}