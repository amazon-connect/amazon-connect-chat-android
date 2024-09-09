package com.amazon.connect.chat.androidchatexample.viewmodel

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.connect.chat.androidchatexample.Config
import com.amazon.connect.chat.androidchatexample.models.ParticipantDetails
import com.amazon.connect.chat.androidchatexample.models.StartChatRequest
import com.amazon.connect.chat.androidchatexample.models.StartChatResponse
import com.amazon.connect.chat.androidchatexample.network.Resource
import com.amazon.connect.chat.androidchatexample.repository.ChatRepository
import com.amazon.connect.chat.androidchatexample.utils.CommonUtils
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageReceiptType
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazonaws.services.connectparticipant.model.ScanDirection
import com.amazonaws.services.connectparticipant.model.SortKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatSession: ChatSession, // Injected ChatSession
    private val chatRepository: ChatRepository,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {
    private val chatConfiguration = Config
    private val _isLoading = MutableLiveData(false)
    val isLoading: MutableLiveData<Boolean> = _isLoading

    private val _isChatActive = MutableLiveData(false)
    val isChatActive: MutableLiveData<Boolean> = _isChatActive

    private val _messages = MutableLiveData<List<TranscriptItem>>()
    val messages: LiveData<List<TranscriptItem>> = _messages

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _liveParticipantToken = MutableLiveData<String?>(sharedPreferences.getString("participantToken", null))
    val liveParticipantToken: LiveData<String?> = _liveParticipantToken

    private var participantToken: String?
        get() = liveParticipantToken.value
        set(value) {
            sharedPreferences.edit().putString("participantToken", value).apply()
            _liveParticipantToken.value = value  // Reflect the new value in LiveData
        }

    fun clearParticipantToken() {
        sharedPreferences.edit().remove("participantToken").apply()
        _liveParticipantToken.value = null
    }

    init {
        viewModelScope.launch {
            configureChatSession()
        }
    }

    private suspend fun configureChatSession() {
        val globalConfig = GlobalConfig(region = chatConfiguration.region)
        chatSession.configure(globalConfig)
        setupChatHandlers(chatSession)
    }

    private suspend fun setupChatHandlers(chatSession: ChatSession) {
        chatSession.onConnectionEstablished = {
            Log.d("ChatViewModel", "Connection established.")
            _isChatActive.value = true
        }

        chatSession.onMessageReceived = { transcriptItem ->
            // Handle received message
            if (transcriptItem is Message) {
                Log.d("ChatViewModel", "Message received: $transcriptItem")
                // Send delivered receipt
//                    chatSession.sendMessageReceipt(
//                        transcriptItem,
//                        MessageReceiptType.MESSAGE_DELIVERED
//                    )
            }
        }

        chatSession.onTranscriptUpdated = { transcriptList ->
            Log.d("ChatViewModel", "Transcript onTranscriptUpdated: $transcriptList")
            viewModelScope.launch {
                onUpdateTranscript(transcriptList)
            }
        }

        chatSession.onChatEnded = {
           Log.d("ChatViewModel", "Chat ended.")
            _isChatActive.value = false
        }

        chatSession.onConnectionBroken = {
            Log.d("ChatViewModel", "Connection broken.")
        }

        chatSession.onConnectionReEstablished = {
            Log.d("ChatViewModel", "Connection re-established.")
            _isChatActive.value = true
        }

        chatSession.onChatSessionStateChanged = {
            Log.d("ChatViewModel", "Chat session state changed: $it")
            _isChatActive.value = it
        }
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
            } else {
                startChat() // Start a fresh chat if no tokens are present
            }
        }
    }

    private fun startChat() {
        viewModelScope.launch {
            _isLoading.value = true
            val participantDetails = ParticipantDetails(displayName = chatConfiguration.customerName)
            val request = StartChatRequest(
                connectInstanceId = chatConfiguration.connectInstanceId,
                contactFlowId = chatConfiguration.contactFlowId,
                participantDetails = participantDetails
            )
            when (val response = chatRepository.startChat(startChatRequest = request)) {
                is Resource.Success -> {
                    response.data?.data?.startChatResult?.let { result ->
                        this@ChatViewModel.participantToken = result.participantToken
                        handleStartChatResponse(result)
                    } ?: run {
                        _isLoading.value = false
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = response.message
                    _isLoading.value = false
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

    private fun createParticipantConnection(chatDetails: ChatDetails) {
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

    private fun onUpdateTranscript(transcriptList: List<TranscriptItem>) {
        viewModelScope.launch {
            val tempTranscriptList = transcriptList.toList()
            val updatedMessages = tempTranscriptList.map { transcriptItem ->
                if (transcriptItem is Event) {
                    CommonUtils.customizeEvent(transcriptItem)
                }
                CommonUtils.getMessageDirection(transcriptItem)
                transcriptItem
            }
            _messages.value = updatedMessages
            Log.d("ChatViewModel", "Transcript updated: ${_messages.value}")
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            if (text.isNotEmpty()) {
                val result = chatSession.sendMessage(ContentType.RICH_TEXT, text)
                result.onSuccess {
                    // Handle success - update UI or state as needed
                }.onFailure { exception ->
                    // Handle failure - update UI or state, log error, etc.
                    Log.e("ChatViewModel", "Error sending message: ${exception.message}")
                }
            }
        }
    }

    fun sendEvent(content: String = "", contentType: ContentType) {
        viewModelScope.launch {
            val result = chatSession.sendEvent(contentType, content)
            result.onSuccess {
                // Handle success - update UI or state as needed
            }.onFailure { exception ->
                // Handle failure - update UI or state, log error, etc.
                Log.e("ChatViewModel", "Error sending event: ${exception.message}")
            }
        }
    }

    suspend fun sendReadEventOnAppear(message: Message) {
        chatSession.sendMessageReceipt(message, MessageReceiptType.MESSAGE_READ)
    }


    // Fetches transcripts
    fun fetchTranscript(onCompletion: (Boolean) -> Unit) {
        viewModelScope.launch {
            chatSession.getTranscript(ScanDirection.BACKWARD, SortKey.DESCENDING, 30, null, _messages.value?.get(0)?.id).onSuccess {
                Log.d("ChatViewModel", "Transcript fetched successfully")
                onCompletion(true)
            }.onFailure {
                Log.e("ChatViewModel", "Error fetching transcript: ${it.message}")
                onCompletion(false)
            }
        }
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

    // Request code for selecting a PDF document.
    val PICK_PDF_FILE = 2

    fun openFile(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        activity.startActivityForResult(intent, PICK_PDF_FILE)
    }


    fun uploadAttachment(fileUri: Uri) {
        viewModelScope.launch {
            chatSession.sendAttachment(fileUri)
        }
    }
}