package com.amazon.connect.chat.androidchatexample.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.ListPickerContent
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.PlainTextContent
import com.amazon.connect.chat.sdk.model.QuickReplyContent
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazon.connect.chat.sdk.utils.CommonUtils.Companion.MarkdownText

@Composable
fun ChatMessageView(transcriptItem: TranscriptItem) {
    when (transcriptItem) {
        is Message -> {
            when (transcriptItem.messageDirection) {
                MessageDirection.OUTGOING -> SenderChatBubble(transcriptItem)
                MessageDirection.INCOMING -> {
                    if (transcriptItem.text == "...") {
                        TypingIndicator()
                    } else {
                        when (val content = transcriptItem.content) {
                            is PlainTextContent -> ReceiverChatBubble(transcriptItem)
                            is QuickReplyContent -> QuickReplyContentView(transcriptItem, content)
                            is ListPickerContent -> ListPickerContentView(transcriptItem, content)
                            else -> Text(text = "Unsupported message type")
                        }
                    }
                }
                MessageDirection.COMMON -> CommonChatBubble(transcriptItem)
                null -> CommonChatBubble(transcriptItem)
            }
        }
        // Add handling for other TranscriptItem subclasses if necessary
        is Event -> {
            EventView(transcriptItem)
        }
        else -> Text(text = "Unsupported transcript item type")
    }
}


@Composable
fun SenderChatBubble(message: Message) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        message.participant?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Surface(
            color = Color(0xFF4D74DA),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                MarkdownText(
                    text = message.text,
                    color = Color.White
                )
                message.timeStamp?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        // TODO : update receipts
//        message.status?.let {
//            Text(
//                text = it,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color.Gray
//            )
//        }
    }
}

@Composable
fun ReceiverChatBubble(message: Message) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        message.participant?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(bottom = 4.dp)
            )
        }
        Surface(
            color = Color(0xFF8BC34A),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                MarkdownText(
                    text = message.text,
                    color = Color.White
                )
                message.timeStamp?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.End).alpha(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CommonChatBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.text,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        )
    }
}

@Composable
fun EventView(event: Event) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        event.text?.let {
            Text(
                text = it,
                color = Color.Blue,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
            )
        }
    }
}

