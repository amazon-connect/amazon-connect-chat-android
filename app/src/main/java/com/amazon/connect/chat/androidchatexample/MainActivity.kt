package com.amazon.connect.chat.androidchatexample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.keyboardAsState
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.androidchatexample.viewmodel.ChatViewModel
import com.amazon.connect.chat.androidchatexample.views.ChatMessageView
import com.amazon.connect.chat.androidchatexample.ui.theme.androidconnectchatandroidTheme
import com.amazon.connect.chat.androidchatexample.utils.FileUtils.getOriginalFileName
import com.amazon.connect.chat.androidchatexample.utils.FileUtils.previewFileFromCacheOrDownload
import com.amazon.connect.chat.androidchatexample.views.AttachmentTextView
import com.amazon.connect.chat.androidchatexample.views.ConfigPicker
import com.amazon.connect.chat.sdk.model.TranscriptItem
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        setContent {
            androidconnectchatandroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(this)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { fileUri ->
                chatViewModel.selectedFileUri.value = fileUri
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(activity: Activity, viewModel: ChatViewModel = hiltViewModel()) {
    var showCustomSheet by remember { mutableStateOf(false) }
    val isLoading = viewModel.isLoading.observeAsState(initial = false)
    val isChatActive = viewModel.isChatActive.observeAsState(initial = false)
    var showDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    val participantToken = viewModel.liveParticipantToken.observeAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorMessage.observeAsState()

    LaunchedEffect(errorMessage) {
        showErrorDialog = errorMessage != null
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearErrorMessage()
            },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "An unknown error occurred") },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    viewModel.clearErrorMessage()
                }) { Text("OK") }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Chat") },
            text = { Text("Do you want to restore the previous chat session?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.clearParticipantToken()
                        viewModel.initiateChat() // Restore the chat directly
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    viewModel.clearParticipantToken()
                    viewModel.initiateChat() // Start new chat
                }) { Text("Start new") }
            }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("End Chat") },
            text = { Text("Are you sure you want to end the chat?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.endChat()
                        showCustomSheet = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (!showCustomSheet) {
                ExtendedFloatingActionButton(
                    text = {
                        if (isChatActive.value == false) {
                            Text("Start Chat")
                        } else {
                            Text("Resume Chat")
                        }
                    },
                    icon = {
                        if (isLoading.value) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        if (isChatActive.value == false) {
                            viewModel.initiateChat()
                        } else {
                            showCustomSheet = true
                        }
                    },

                )
            }
        }
    ) {
        LaunchedEffect(isChatActive.value) {
            if (!isLoading.value && isChatActive.value) {
                showCustomSheet = true
            }
        }


        Column {
            ConfigPicker(viewModel) // Include the configuration picker here
//            ParticipantTokenSection(activity, viewModel)
        }
//        ParticipantTokenSection(activity, viewModel)

        AnimatedVisibility(
            visible = showCustomSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(Color.White, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                "Chat", modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showCustomSheet = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                showDialog = true
                            }) {
                                Text("End Chat")
                            }
                        }
                    )
                    ChatView(activity = activity, viewModel = viewModel) // Your chat view composable
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(viewModel: ChatViewModel, activity: Activity) {
    val messages by viewModel.messages.observeAsState(listOf())
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isKeyboardVisible = keyboardAsState().value
    var isChatEnded by remember { mutableStateOf(false) }
    // Track if the typing event has been sent
    var hasSentTypingEvent by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val onPreviewAttachment: (URL, String) -> Unit = { uri, fileName->
        previewFileFromCacheOrDownload(activity, uri, fileName)
    }

    val selectedFileName by viewModel.selectedFileUri.observeAsState()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.fetchTranscript { success ->
            isRefreshing = false
            if (success) {
                Log.d("ChatView", "Transcript fetched successfully")
            } else {
                Log.e("ChatView", "Failed to fetch transcript")
            }
        }
    }

    LaunchedEffect(messages, isKeyboardVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }

        // Send typing event only once when the keyboard is visible and there's input
        if (isKeyboardVisible && !hasSentTypingEvent) {
            Log.d("ChatView", "Sending typing event")
            viewModel.sendEvent(contentType = ContentType.TYPING)
            hasSentTypingEvent = true
        }

        // Reset the flag when the keyboard is hidden
        if (!isKeyboardVisible) {
            hasSentTypingEvent = false
        }
    }

    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Display the chat messages
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(messages, key = { index, message -> message.id }) { index, message ->
                    ChatMessage(
                        transcriptItem = message,
                        viewModel = viewModel,
                        onPreviewAttachment = onPreviewAttachment,
                        recentOutgoingMessageID = messages.getOrNull(index)?.id
                    )
                    LaunchedEffect(key1 = message, key2 = index) {
                        if (message.contentType == ContentType.ENDED.type) {
                            isChatEnded = true
                            viewModel.clearParticipantToken()
                        } else {
                            isChatEnded = false
                        }
                        // Logic to determine if the message is visible.
                        // For simplicity, let's say it's visible if it's one of the last three messages.
                        if (index == messages.size - 1 && message is Message) {
                            viewModel.sendReadEventOnAppear(message)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                AttachmentTextView(
                    text = textInput,
                    selectedFileUri = selectedFileName?.getOriginalFileName(activity),
                    onTextChange = { text ->
                        textInput = text
                    },
                    onRemoveAttachment = {
                        viewModel.selectedFileUri.value = null
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if(!selectedFileName?.lastPathSegment.isNullOrEmpty()) {
                            selectedFileName?.let { viewModel.uploadAttachment(it) }
                        }
                        if (textInput.isNotEmpty()) {
                            viewModel.sendMessage(textInput)
                        }
                        textInput = ""
                        viewModel.selectedFileUri.value = null
                    },
                    enabled = !isChatEnded,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }

                IconButton(
                    onClick = {
                        viewModel.openFile(activity = activity )
                    },
                    enabled = !isChatEnded,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                }
            }

        }
    }
}

