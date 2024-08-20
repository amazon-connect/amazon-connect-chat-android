package com.amazon.connect.chat.sdk

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.repository.ChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ChatSession {
    fun configure(config: GlobalConfig)
    /**
     * Connects to a chat session with the specified chat details.
     * @param chatDetails The details of the chat.
     * @return A Result indicating whether the connection was successful.
     */
    suspend fun connect(chatDetails: ChatDetails): Result<Unit>

    /**
     * Disconnects the current chat session.
     * @return A Result indicating whether the disconnection was successful.
     */
    suspend fun disconnect(): Result<Unit>

    var onConnectionEstablished: (() -> Unit)?
    var onConnectionReEstablished: (() -> Unit)?
    var onConnectionBroken: (() -> Unit)?
    var onMessageReceived: ((TranscriptItem) -> Unit)?
    var onChatEnded: (() -> Unit)?
}

@Singleton
class ChatSessionImpl @Inject constructor(private val chatService: ChatService) : ChatSession {

    override var onConnectionEstablished: (() -> Unit)? = null
    override var onConnectionReEstablished: (() -> Unit)? = null
    override var onConnectionBroken: (() -> Unit)? = null
    override var onMessageReceived: ((TranscriptItem) -> Unit)? = null
    override var onChatEnded: (() -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var eventCollectionJob: Job? = null
    private var transcriptCollectionJob: Job? = null

    init {
        setupEventSubscriptions()
    }

    private fun setupEventSubscriptions() {
        // Cancel any existing subscriptions before setting up new ones
        eventCollectionJob?.cancel()
        transcriptCollectionJob?.cancel()

        // Set up new subscriptions
        eventCollectionJob = coroutineScope.launch {
            chatService.eventPublisher.collect { event ->
                when (event) {
                    ChatEvent.ConnectionEstablished -> onConnectionEstablished?.invoke()
                    ChatEvent.ConnectionReEstablished -> onConnectionReEstablished?.invoke()
                    ChatEvent.ChatEnded -> onChatEnded?.invoke()
                    ChatEvent.ConnectionBroken -> onConnectionBroken?.invoke()
                }
            }
        }

        transcriptCollectionJob = coroutineScope.launch {
            chatService.transcriptPublisher.collect { transcriptItem ->
                onMessageReceived?.invoke(transcriptItem)
            }
        }
    }

    override fun configure(config: GlobalConfig) {
        chatService.configure(config)
    }

    override suspend fun connect(chatDetails: ChatDetails): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.createChatSession(chatDetails)
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.disconnectChatSession()
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }.also {
            cleanup()
        }
    }

    private fun cleanup() {
        // Cancel flow collection jobs when disconnecting or cleaning up
        eventCollectionJob?.cancel()
        transcriptCollectionJob?.cancel()
    }
}