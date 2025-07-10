// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageMetadataProtocol
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.WebSocketMessageType
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.repository.HeartbeatManager
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

object EventTypes {
    const val subscribe =
        "{\"topic\": \"aws/subscribe\", \"content\": {\"topics\": [\"aws/chat\"]}}"
    const val heartbeat = "{\"topic\": \"aws/heartbeat\"}"
    const val deepHeartbeat = "{\"topic\": \"aws/ping\"}"
}

interface WebSocketManager {
    val eventPublisher: SharedFlow<ChatEvent>
    val transcriptPublisher: SharedFlow<Pair<TranscriptItem, Boolean>>
    val requestNewWsUrlFlow: MutableSharedFlow<Unit>
    var isReconnecting: MutableStateFlow<Boolean>
    suspend fun connect(wsUrl: String, isReconnectFlow: Boolean = false)
    suspend fun disconnect(reason: String?)
    suspend fun parseTranscriptItemFromJson(jsonString: String): TranscriptItem?
    fun suspendWebSocketConnection()
    fun resumeWebSocketConnection()
}

class WebSocketManagerImpl @Inject constructor(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val networkConnectionManager: NetworkConnectionManager,
    private val connectionDetailsProvider: ConnectionDetailsProvider
) : WebSocketManager {

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnectedToNetwork: Boolean = false
    private var isChatActive: Boolean = false
    private var isChatSuspended: Boolean = false

    private val _isReconnecting = MutableStateFlow(false)
    override var isReconnecting: MutableStateFlow<Boolean>
        get() = _isReconnecting
        set(value) {
            _isReconnecting.value = value.value
        }

    private var heartbeatManager: HeartbeatManager = HeartbeatManager(
        sendHeartbeatCallback = ::sendHeartbeat,
        missedHeartbeatCallback = ::onHeartbeatMissed
    )
    private var deepHeartbeatManager: HeartbeatManager = HeartbeatManager(
        sendHeartbeatCallback = ::sendDeepHeartbeat,
        missedHeartbeatCallback = ::onDeepHeartbeatMissed
    )

    private val _eventPublisher = MutableSharedFlow<ChatEvent>(
        replay = 0,
        extraBufferCapacity = 5
    )
    override val eventPublisher: SharedFlow<ChatEvent> get() = _eventPublisher

    private val _transcriptPublisher = MutableSharedFlow<Pair<TranscriptItem, Boolean>>(
        replay = 0,
        extraBufferCapacity = 10
    )
    override val transcriptPublisher: SharedFlow<Pair<TranscriptItem, Boolean>> get() = _transcriptPublisher
    override val requestNewWsUrlFlow = MutableSharedFlow<Unit>()

    init {
        registerObservers()
    }

    private fun registerObservers() {
        networkConnectionManager.registerNetworkCallback()

        // Observe network state changes
        CoroutineScope(Dispatchers.IO).launch {
            networkConnectionManager.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable) {
                    isConnectedToNetwork = true
                    reestablishConnectionIfChatActive()
                } else {
                    isConnectedToNetwork = false
                    SDKLogger.logger.logInfo{"WebSocket: Network connection lost"}
                }
            }
        }

        // Observe lifecycle events
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    SDKLogger.logger.logInfo{"WebSocket: App in Foreground"}
                    reestablishConnectionIfChatActive()
                }
                Lifecycle.Event.ON_STOP -> {
                    SDKLogger.logger.logInfo{"WebSocket: App in Background"}
                    if (isChatActive) {
                        CoroutineScope(Dispatchers.IO).launch {
                            closeWebSocket("App Backgrounded", 4000)
                        }
                    }

                    // Optional: Implement any specific actions for when the app goes to the background
                }
                else -> {}
            }
        }

        // Ensure lifecycle observer registration happens on main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, no need to post
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        } else {
            // Not on main thread, post to main thread
            Handler(Looper.getMainLooper()).post {
                ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            }
        }
    }

    // --- Initialization and Connection Management ---

    override suspend fun connect(wsUrl: String, isReconnectFlow: Boolean) {
        closeWebSocket("Connecting...")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, createWebSocketListener(isReconnectFlow))
    }

    private fun closeWebSocket(reason: String? = null, code: Int = 1000) {
        CoroutineScope(Dispatchers.IO).launch {
            // 4000 = disconnect websocket due to backgrounding.
            if (code != 4000) {
                isChatActive = false
            }
            resetHeartbeatManagers()
            webSocket?.close(code, reason)
        }
    }

    override suspend fun disconnect(reason: String?) {
        closeWebSocket(reason)
    }

    override fun suspendWebSocketConnection() {
        isChatSuspended = true
        closeWebSocket("Suspend WebSocket Connection", 4000)
    }

    override fun resumeWebSocketConnection() {
        isChatSuspended = false
        reestablishConnectionIfChatActive()
    }

    // --- WebSocket Listener ---

    private fun createWebSocketListener(isReconnectFlow: Boolean) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            coroutineScope.launch {
                handleWebSocketOpen(isReconnectFlow)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            coroutineScope.launch {
                processJsonContent(text)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            SDKLogger.logger.logInfo{"WebSocket: WebSocket is closing with code: $code, reason: $reason"}
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            SDKLogger.logger.logInfo{"WebSocket: WebSocket is closed with code: $code, reason: $reason"}
            handleWebSocketClosed(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            SDKLogger.logger.logError{"WebSocket: WebSocket failure: ${t.message}"}
            handleWebSocketFailure(t)
        }
    }

    private suspend fun handleWebSocketOpen(isReconnectFlow: Boolean) {
        sendMessage(EventTypes.subscribe)
        startHeartbeats()
        _isReconnecting.value = false // Reconnection successful, reset flag
        isChatActive = true
        if (isReconnectFlow) {
            this._eventPublisher.emit(ChatEvent.ConnectionReEstablished)
        } else {
            this._eventPublisher.emit(ChatEvent.ConnectionEstablished)
        }
    }

    private fun handleWebSocketClosed(code: Int, reason: String) {
        SDKLogger.logger.logInfo{"WebSocket: WebSocket closed with code: $code, reason: $reason"}
        if (code == 1000) {
            isChatActive = false
        } else if (code != 4000) {
            reestablishConnectionIfChatActive()
        }
    }

    private fun handleWebSocketFailure(t: Throwable) {
        SDKLogger.logger.logError{"WebSocket: WebSocket failure: ${t.message}"}
        if (t is IOException && t.message == "Software caused connection abort") {
            reestablishConnectionIfChatActive()
        }
    }

    private fun handlePing(json: JSONObject) {
        val statusCode = json.optInt("statusCode")
        val statusContent = json.optString("statusContent")
        if (statusCode == 200 && statusContent == "OK") {
            deepHeartbeatManager.heartbeatReceived()
        } else {
            SDKLogger.logger.logWarn{"WebSocket: Deep heartbeat failed. Status: $statusCode, StatusContent: $statusContent"}
        }
    }

    private suspend fun processJsonContent(text: String) {
        val json = try {
            JSONObject(text)
        } catch (e: JSONException) {
            SDKLogger.logger.logError{"WebSocket: Failed to parse JSON message: $text and error $e"}
            return
        }

        val topic = json.optString("topic")
        when (topic) {
            "aws/ping" -> handlePing(json)
            "aws/heartbeat" -> handleHeartbeat()
            "aws/chat" -> {
                SDKLogger.logger.logDebug{"WebSocket: Received chat message from websocket $json"}
                handleWebsocketMessage(json.optString("content"))
            }
            else -> SDKLogger.logger.logInfo{"WebSocket: Unhandled topic: $topic"}
        }
    }

    private suspend fun handleWebsocketMessage(content: String?) {
        content?.let {
            val transcriptItem = parseTranscriptItemFromJson(it)
            if (transcriptItem != null) {
                this._transcriptPublisher.emit(Pair(transcriptItem, true))
            } else {
                SDKLogger.logger.logInfo{"WebSocket: Received unrecognized or unsupported content."}
            }
        } ?: run {
            SDKLogger.logger.logWarn{"WebSocket: Received null or empty content in chat message"}
        }
    }

    override suspend fun parseTranscriptItemFromJson(jsonString: String): TranscriptItem? {
        val json = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            SDKLogger.logger.logError{"WebSocket: Failed to parse inner JSON content: $jsonString: ${e.message}"}
            return null
        }

        json?.let { jsonObject ->
            val typeString = jsonObject.optString("Type")
            val type = WebSocketMessageType.fromType(typeString)

            return when (type) {
                WebSocketMessageType.MESSAGE -> handleMessage(jsonObject, jsonString)
                WebSocketMessageType.EVENT -> {
                    val eventTypeString = jsonObject.optString("ContentType")
                    when (val eventType = ContentType.fromType(eventTypeString)) {
                        ContentType.JOINED -> handleParticipantEvent(jsonObject, jsonString)
                        ContentType.LEFT -> handleParticipantEvent(jsonObject, jsonString)
                        ContentType.TYPING -> handleTyping(jsonObject, jsonString)
                        ContentType.ENDED -> handleChatEnded(jsonObject, jsonString)
                        else -> {
                            SDKLogger.logger.logWarn{"WebSocket: Unknown event: $eventType"}
                            null
                        }
                    }
                }
                WebSocketMessageType.ATTACHMENT -> handleAttachment(jsonObject, jsonString)
                WebSocketMessageType.MESSAGE_METADATA -> handleMetadata(jsonObject, jsonString)
                else -> {
                    SDKLogger.logger.logWarn{"WebSocket: Unknown websocket message type: $type"}
                    null
                }
            }
        }
        return null
    }

    // --- Heartbeat Logic ---

    private fun handleHeartbeat() {
        heartbeatManager.heartbeatReceived()
    }

    private fun resetHeartbeatManagers() {
        heartbeatManager.stopHeartbeat()
        deepHeartbeatManager.stopHeartbeat()
    }

    private suspend fun startHeartbeats() {
        heartbeatManager.startHeartbeat()
        deepHeartbeatManager.startHeartbeat()
    }

    private fun sendHeartbeat() {
        sendMessage(EventTypes.heartbeat)
    }

    private fun sendDeepHeartbeat() {
        sendMessage(EventTypes.deepHeartbeat)
    }

    private fun onHeartbeatMissed() {
        if (isConnectedToNetwork) {
            SDKLogger.logger.logWarn{"WebSocket: Heartbeat missed"}
        } else {
            SDKLogger.logger.logWarn{"WebSocket: Heartbeat missed, no internet connection"}
        }
    }

    private suspend fun onDeepHeartbeatMissed() {
        this._eventPublisher.emit(ChatEvent.DeepHeartBeatFailure)
        if (isConnectedToNetwork) {
            reestablishConnectionIfChatActive()
            SDKLogger.logger.logWarn{"WebSocket: Deep Heartbeat missed, retrying connection"}
        } else {
            SDKLogger.logger.logWarn{"WebSocket: Deep Heartbeat missed, no internet connection"}
        }
        val success = this._eventPublisher.tryEmit(ChatEvent.ConnectionBroken)
        if (!success) {
            SDKLogger.logger.logDebug{"WebSocket: Failed to emit ConnectionBroken event, no subscribers and no buffer capacity"}
        }
    }

    private fun reestablishConnectionIfChatActive() {
        if (!isChatActive) {
            SDKLogger.logger.logDebug{"WebSocket: Re-connection aborted due to inactive chat session"}
            return
        }
        if (!isConnectedToNetwork) {
            SDKLogger.logger.logDebug{"WebSocket: Re-connection aborted due to missing network connectivity"}
            return
        }
        if (isChatSuspended) {
            SDKLogger.logger.logDebug{"WebSocket: Re-connection aborted due to suspended chat session."}
            return
        }
        if (_isReconnecting.value) {
            SDKLogger.logger.logDebug{"WebSocket: Re-connection aborted due to ongoing reconnection attempt."}
            return
        }

        _isReconnecting.value = true
        requestNewWsUrl()
    }

    private fun requestNewWsUrl() {
        CoroutineScope(Dispatchers.IO).launch {
            requestNewWsUrlFlow.emit(Unit)
        }
    }

    private fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            webSocket?.send(message)
        }
    }

    // --- Helper Methods for websocket data ---

    private fun handleMessage(innerJson: JSONObject, rawData: String): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val messageId = innerJson.getString("Id")
        val messageText = innerJson.getString("Content")
        val displayName = innerJson.getString("DisplayName")
        val time = innerJson.getString("AbsoluteTime")

        val message = Message(
            participant = participantRole,
            text = messageText,
            contentType = innerJson.getString("ContentType"),
            timeStamp = time,
            id = messageId,
            displayName = displayName,
            serializedContent = rawData,
            metadata = if (innerJson.has("MessageMetadata"))
                    (handleMetadata(innerJson, rawData) as? MessageMetadataProtocol) else null
        )
        return message
    }

    private fun handleParticipantEvent(innerJson: JSONObject, rawData: String): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val displayName = innerJson.getString("DisplayName")
        val time = innerJson.getString("AbsoluteTime")
        val eventId = innerJson.getString("Id")

        val event = Event(
            id = eventId,
            timeStamp = time,
            displayName = displayName,
            participant = participantRole,
            text = innerJson.getString("ContentType"),
            contentType = innerJson.getString("ContentType"),
            eventDirection = MessageDirection.COMMON,
            serializedContent = rawData
        )
        return event
    }

    private fun handleTyping(innerJson: JSONObject, rawData: String): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val time = innerJson.getString("AbsoluteTime")
        val displayName = innerJson.getString("DisplayName")
        val eventId = innerJson.getString("Id")

        val event = Event(
            timeStamp = time,
            contentType = innerJson.getString("ContentType"),
            id = eventId,
            displayName = displayName,
            participant = participantRole,
            serializedContent = rawData,
        )
        return event
    }

    private suspend fun handleChatEnded(innerJson: JSONObject, rawData: String): TranscriptItem {
        val time = innerJson.getString("AbsoluteTime")
        val eventId = innerJson.getString("Id")
        val isFromPastSession = innerJson.optBoolean("isFromPastSession", false)
        // Current session event: Reset state and update session
        if (!isFromPastSession) {
            resetHeartbeatManagers()
            this._eventPublisher.emit(ChatEvent.ChatEnded)
            connectionDetailsProvider.setChatSessionState(false)
        }

        val event = Event(
            timeStamp = time,
            contentType = innerJson.getString("ContentType"),
            id = eventId,
            eventDirection = MessageDirection.COMMON,
            serializedContent = rawData
        )
        return event
    }

    private fun handleMetadata(innerJson: JSONObject, rawData: String): TranscriptItem {
        val messageMetadata = innerJson.getJSONObject("MessageMetadata")
        val messageId = messageMetadata.getString("MessageId")
        val receipts = messageMetadata.optJSONArray("Receipts")
        var status = MessageStatus.Delivered
        val time = innerJson.getString("AbsoluteTime")

        receipts?.let {
            for (i in 0 until it.length()) {
                val receipt = it.getJSONObject(i)
                if (receipt.optString("ReadTimestamp").isNotEmpty()) {
                    status = MessageStatus.Read
                }
            }
        }
        val metadata = MessageMetadata(
            contentType = innerJson.getString("ContentType"),
            eventDirection = MessageDirection.OUTGOING,
            timeStamp = time,
            id = messageId,
            status = status,
            serializedContent = rawData
        )
        return metadata
    }

    private fun handleAttachment(innerJson: JSONObject, rawData: String): TranscriptItem? {
        val participantRole = innerJson.getString("ParticipantRole")
        val time = innerJson.getString("AbsoluteTime")
        val displayName = innerJson.getString("DisplayName")
        val messageId = innerJson.getString("Id")

        val attachmentsArray = innerJson.optJSONArray("Attachments") ?: return null
        if (attachmentsArray.length() == 0) {
            SDKLogger.logger.logInfo{"WebSocket: No attachments found"}
            return null
        }

        // Access the first attachment
        val firstAttachment = attachmentsArray.optJSONObject(0) ?: return null

        // Extract details from the first attachment
        val attachmentName = firstAttachment.optString("AttachmentName")
        val contentType = firstAttachment.optString("ContentType")
        val attachmentId = firstAttachment.optString("AttachmentId")

        if (attachmentName == null || contentType == null || attachmentId == null) {
            SDKLogger.logger.logError{"WebSocket: Failed to access attachments"}
            return null
        }

        return Message(
            participant = participantRole,
            text = attachmentName,
            contentType = contentType,
            timeStamp = time,
            attachmentId = attachmentId,
            id = messageId,
            displayName = displayName,
            serializedContent = rawData
        )
    }
}
