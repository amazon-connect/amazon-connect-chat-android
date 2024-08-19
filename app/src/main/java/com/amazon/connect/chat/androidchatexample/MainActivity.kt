package com.amazon.connect.chat.androidchatexample

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.keyboardAsState
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.androidchatexample.viewmodel.ChatViewModel
import com.amazon.connect.chat.androidchatexample.views.ChatMessageView
import com.amazon.connect.chat.androidchatexample.ui.theme.androidconnectchatandroidTheme
import com.amazon.connect.chat.sdk.model.TranscriptItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            androidconnectchatandroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
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
                    viewModel.clearContactId() // Clear contactId
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

        ContactIdAndTokenSection(viewModel)

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
                    ChatView(viewModel = viewModel) // Your chat view composable
                }
            }
        }
    }
}

@Composable
fun ChatView(viewModel: ChatViewModel) {
    val messages by viewModel.messages.observeAsState(listOf())
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isKeyboardVisible = keyboardAsState().value
    var isChatEnded by remember { mutableStateOf(false) }

    LaunchedEffect(messages, isKeyboardVisible) {
        if (messages.isNotEmpty() ) {
            listState.animateScrollToItem(messages.lastIndex)
        }

        if (isKeyboardVisible){
            viewModel.sendEvent(contentType = ContentType.TYPING)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Display the chat messages
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(messages) { index, message ->
                ChatMessage(message)
                LaunchedEffect(key1 = message, key2 = index) {
                    if (message.contentType == ContentType.ENDED.type) {
                        isChatEnded = true
                        viewModel.clearParticipantToken()
                    }else{
                        isChatEnded = false
                    }
                    // Logic to determine if the message is visible.
                    // For simplicity, let's say it's visible if it's one of the last three messages.
                    // TODO: Update here to send read receipts from SDK
//                    if (index >= messages.size - 3 && message.messageDirection == MessageDirection.INCOMING) {
//                        viewModel.sendReadEventOnAppear(message)
//                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                enabled = !isChatEnded
            )
            IconButton(onClick = {
                viewModel.sendMessage(textInput)
                textInput = ""
            },
                enabled = !isChatEnded,
                modifier = if (isChatEnded) Modifier.blur(2.dp) else Modifier
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatMessage(transcriptItem: TranscriptItem) {
    // Customize this composable to display each message
    ChatMessageView(transcriptItem = transcriptItem)
}

@Composable
fun ContactIdAndTokenSection(viewModel: ChatViewModel) {
    val contactId by viewModel.liveContactId.observeAsState()
    val participantToken by viewModel.liveParticipantToken.observeAsState()

    Column {
        Text(text = "Contact ID: ${if (contactId != null) "Available" else "Not available"}", color = if (contactId != null) Color.Blue else Color.Red)
        Button(onClick = viewModel::clearContactId) {
            Text("Clear Contact ID")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Participant Token: ${if (participantToken != null) "Available" else "Not available"}", color = if (participantToken != null) Color.Blue else Color.Red)
        Button(onClick = viewModel::clearParticipantToken) {
            Text("Clear Participant Token")
        }
        Button(onClick = viewModel::endChat) {
            Text(text = "Disconnect")
        }
    }
}


// Temporary composable for preview purposes
@Composable
fun ChatViewPreview(messages: List<Message>) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                ChatMessage(message)
            }
        }
        // Rest of the ChatView layout
        // ...
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
//    ChatViewPreview(messages = sampleMessages)
}
