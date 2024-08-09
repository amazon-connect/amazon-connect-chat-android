package com.amazon.connect.chat.sdk.network
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.amazon.connect.chat.sdk.Config
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageType
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.utils.CommonUtils
import com.amazon.connect.chat.sdk.utils.ContentType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.IOException
import org.json.JSONObject
import javax.inject.Inject

object EventTypes {
    const val subscribe =
        "{\"topic\": \"aws/subscribe\", \"content\": {\"topics\": [\"aws/chat\"]}}"
    const val heartbeat = "{\"topic\": \"aws/heartbeat\"}"
    const val deepHeartbeat = "{\"topic\": \"aws/ping\"}"
}

class WebSocketManager @Inject constructor(
    private val context: Context,
    var requestNewWsUrl: () -> Unit
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(60, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private lateinit var messageCallBack : (Message) -> Unit
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

    fun createWebSocket(url: String, onMessageReceived: (Message) -> Unit, onConnectionFailed: (String) -> Unit) {
        val request = Request.Builder().url(url).build()
        this.messageCallBack = onMessageReceived
        closeWebSocket();
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // Handle WebSocket open event
                sendMessage(EventTypes.subscribe)

                // Start heartbeats
                startHeartbeats()
                isReconnecting = false
                isChatActive = true
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.i("text@onMessage",text)
                websocketDidReceiveMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                // Handle WebSocket closing event
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                // Handle WebSocket closed event
                if (code == 1000) {
                    // TODO: set isChatActive to false
                    isChatActive = false
                } else if (isConnectedToNetwork && isChatActive) {
                    reestablishConnection();
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                onConnectionFailed(t.message ?: "Unknown Error")
                if (t is IOException && t.message == "Software caused connection abort") {
                    if (isChatActive && isConnectedToNetwork) {
                        reestablishConnection()
                    }
                }
            }
        })
    }

    fun websocketDidReceiveMessage(text: String) {
        val json = JSONObject(text)

        val topic = json.opt("topic")
        when (topic) {
            "aws/ping" -> {
                val statusCode = json.opt("statusCode")
                val statusContent = json.opt("statusContent")
                if ((statusCode as? Int == 200) && (statusContent == "OK")) {
                    deepHeartbeatManager?.heartbeatReceived()
                } else {
                    println("Deep heartbeat failed. Status: ${statusCode ?: "nil"}, StatusContent: ${statusContent ?: "nil"}")
                }
            }
            "aws/heartbeat" -> {
                heartbeatManager?.heartbeatReceived()
            }
            "aws/chat" -> {
                val content = json.opt("content")
                if (content is String) {
                    val contentJson = JSONObject(content)
                    contentJson?.let {
                        if (it.has("Type") && it.has("ContentType")) {
                            val type = it.getString("Type")
                            val contentType = it.getString("ContentType")
                            when {
                                type == "MESSAGE" -> handleMessage(it)
                                contentType == ContentType.JOINED.type -> handleParticipantJoined(it)
                                contentType == ContentType.LEFT.type -> handleParticipantLeft(it)
                                contentType == ContentType.TYPING.type -> handleTyping(it)
                                contentType == ContentType.ENDED.type -> handleChatEnded(it)
                                contentType == ContentType.META_DATA.type -> handleMetadata(it)
                            }
                        }
                    }
                }
            }
        }
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
    }

    // Message Handling Logic
    private fun handleMessage(innerJson: JSONObject) {
        val participantRole = innerJson.getString("ParticipantRole")
        val messageId = innerJson.getString("Id")
        var messageText = innerJson.getString("Content")
        val messageType = if (participantRole.equals(chatConfiguration.customerName, ignoreCase = true)) MessageType.SENDER else MessageType.RECEIVER
        val time = CommonUtils.formatTime(innerJson.getString("AbsoluteTime"))
        val message = Message(
            participant = participantRole,
            text = messageText,
            contentType = innerJson.getString("ContentType"),
            messageType = messageType,
            timeStamp = time,
            messageID = messageId
        )
        this.messageCallBack(message)
    }

    private fun handleParticipantJoined(innerJson: JSONObject) {
        val participantRole = innerJson.getString("ParticipantRole")
        val messageText = "$participantRole has joined"
        val message = Message(
            participant = participantRole,
            text = messageText,
            contentType = innerJson.getString("ContentType"),
            messageType = MessageType.COMMON
        )
        this.messageCallBack(message)
    }

    private fun handleParticipantLeft(innerJson: JSONObject) {
        val participantRole = innerJson.getString("ParticipantRole")
        val messageText = "$participantRole has left"
        val message = Message(
            participant = participantRole,
            text = messageText,
            contentType = innerJson.getString("ContentType"),
            messageType = MessageType.COMMON
        )
        this.messageCallBack(message)
    }

    private fun handleTyping(innerJson: JSONObject) {
        val participantRole = innerJson.getString("ParticipantRole")
        val time = CommonUtils.formatTime(innerJson.getString("AbsoluteTime"))
        val messageType = if (participantRole.equals(chatConfiguration.customerName, ignoreCase = true)) MessageType.SENDER else MessageType.RECEIVER
        val message = Message(
            participant = participantRole,
            text = "...",
            contentType = innerJson.getString("ContentType"),
            messageType = messageType,
            timeStamp = time
        )
        this.messageCallBack(message)    }

    private fun handleChatEnded(innerJson: JSONObject) {
        closeWebSocket();
        isChatActive = false;
        val message = Message(
            participant = "System Message",
            text = "The chat has ended.",
            contentType = innerJson.getString("ContentType"),
            messageType = MessageType.COMMON
        )
        this.messageCallBack(message)
    }

    private fun handleMetadata(innerJson: JSONObject) {
        val messageMetadata = innerJson.getJSONObject("MessageMetadata")
        val messageId = messageMetadata.getString("MessageId")
        val receipts = messageMetadata.optJSONArray("Receipts")
        var status = "Delivered"
        val time = CommonUtils.formatTime(innerJson.getString("AbsoluteTime"))
        receipts?.let {
            for (i in 0 until it.length()) {
                val receipt = it.getJSONObject(i)
                if (receipt.optString("ReadTimestamp").isNotEmpty()) {
                    status = "Read"
                }
            }
        }
        val message = Message(
            participant = "",
            text = "",
            contentType = innerJson.getString("ContentType"),
            messageType = MessageType.SENDER,
            timeStamp = time,
            messageID = messageId,
            status = status
        )
        this.messageCallBack(message)
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
        transcriptItems.forEach { item ->
            val participantRole = item.participantRole

            // Create the message content in JSON format
            val messageContentJson = JSONObject().apply {
                put("Id", item.id ?: "")
                put("ParticipantRole", participantRole)
                put("AbsoluteTime", item.absoluteTime ?: "")
                put("ContentType", item.contentType ?: "")
                put("Content", item.content ?: "")
                put("Type", item.type)
                put("DisplayName", item.displayName ?: "")
            }

            // Convert JSON object to String format
            val messageContentString = messageContentJson.toString()

            // Prepare the message in the format expected by WebSocket
            val wrappedMessageString = "{\"content\":\"${messageContentString.replace("\"", "\\\"")}\"}"

            // Send the formatted message string via WebSocket
            websocketDidReceiveMessage(wrappedMessageString)
        }
    }

}
