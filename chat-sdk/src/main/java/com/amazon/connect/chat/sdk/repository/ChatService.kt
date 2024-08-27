package com.amazon.connect.chat.sdk.repository

import android.net.Uri
import android.util.Log
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazon.connect.chat.sdk.network.AttachmentsManager
import com.amazon.connect.chat.sdk.network.MetricsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ChatService {
    fun configure(config: GlobalConfig)
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

    val eventPublisher: SharedFlow<ChatEvent>
    val transcriptPublisher: SharedFlow<TranscriptItem>
    val transcriptListPublisher : SharedFlow<List<TranscriptItem>>
    val chatSessionStatePublisher: SharedFlow<Boolean>

    /**
     * Sends an attachment.
     * @return A Result indicating whether sending the attachment was successful.
     */
    suspend fun sendAttachment(fileUri: Uri): Result<Boolean>
}

class ChatServiceImpl @Inject constructor(
    private val awsClient: AWSClient,
    private val connectionDetailsProvider: ConnectionDetailsProvider,
    private val webSocketManager: WebSocketManager,
    private val metricsManager: MetricsManager,
    private val attachmentsManager: AttachmentsManager
) : ChatService {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _eventPublisher= MutableSharedFlow<ChatEvent>()
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

    private var transcriptDict = mutableMapOf<String, TranscriptItem>()
    private var internalTranscript = mutableListOf<TranscriptItem>()

    override fun configure(config: GlobalConfig) {
        awsClient.configure(config)
    }

    init {
        registerNotificationListeners()
    }

    override suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean> {
        setupEventSubscriptions()
        return runCatching {
            connectionDetailsProvider.updateChatDetails(chatDetails)
            val connectionDetails = awsClient.createParticipantConnection(chatDetails.participantToken).getOrThrow()
            metricsManager.addCountMetric(MetricName.CreateParticipantConnection);
            connectionDetailsProvider.updateConnectionDetails(connectionDetails)
            setupWebSocket(connectionDetails.websocketUrl)
            Log.d("ChatServiceImpl", "Participant Connected")
            true
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to create chat session: ${exception.message}", exception)
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
                        Log.d("ChatServiceImpl", "Connection Established")
                    }

                    ChatEvent.ConnectionReEstablished -> {
                        Log.d("ChatServiceImpl", "Connection Re-Established")
                    }

                    ChatEvent.ChatEnded -> {
                        Log.d("ChatServiceImpl", "Chat Ended")
                        connectionDetailsProvider.setChatSessionState(false)
                    }

                    ChatEvent.ConnectionBroken -> Log.d("ChatServiceImpl", "Connection Broken")
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
        when(item) {
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
                }

                // TODO ; Handle temporary attachment here

                transcriptDict[item.id] = item
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
        // TODO
        // TODO("removeTypingIndicators: Not yet implemented")
    }

    private fun handleEvent(event: Event, currentDict: MutableMap<String, TranscriptItem>) {
        if (event.contentType == ContentType.TYPING.type) {
            // TODO ; reset typing timer
        }
        currentDict[event.id] = event
    }


    private fun handleTranscriptItemUpdate(item: TranscriptItem) {
        // Send out the individual transcript item to subscribers
        coroutineScope.launch {
            _transcriptPublisher.emit(item)
        }

        // Update the internal transcript list with the new or updated item
        val existingIndex = internalTranscript.indexOfFirst { it.id == item.id }
        if (existingIndex != -1) {
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
        Log.d("ChatServiceImpl", "Updated transcript: $internalTranscript")

        // Send the updated transcript list to subscribers
        coroutineScope.launch {
            _transcriptListPublisher.emit(internalTranscript)
        }
    }

    override suspend fun disconnectChatSession(): Result<Boolean> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            awsClient.disconnectParticipantConnection(connectionDetails.connectionToken).getOrThrow()
            Log.d("ChatServiceImpl", "Participant Disconnected")
            connectionDetailsProvider.setChatSessionState(false)
            clearSubscriptionsAndPublishers()
            true
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to disconnect participant: ${exception.message}", exception)
        }
    }


    private fun registerNotificationListeners() {
        Log.d("ChatServiceImpl", "registerNotificationListeners")
        coroutineScope.launch {
            webSocketManager.requestNewWsUrlFlow.collect{
                handleNewWsUrlRequest()
            }
        }
    }

    private suspend fun handleNewWsUrlRequest() {
        Log.d("ChatServiceImpl", "handleNewWsUrlRequest")

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
//                    val endedEvent = TranscriptItemUtils.createDummyEndedEvent()
//                    updateTranscriptDict(endedEvent)
                    _eventPublisher.emit(ChatEvent.ChatEnded)
                }
                Log.e("ChatServiceImpl", "CreateParticipantConnection failed: $error")
            }
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
    }

    override suspend fun sendAttachment(fileUri: Uri): Result<Boolean> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")

            attachmentsManager.sendAttachment(connectionDetails.connectionToken, fileUri)
            true
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to send attachment: ${exception.message}", exception)
        }
    }
}