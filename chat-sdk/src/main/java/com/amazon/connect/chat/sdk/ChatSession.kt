package com.amazon.connect.chat.sdk

import android.net.Uri
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

    /**
     * Checks if a chat session is currently active.
     * @return True if a chat session is active, false otherwise.
     */
    var onChatSessionStateChanged: ((Boolean) -> Unit)?

    /**
     * Sends an attachment.
     * @return A Result indicating whether sending the attachment was successful.
     */
    suspend fun sendAttachment(fileUri: Uri): Result<Unit>

    var onConnectionEstablished: (() -> Unit)?
    var onConnectionReEstablished: (() -> Unit)?
    var onConnectionBroken: (() -> Unit)?
    var onMessageReceived: ((TranscriptItem) -> Unit)?
    var onTranscriptUpdated: ((List<TranscriptItem>) -> Unit)?
    var onChatEnded: (() -> Unit)?
}

@Singleton
class ChatSessionImpl @Inject constructor(private val chatService: ChatService) : ChatSession {

    override var onConnectionEstablished: (() -> Unit)? = null
    override var onConnectionReEstablished: (() -> Unit)? = null
    override var onConnectionBroken: (() -> Unit)? = null
    override var onMessageReceived: ((TranscriptItem) -> Unit)? = null
    override var onTranscriptUpdated: ((List<TranscriptItem>) -> Unit)? = null
    override var onChatEnded: (() -> Unit)? = null
    override var onChatSessionStateChanged: ((Boolean) -> Unit)? = null
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
                    ChatEvent.ChatEnded -> onChatEnded?.invoke()
                    ChatEvent.ConnectionBroken -> onConnectionBroken?.invoke()
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
            chatService.transcriptListPublisher.collect { transcriptList ->
                if (transcriptList.isNotEmpty()) {
                    // Make sure it runs on main thread
                    coroutineScope.launch {
                        onTranscriptUpdated?.invoke(transcriptList)
                    }
                }
            }
        }

        chatSessionStateCollectionJob = coroutineScope.launch {
            chatService.chatSessionStatePublisher.collect { isActive ->
                onChatSessionStateChanged?.invoke(isActive)
            }
        }
    }

    override fun configure(config: GlobalConfig) {
        chatService.configure(config)
    }

    override suspend fun connect(chatDetails: ChatDetails): Result<Unit> {
        // Establish subscriptions whenever a new chat session is initiated
        setupEventSubscriptions()
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
        transcriptListCollectionJob?.cancel()
        chatSessionStateCollectionJob?.cancel()

        eventCollectionJob = null
        transcriptCollectionJob = null
        transcriptListCollectionJob = null
        chatSessionStateCollectionJob = null
    }
    
    override suspend fun sendAttachment(fileUri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.sendAttachment(fileUri)
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }
}