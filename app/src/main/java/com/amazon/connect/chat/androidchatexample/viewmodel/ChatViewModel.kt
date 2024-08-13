package com.amazon.connect.chat.androidchatexample.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amazon.connect.chat.androidchatexample.models.StartChatResponse
import com.amazon.connect.chat.androidchatexample.network.Resource
import com.amazon.connect.chat.androidchatexample.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionRequest
import com.amazonaws.services.connectparticipant.model.CreateParticipantConnectionResult
import com.amazon.connect.chat.androidchatexample.Config
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.androidchatexample.models.ParticipantDetails
import com.amazon.connect.chat.androidchatexample.models.PersistentChat
import com.amazon.connect.chat.androidchatexample.models.StartChatRequest
import com.amazon.connect.chat.androidchatexample.utils.CommonUtils
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.parseErrorMessage
import com.amazon.connect.chat.sdk.utils.ContentType
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.TranscriptItem
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatSession: ChatSession, // Injected ChatSession
    private val chatRepository: ChatRepository,
    private val sharedPreferences: SharedPreferences,
    private val webSocketManager: WebSocketManager,
) : ViewModel() {
    private val chatConfiguration = Config
    private val _isLoading = MutableLiveData(false)
    val isLoading: MutableLiveData<Boolean> = _isLoading
    private val _startChatResponse = MutableLiveData<Resource<StartChatResponse>>()
    private val startChatResponse: LiveData<Resource<StartChatResponse>> = _startChatResponse
    private val _createParticipantConnectionResult = MutableLiveData<CreateParticipantConnectionResult?>()
    val createParticipantConnectionResult: MutableLiveData<CreateParticipantConnectionResult?> = _createParticipantConnectionResult
    private val _messages = MutableLiveData<List<TranscriptItem>>()
    val messages: LiveData<List<TranscriptItem>> = _messages
    private val _webSocketUrl = MutableLiveData<String?>()
    val webSocketUrl: MutableLiveData<String?> = _webSocketUrl
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for actual string values, updates will reflect in the UI
    private val _liveContactId = MutableLiveData<String?>(sharedPreferences.getString("contactID", null))
    val liveContactId: LiveData<String?> = _liveContactId

    private val _liveParticipantToken = MutableLiveData<String?>(sharedPreferences.getString("participantToken", null))
    val liveParticipantToken: LiveData<String?> = _liveParticipantToken

    init {
        webSocketManager.requestNewWsUrl = { createParticipantConnection(null) }
    }

    // Setters that update LiveData, which in turn update the UI
    private var contactId: String?
        get() = liveContactId.value
        set(value) {
//            sharedPreferences.edit().putString("contactID", value).apply()
            _liveContactId.value = value
        }

    private var participantToken: String?
        get() = liveParticipantToken.value
        set(value) {
//            sharedPreferences.edit().putString("participantToken", value).apply()
            _liveParticipantToken.value = value  // Reflect the new value in LiveData
        }

    fun clearContactId() {
        sharedPreferences.edit().remove("contactID").apply()
        _liveContactId.value = null
    }

    fun clearParticipantToken() {
        sharedPreferences.edit().remove("participantToken").apply()
        _liveParticipantToken.value = null
    }

    init {
        configureChatSession()
    }

    private fun configureChatSession() {
        val globalConfig = GlobalConfig(region = chatConfiguration.region)
        chatSession.configure(globalConfig)
    }

    fun initiateChat() {
        viewModelScope.launch {
            _isLoading.value = true
            _messages.postValue(emptyList()) // Clear existing messages
            if (participantToken != null) {
                participantToken?.let {
                    val chatDetails = ChatDetails(participantToken = it)
                    createParticipantConnection(chatDetails)
                }
            } else if (contactId != null) {
                startChat(contactId)
            } else {
                startChat(null) // Start a fresh chat if no tokens are present
            }
        }
    }

    private fun startChat(sourceContactId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val participantDetails = ParticipantDetails(displayName = chatConfiguration.customerName)
            val persistentChat: PersistentChat? = sourceContactId?.let { PersistentChat(it, "ENTIRE_PAST_SESSION") }
            val request = StartChatRequest(
                connectInstanceId = chatConfiguration.connectInstanceId,
                contactFlowId = chatConfiguration.contactFlowId,
                participantDetails = participantDetails,
                persistentChat = persistentChat
            )
            when (val response = chatRepository.startChat(startChatRequest = request)) {
                is Resource.Success -> {
                    response.data?.data?.startChatResult?.let { result ->
                        this@ChatViewModel.contactId = result.contactId
                        this@ChatViewModel.participantToken = result.participantToken
                        handleStartChatResponse(result)
                    } ?: run {
                        _isLoading.value = false
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = response.message
                    _isLoading.value = false
                    clearContactId()
                }

                is Resource.Loading -> _isLoading.value = true
            }
        }
    }


    private fun handleStartChatResponse(result: StartChatResponse.Data.StartChatResult) {
        viewModelScope.launch {
            val chatDetails = ChatDetails(
                contactId = result.contactId,
                participantId = result.participantId,
                participantToken = result.participantToken
            )
            createParticipantConnection(chatDetails)
        }
    }

    private fun createParticipantConnection1(chatDetails: ChatDetails) {
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            val result = chatSession.connect(chatDetails)
            _isLoading.value = false // Stop loading
            if (result.isSuccess) {
                // Handle successful connection
                Log.d("ChatViewModel", "Connection successful $result")
            } else if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
            }
        }
    }


    private fun createParticipantConnection(chatDetails: ChatDetails?) {
        val pToken: String = if (chatDetails?.contactId == null) participantToken.toString() else chatDetails?.participantToken.toString()
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            chatRepository.createParticipantConnection(
                pToken,
                object : AsyncHandler<CreateParticipantConnectionRequest, CreateParticipantConnectionResult> {
                    override fun onError(exception: Exception?) {
                        Log.e("ChatViewModel", "CreateParticipantConnection failed: ${exception?.localizedMessage}")
                        clearParticipantToken()
                        _errorMessage.value = parseErrorMessage(exception?.localizedMessage)
                        _isLoading.postValue(false)
                    }
                    override fun onSuccess(request: CreateParticipantConnectionRequest?, result: CreateParticipantConnectionResult?) {
                        viewModelScope.launch {
                            result?.let { connectionResult ->
                                _createParticipantConnectionResult.value = connectionResult
                                val websocketUrl = connectionResult.websocket?.url
                                _webSocketUrl.value = websocketUrl
                                if (chatDetails !== null) {
                                    participantToken = chatDetails?.participantToken;
                                }

                                websocketUrl?.let { wsUrl ->
                                    webSocketManager.createWebSocket(
                                        wsUrl,
                                        this@ChatViewModel::onMessageReceived,
                                        this@ChatViewModel::onWebSocketError
                                    )
                                }
                                connectionResult.connectionCredentials?.connectionToken?.let { cToken ->
                                    val transcriptsResource =
                                        chatRepository.getAllTranscripts(cToken)
                                    if (transcriptsResource is Resource.Success) {
                                        transcriptsResource.data?.transcript?.let { transcriptItems ->
                                            Log.d("ChatViewModel:GetTranscript",
                                                transcriptItems.toString()
                                            )
                                            webSocketManager.formatAndProcessTranscriptItems(
                                                transcriptItems.reversed()
                                            )
                                        }
                                    } else {
                                        Log.e(
                                            "ChatViewModel",
                                            "Error fetching transcripts: ${transcriptsResource.message}"
                                        )
                                        _errorMessage.value = parseErrorMessage("Error fetching transcripts: ${transcriptsResource.message}")
                                    }
                                }
                                _isLoading.postValue(false) // End loading
                            } ?: run {
                                Log.e(
                                    "ChatViewModel",
                                    "CreateParticipantConnection returned null result"
                                )
                                _errorMessage.value = parseErrorMessage("CreateParticipantConnection returned null result")
                                _isLoading.postValue(false) // End loading
                            }
                        }
                    }
                }
            )
        }
    }

    private fun onMessageReceived(transcriptItem: TranscriptItem) {
        viewModelScope.launch {
            // Log the current state before the update
            Log.i("ChatViewModel", "Received transcript item: $transcriptItem")

            // Construct the new list with modifications based on the received transcript item
            val updatedMessages = _messages.value.orEmpty().toMutableList().apply {
                // Remove any typing indicators
                removeIf { it is Message && it.text == "..." }

                when (transcriptItem) {
                    is Message  -> {

                        // Get message direction here
                        CommonUtils.getMessageDirection(transcriptItem)

                        if (!(transcriptItem.text == "..." && transcriptItem.participant == chatConfiguration.customerName)) {
                            add(transcriptItem)
                        }

                        // Additional logic like sending 'Delivered' events
                        // TODO : Update here to send read receipts from SDK
                        if (transcriptItem.participant == chatConfiguration.agentName && transcriptItem.contentType.contains("text")) {
                            val content = "{\"messageId\":\"${transcriptItem.id}\"}"
                            sendEvent(content, ContentType.MESSAGE_DELIVERED)
                        }
                    }
                    is Event -> {
                        CommonUtils.customizeEvent(transcriptItem)
                        add(transcriptItem)
                    }
                    else -> {
                        Log.i("ChatViewModel", "Unhandled transcript item type: ${transcriptItem::class.simpleName}")
                    }
                }
            }

            // Update messages LiveData in a thread-safe manner
            _messages.value = updatedMessages
        }
    }



    private fun onWebSocketError(errorMessage: String) {
        // Handle WebSocket errors
        _isLoading.postValue(false)
    }

    fun sendMessage(text: String) {
        if (text.isNotEmpty()) {
            createParticipantConnectionResult.value?.connectionCredentials?.let { credentials ->
                viewModelScope.launch {
                    val result = chatRepository.sendMessage(credentials.connectionToken, text)
                    result.onSuccess {
                        // Handle success - update UI or state as needed
                    }.onFailure { exception ->
                        // Handle failure - update UI or state, log error, etc.
                        Log.e("ChatViewModel", "Error sending message: ${exception.message}")
                    }
                }
            }
        }
    }

    fun sendEvent(content: String = "", contentType: ContentType) {
        createParticipantConnectionResult.value?.connectionCredentials?.let { credentials ->
            viewModelScope.launch {
                val result = chatRepository.sendEvent(credentials.connectionToken, contentType,content)
                result.onSuccess {
                    // Handle success - update UI or state as needed
                }.onFailure { exception ->
                    // Handle failure - update UI or state, log error, etc.
                    Log.e("ChatViewModel", "Error sending Event: ${exception.message}")
                }
            }
        }
    }

    fun sendReadEventOnAppear(message: Message) {
        // TODO : Update here to send read receipts from SDK
//        val messagesList = (_messages.value ?: return).toMutableList()
//        val index = messagesList.indexOfFirst {
//            it.text == message.text && it.text.isNotEmpty() && it.contentType.contains("text")
//                    && it.participant != chatConfiguration.customerName && !it.isRead
//        }
//        if (index != -1) {
//            val messageId = messagesList[index].messageID ?: return
//            val content = "{\"messageId\":\"$messageId\"}"
//            sendEvent(content, ContentType.MESSAGE_READ)
//            messagesList[index] = messagesList[index].copy(isRead = true)
//            _messages.postValue(messagesList) // Safely post the updated list to the LiveData
//        }
    }

    fun endChat(){
        viewModelScope.launch {
            chatSession.disconnect()
        }
        clearParticipantToken()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}