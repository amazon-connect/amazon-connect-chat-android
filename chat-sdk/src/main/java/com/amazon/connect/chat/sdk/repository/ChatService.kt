package com.amazon.connect.chat.sdk.repository

import android.util.Log
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.MetricName
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.MetricsManager
import com.amazon.connect.chat.sdk.network.WebSocketManager
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
}

class ChatServiceImpl @Inject constructor(
    private val awsClient: AWSClient,
    private val connectionDetailsProvider: ConnectionDetailsProvider,
    private val webSocketManager: WebSocketManager,
    private val metricsManager: MetricsManager
) : ChatService {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _eventPublisher= MutableSharedFlow<ChatEvent>()
    override val eventPublisher: SharedFlow<ChatEvent> get() = _eventPublisher
    private var eventCollectionJob: Job? = null

    private val _transcriptPublisher = MutableSharedFlow<TranscriptItem>()
    override val transcriptPublisher: SharedFlow<TranscriptItem> get() = _transcriptPublisher
    private var transcriptCollectionJob: Job? = null

    override fun configure(config: GlobalConfig) {
        awsClient.configure(config)
    }

    init {
        setupEventSubscriptions()
        registerNotificationListeners()
    }

    override suspend fun createChatSession(chatDetails: ChatDetails): Result<Boolean> {
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
        transcriptCollectionJob?.cancel()
        eventCollectionJob?.cancel()

        eventCollectionJob = coroutineScope.launch {
            webSocketManager.eventPublisher.collect { event ->
                when (event) {
                    ChatEvent.ConnectionEstablished -> {
                        Log.d("ChatServiceImpl", "Connection Established")
                    }

                    ChatEvent.ConnectionReEstablished -> {
                        Log.d("ChatServiceImpl", "Connection Re-Established")
                    }

                    ChatEvent.ChatEnded -> Log.d("ChatServiceImpl", "Chat Ended")
                    ChatEvent.ConnectionBroken -> Log.d("ChatServiceImpl", "Connection Broken")
                }
                _eventPublisher.emit(event)
            }
        }

        transcriptCollectionJob = coroutineScope.launch {
            webSocketManager.transcriptPublisher.collect { transcriptItem ->
                _transcriptPublisher.emit(transcriptItem)
            }
        }
    }

    override suspend fun disconnectChatSession(): Result<Boolean> {
        return runCatching {
            val connectionDetails = connectionDetailsProvider.getConnectionDetails()
                ?: throw Exception("No connection details available")
            awsClient.disconnectParticipantConnection(connectionDetails.connectionToken).getOrThrow()
            Log.d("ChatServiceImpl", "Participant Disconnected")
            clearSubscriptionsAndPublishers()
            true
        }.onFailure { exception ->
            Log.e("ChatServiceImpl", "Failed to disconnect participant: ${exception.message}", exception)
        }
    }


    private fun registerNotificationListeners() {
        coroutineScope.launch {
            Log.d("ChatServiceImpl", "registerNotificationListeners")
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
    }

}