package com.amazon.connect.chat.sdk.network

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.WebSocketMessageType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
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
    val transcriptPublisher: SharedFlow<TranscriptItem>
    val requestNewWsUrlFlow: MutableSharedFlow<Unit>
    suspend fun connect(wsUrl: String, isReconnectFlow: Boolean = false)
    suspend fun disconnect()
    fun formatAndProcessTranscriptItems(transcriptItems: List<TranscriptItem>) : List<TranscriptItem>
}

class WebSocketManagerImpl @Inject constructor(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val networkConnectionManager: NetworkConnectionManager
) : WebSocketManager {

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnectedToNetwork: Boolean = false
    private var isChatActive: Boolean = false
    private var isReconnecting: Boolean = false

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

    private val _transcriptPublisher = MutableSharedFlow<TranscriptItem>(
        replay = 0,
        extraBufferCapacity = 10
    )
    override val transcriptPublisher: SharedFlow<TranscriptItem> get() = _transcriptPublisher
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
                    if (isChatActive) {
                        reestablishConnection()
                    }
                } else {
                    isConnectedToNetwork = false
                    Log.d("WebSocketManager", "Network connection lost")
                }
            }
        }

        // Observe lifecycle events
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("AppLifecycleObserver", "App in Foreground")
                    if (isChatActive) {
                        reestablishConnection()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d("AppLifecycleObserver", "App in Background")
                    // Optional: Implement any specific actions for when the app goes to the background
                }
                else -> {}
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }


    // --- Initialization and Connection Management ---

    override suspend fun connect(wsUrl: String, isReconnectFlow: Boolean) {
        closeWebSocket()
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, createWebSocketListener(isReconnectFlow))
    }

    private fun closeWebSocket() {
        CoroutineScope(Dispatchers.IO).launch {
            resetHeartbeatManagers()
            webSocket?.close(1000, null)
        }
    }

    override suspend fun disconnect() {
        closeWebSocket()
    }

    // --- WebSocket Listener ---

    private fun createWebSocketListener(isReconnectFlow: Boolean) = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            coroutineScope.launch {
                handleWebSocketOpen(isReconnectFlow)
            }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            coroutineScope.launch {
                processJsonContent(text)
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            Log.i("WebSocket", "WebSocket is closing with code: $code, reason: $reason")
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            handleWebSocketClosed(code, reason)
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            handleWebSocketFailure(t)
        }
    }

    private suspend fun handleWebSocketOpen(isReconnectFlow: Boolean) {
        sendMessage(EventTypes.subscribe)
        startHeartbeats()
        isReconnecting = false
        isChatActive = true
        if (isReconnectFlow) {
            this._eventPublisher.emit(ChatEvent.ConnectionReEstablished)
        } else {
            this._eventPublisher.emit(ChatEvent.ConnectionEstablished)
        }
    }

    private fun handleWebSocketClosed(code: Int, reason: String) {
        Log.i("WebSocket", "WebSocket closed with code: $code, reason: $reason")
        if (code == 1000) {
            isChatActive = false
        } else if (isConnectedToNetwork && isChatActive) {
            reestablishConnection()
        }
    }

    private fun handleWebSocketFailure(t: Throwable) {
        if (t is IOException && t.message == "Software caused connection abort") {
            if (isChatActive && isConnectedToNetwork) {
                reestablishConnection()
            }
        }
    }

    private fun handlePing(json: JSONObject) {
        val statusCode = json.optInt("statusCode")
        val statusContent = json.optString("statusContent")
        if (statusCode == 200 && statusContent == "OK") {
            deepHeartbeatManager.heartbeatReceived()
        } else {
            Log.w("WebSocket", "Deep heartbeat failed. Status: $statusCode, StatusContent: $statusContent")
        }
    }

    // --- Message Processing ---

    private suspend fun processJsonContent(text: String) {
        val json = try {
            JSONObject(text)
        } catch (e: JSONException) {
            Log.e("WebSocket", "Failed to parse JSON message: $text", e)
            return
        }

        val topic = json.optString("topic")
        when (topic) {
            "aws/ping" -> handlePing(json)
            "aws/heartbeat" -> handleHeartbeat()
            "aws/chat" -> websocketDidReceiveMessage(json.optString("content"))
            else -> Log.i("WebSocket", "Unhandled topic: $topic")
        }
    }

    private suspend fun websocketDidReceiveMessage(content: String?) {
        content?.let {
            val transcriptItem = parseTranscriptItemFromJson(it)
            if (transcriptItem != null) {
                this._transcriptPublisher.emit(transcriptItem)
            } else {
                Log.i("WebSocket", "Received unrecognized or unsupported content.")
            }
        } ?: run {
            Log.w("WebSocket", "Received null or empty content in chat message")
        }
    }

    private suspend fun parseTranscriptItemFromJson(jsonString: String): TranscriptItem? {
        val json = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            Log.e("WebSocket", "Failed to parse inner JSON content: $jsonString", e)
            return null
        }

        json?.let { jsonObject ->
            val typeString = jsonObject.optString("Type")
            val type = WebSocketMessageType.fromType(typeString)

            return when (type) {
                WebSocketMessageType.MESSAGE -> handleMessage(jsonObject)
                WebSocketMessageType.EVENT -> {
                    val eventTypeString = jsonObject.optString("ContentType")
                    when (val eventType = ContentType.fromType(eventTypeString)) {
                        ContentType.JOINED -> handleParticipantEvent(jsonObject)
                        ContentType.LEFT -> handleParticipantEvent(jsonObject)
                        ContentType.TYPING -> handleTyping(jsonObject)
                        ContentType.ENDED -> handleChatEnded(jsonObject)
                        else -> {
                            Log.w("WebSocket", "Unknown event: $eventType")
                            null
                        }
                    }
                }
                // TODO-  WebSocketMessageType.ATTACHMENT -> handleAttachment(jsonObject)
                WebSocketMessageType.MESSAGE_METADATA -> handleMetadata(jsonObject)
                else -> {
                    Log.w("WebSocket", "Unknown websocket message type: $type")
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

    private fun startHeartbeats() {
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
            Log.w("WebSocket", "Heartbeat missed")
        }else {
            Log.w("WebSocket", "Heartbeat missed, no internet connection")
        }
    }

    private fun onDeepHeartbeatMissed() {
        if (isConnectedToNetwork) {
            reestablishConnection()
            Log.w("WebSocket", "Deep Heartbeat missed, retrying connection")
        } else {
            Log.w("WebSocket", "Deep Heartbeat missed, no internet connection")
        }
        val success = this._eventPublisher.tryEmit(ChatEvent.ConnectionBroken)
        if (!success) {
            Log.d("WebSocket", "Failed to emit ConnectionBroken event, " +
                    "no subscribers and no buffer capacity")
        }
    }

    // --- Helper Methods ---

    private fun reestablishConnection() {
        if (!isReconnecting) {
            requestNewWsUrl()
            isReconnecting = true
        }
    }

    private fun requestNewWsUrl() {
        CoroutineScope(Dispatchers.IO).launch {
            requestNewWsUrlFlow.emit(Unit)
            isReconnecting = false
        }
    }

    private fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            webSocket?.send(message)
        }
    }

    // --- Helper Methods for websocket data ---

    private fun handleMessage(innerJson: JSONObject): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val messageId = innerJson.getString("Id")
        val messageText = innerJson.getString("Content")
        val displayName = innerJson.getString("DisplayName")
        val time = innerJson.getString("AbsoluteTime")

        // TODO: Pass raw data
        val message = Message(
            participant = participantRole,
            text = messageText,
            contentType = innerJson.getString("ContentType"),
            timeStamp = time,
            id = messageId,
            displayName = displayName
        )
        return message
    }

    private fun handleParticipantEvent(innerJson: JSONObject): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val displayName = innerJson.getString("DisplayName")
        val time = innerJson.getString("AbsoluteTime")
        val eventId = innerJson.getString("Id")

        val event = Event(
            id = eventId,
            timeStamp = time,
            displayName = displayName,
            participant = participantRole,
            text = innerJson.getString("ContentType"), // TODO: Need to be removed and replaced in UI once callbacks are hooked
            contentType = innerJson.getString("ContentType"),
            eventDirection = MessageDirection.COMMON,
        )
        return event
    }

    private fun handleTyping(innerJson: JSONObject): TranscriptItem {
        val participantRole = innerJson.getString("ParticipantRole")
        val time = innerJson.getString("AbsoluteTime")
        val displayName = innerJson.getString("DisplayName")
        val eventId = innerJson.getString("Id")

        val event = Event(
            timeStamp = time,
            contentType =  innerJson.getString("ContentType"),
            id = eventId,
            displayName = displayName,
            participant = participantRole
        )
        return event
    }

    private suspend fun handleChatEnded(innerJson: JSONObject): TranscriptItem {
        closeWebSocket();
        isChatActive = false;
        this._eventPublisher.emit(ChatEvent.ChatEnded)
        val time = innerJson.getString("AbsoluteTime")
        val eventId = innerJson.getString("Id")
        val event = Event(
            timeStamp = time,
            contentType =  innerJson.getString("ContentType"),
            id = eventId,
            eventDirection = MessageDirection.COMMON
        )
        return event
    }

    private fun handleMetadata(innerJson: JSONObject): TranscriptItem {
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
            status = status
        )
        return metadata
    }

    override fun formatAndProcessTranscriptItems(transcriptItems: List<TranscriptItem>): List<TranscriptItem> {
        // TODO: Need to be updated with latest transcript items format
//        transcriptItems.forEach { item ->
//            val participantRole = item.participantRole
//
//            // Create the message content in JSON format
//            val messageContentJson = JSONObject().apply {
//                put("Id", item.id ?: "")
//                put("ParticipantRole", participantRole)
//                put("AbsoluteTime", item.absoluteTime ?: "")
//                put("ContentType", item.contentType ?: "")
//                put("Content", item.content ?: "")
//                put("Type", item.type)
//                put("DisplayName", item.displayName ?: "")
//            }
//
//            // Convert JSON object to String format
//            val messageContentString = messageContentJson.toString()
//
//            // Prepare the message in the format expected by WebSocket
//            val wrappedMessageString = "{\"content\":\"${messageContentString.replace("\"", "\\\"")}\"}"
//
//            // Send the formatted message string via WebSocket
//            websocketDidReceiveMessage(wrappedMessageString)
//        }
        return emptyList()
    }

}
