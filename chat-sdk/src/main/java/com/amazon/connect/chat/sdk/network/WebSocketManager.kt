package com.amazon.connect.chat.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazon.connect.chat.sdk.Config
import com.amazon.connect.chat.sdk.model.ChatEvent
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.model.WebSocketMessageType
import com.amazon.connect.chat.sdk.model.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

interface WebSocketManagerInterface {
    // TODO - Align it with IOS
    /*
    *     var eventPublisher: PassthroughSubject<ChatEvent, Never> { get }
    var transcriptPublisher: PassthroughSubject<TranscriptItem, Never> { get }
    func connect(wsUrl: URL?, isReconnect: Bool?)
    func disconnect()
    func formatAndProcessTranscriptItems(_ transcriptItems: [AWSConnectParticipantItem]) -> [TranscriptItem]
    * */
}

class WebSocketManager @Inject constructor(
    private val context: Context,
    var requestNewWsUrl: () -> Unit
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private lateinit var messageCallBack : (TranscriptItem) -> Unit
    private val chatConfiguration = Config
    private var heartbeatManager: HeartbeatManager = HeartbeatManager(
        sendHeartbeatCallback = ::sendHeartbeat,
        missedHeartbeatCallback = ::onHeartbeatMissed
    )
    private var deepHeartbeatManager: HeartbeatManager = HeartbeatManager(
        sendHeartbeatCallback = ::sendDeepHeartbeat,
        missedHeartbeatCallback = ::onDeepHeartbeatMissed
    )
    private val networkConnectionManager: NetworkConnectionManager = NetworkConnectionManager.getInstance(context)
    private var isConnectedToNetwork: Boolean = false
    private var isChatActive: Boolean = false
    private var isReconnecting: Boolean = false

    private val _eventPublisher = MutableSharedFlow<ChatEvent>(
// Keeping these here for future reference if we encounter issue with SharedFlow memory - Will update or remove later
//        replay = 1,              // Replay the last event to new subscribers
//        extraBufferCapacity = 10  // Buffer up to 10 events in case of bursts or slow consumption
    )
    val eventPublisher: SharedFlow<ChatEvent> get() = _eventPublisher

    private val _transcriptPublisher = MutableSharedFlow<TranscriptItem>(
//        replay = 1,              // Replay the last transcript item to new subscribers
//        extraBufferCapacity = 10  // Buffer up to 10 transcript items
    )
    val transcriptPublisher: SharedFlow<TranscriptItem> get() = _transcriptPublisher

    init {
        registerObservers()
    }

    private fun registerObservers() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isConnectedToNetwork = true
                if (isChatActive) {
                    requestNewWsUrl()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnectedToNetwork = false
            }
        };
        networkConnectionManager.registerNetworkCallback(networkCallback)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.d("AppLifecycleObserver", "App in Foreground")
                    if (isChatActive) {
                        reestablishConnection();
                    }
                }
                else -> {}
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    fun reestablishConnection() {
        if (!isReconnecting) {
            requestNewWsUrl()
            isReconnecting = true
        }
    }

    fun createWebSocket(url: String, onMessageReceived: (TranscriptItem) -> Unit, onConnectionFailed: (String) -> Unit) {
        val request = Request.Builder().url(url).build()
        this.messageCallBack = onMessageReceived
        closeWebSocket()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    CoroutineScope(Dispatchers.IO).launch {
                        handleWebSocketOpen()
                    }
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    Log.i("WebSocket", "Received message: $text")
                    CoroutineScope(Dispatchers.IO).launch {
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
                    handleWebSocketFailure(t, onConnectionFailed)
                }
            })

    }

    private suspend fun handleWebSocketOpen() {
        sendMessage(EventTypes.subscribe)
        startHeartbeats()
        isReconnecting = false
        isChatActive = true
        this._eventPublisher.emit(ChatEvent.ConnectionEstablished)
    }

    private fun handleWebSocketClosed(code: Int, reason: String) {
        Log.i("WebSocket", "WebSocket closed with code: $code, reason: $reason")
        if (code == 1000) {
            isChatActive = false
        } else if (isConnectedToNetwork && isChatActive) {
            reestablishConnection()
        }
    }

    private fun handleWebSocketFailure(t: Throwable, onConnectionFailed: (String) -> Unit) {
        onConnectionFailed(t.message ?: "Unknown Error")
        if (t is IOException && t.message == "Software caused connection abort") {
            if (isChatActive && isConnectedToNetwork) {
                reestablishConnection()
            }
        }
    }

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

    private fun handlePing(json: JSONObject) {
        val statusCode = json.optInt("statusCode")
        val statusContent = json.optString("statusContent")
        if (statusCode == 200 && statusContent == "OK") {
            deepHeartbeatManager.heartbeatReceived()
        } else {
            Log.w("WebSocket", "Deep heartbeat failed. Status: $statusCode, StatusContent: $statusContent")
        }
    }

    private fun handleHeartbeat() {
        heartbeatManager.heartbeatReceived()
    }

    private suspend fun websocketDidReceiveMessage(content: String?) {
        content?.let {
            val transcriptItem = parseTranscriptItemFromJson(it)
            if (transcriptItem != null) {
                this.messageCallBack(transcriptItem)
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


    // Heartbeat Logic
    fun resetHeartbeatManagers() {
        heartbeatManager.stopHeartbeat()
        deepHeartbeatManager.stopHeartbeat()
    }

    fun startHeartbeats() {
        heartbeatManager.startHeartbeat()
        deepHeartbeatManager.startHeartbeat()
    }

    fun sendHeartbeat() {
        sendMessage(EventTypes.heartbeat)
    }

    fun sendDeepHeartbeat() {
        sendMessage(EventTypes.deepHeartbeat)
    }

    fun onHeartbeatMissed() {
        // TODO: Invoke hearbeatFailure(), Check internet connectivity, publish connectionBroken event
        if (isConnectedToNetwork) {
            reestablishConnection()
        }
    }

    fun onDeepHeartbeatMissed() {
        // TODO: Invoke DeepHearbeatFailure(), Check internet connectivity, publish connectionBroken event
        if (isConnectedToNetwork) {
            reestablishConnection()
        }
        val success = this._eventPublisher.tryEmit(ChatEvent.ConnectionBroken)
        if (!success) {
            println("Emission failed for ${ChatEvent.ConnectionBroken}, no subscribers and no buffer capacity")
        }
    }

    // Message Handling Logic
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


    fun closeWebSocket() {
        CoroutineScope(Dispatchers.IO).launch {
            resetHeartbeatManagers()
            webSocket?.close(1000, null)
        }
    }

    fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            webSocket?.send(message)
        }
    }

    fun formatAndProcessTranscriptItems(transcriptItems: List<TranscriptItem>) {
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
    }

}