@Composable
fun ChatMessage(
    transcriptItem: TranscriptItem,
    viewModel: ChatViewModel,
    recentOutgoingMessageID: String?,
    onPreviewAttachment: (URL, String) -> Unit
) {
    ChatMessageView(transcriptItem = transcriptItem, viewModel = viewModel, onPreviewAttachment = onPreviewAttachment, recentOutgoingMessageID = recentOutgoingMessageID)
}

@Composable
fun ParticipantTokenSection(activity: Activity, viewModel: ChatViewModel) {
    val participantToken by viewModel.liveParticipantToken.observeAsState()

    Column {
        Text(
            text = "Participant Token: ${if (participantToken != null) "Available" else "Not available"}",
            color = if (participantToken != null) Color.Blue else Color.Red
        )
        Button(onClick = viewModel::clearParticipantToken) {
            Text("Clear Participant Token")
        }
        Button(onClick = { viewModel.openFile(activity) }) {
            Text(text = "Upload Attachment")
        }
    }
}

// Preview annotation
@Preview(showBackground = true)
@Composable
fun ChatViewPreview() {
//    val sampleMessages = listOf(
//        Message(
//            participant = "CUSTOMER",
//            text = "Hello asdfioahsdfoas idfuoasdfihjasdlfihjsoadfjopasoaisdfhjoasidjf ",
//            contentType = "text/plain",
//            messageDirection = MessageDirection.OUTGOING,
//            timeStamp = "06=51",
//            status = "Delivered"
//        ),
//        Message(
//            participant = "SYSTEM",
//            text = "{\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Which department do you want to select?\",\"subtitle\":\"Tap to select option\",\"imageType\":\"URL\",\"imageData\":\"https://amazon-connect-interactive-message-blog-assets.s3-us-west-2.amazonaws.com/interactive-images/company.jpg\",\"elements\":[{\"title\":\"Billing\",\"subtitle\":\"Request billing information\",\"imageType\":\"URL\",\"imageData\":\"https://amazon-connect-interactive-message-blog-assets.s3-us-west-2.amazonaws.com/interactive-images/billing.jpg\"},{\"title\":\"New Service\",\"subtitle\":\"Set up a new service\",\"imageType\":\"URL\",\"imageData\":\"https://amazon-connect-interactive-message-blog-assets.s3-us-west-2.amazonaws.com/interactive-images/new_service.jpg\"},{\"title\":\"Cancellation\",\"subtitle\":\"Request a cancellation\",\"imageType\":\"URL\",\"imageData\":\"https://amazon-connect-interactive-message-blog-assets.s3-us-west-2.amazonaws.com/interactive-images/cancel.jpg\"}]}}}",
//            contentType = "application/vnd.amazonaws.connect.message.interactive",
//            messageDirection = MessageDirection.INCOMING,
//            timeStamp = "14:18",
//            messageID = "f905d16e-12a0-4854-9079-d5b34476c3ba",
//            status = null,
//            isRead = false
//        ),
//        Message(
//            participant = "AGENT",
//            text = "...",
//            contentType = "text/plain",
//            messageDirection = MessageDirection.INCOMING,
//            timeStamp = "06:51",
//            isRead = true
//        ),
//        Message(
//            participant = "AGENT",
//            text = "Hello, **this** is a agent \n\n speaking.Hello, this is a agent speaking.",
//            contentType = "text/plain",
//            messageDirection = MessageDirection.INCOMING,
//            timeStamp = "06:51",
//            isRead = true
//        ),
//
//        Message(
//            participant = "SYSTEM",
//            text = "{\"templateType\":\"QuickReply\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"How was your experience?\",\"elements\":[{\"title\":\"Very unsatisfied\"},{\"title\":\"Unsatisfied\"},{\"title\":\"Neutral\"},{\"title\":\"Satisfied\"},{\"title\":\"Very Satisfied\"}]}}}",
//            contentType = "application/vnd.amazonaws.connect.message.interactive",
//            messageDirection = MessageDirection.INCOMING,
//            timeStamp = "06:20",
//            messageID = "8f76a266-6654-434f-94ea-87ec111ee341",
//            status = null,
//            isRead = false
//        ),
//
//        Message(
//            participant = "SYSTEM",
//            text = "{\"templateType\":\"ListPicker\",\"version\":\"1.0\",\"data\":{\"content\":{\"title\":\"Which department would you like?\",\"subtitle\":\"Tap to select option\",\"elements\":[{\"title\":\"Billing\",\"subtitle\":\"For billing issues\"},{\"title\":\"New Service\",\"subtitle\":\"For new service\"},{\"title\":\"Cancellation\",\"subtitle\":\"For new service requests\"}]}}}",
//            contentType = "application/vnd.amazonaws.connect.message.interactive",
//            messageDirection = MessageDirection.INCOMING,
//            timeStamp = "14:18",
//            messageID = "f905d16e-12a0-4854-9079-d5b34476c3ba",
//            status = null,
//            isRead = false
//        ),
//
//        Message(
//            participant = "SYSTEM",
//            text = "Someone joined the chat.Someone joined the chat.Someone joined the chat.",
//            contentType = "text/plain",
//            messageDirection = MessageDirection.COMMON,
//            timeStamp = "06:51",
//            isRead = true
//        )
//    )
//
//    ChatView()
}
